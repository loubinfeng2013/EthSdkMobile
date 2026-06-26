# CHANGELOG

## v2.0.0 (2026-06-26)

### 重大变更（Breaking Changes）

#### `InitWithUrl` 签名变更

v1 只接受一个 URL 参数，v2 严格区分 HTTPS 和 WSS 节点，需要同时传入两个 URL：

```go
// v1
InitWithUrl(url string) bool

// v2
InitWithUrl(httpsUrl string, wssUrl string) bool
```

- `httpsUrl` 必须以 `https://` 开头，否则直接返回 `false`
- `wssUrl` 必须以 `wss://` 开头，否则直接返回 `false`
- 两个参数均不能为空

#### 内部连接由单 Client 拆分为双 Client

| | v1 | v2 |
|---|---|---|
| 连接变量 | `client *ethclient.Client`（单一连接） | `httpsClient` + `wssClient`（双连接） |
| 查询/交易 | 统一走同一个连接 | 走 `httpsClient`（HTTP 长连接，稳定） |
| 事件订阅 | 统一走同一个连接 | 走 `wssClient`（WebSocket，支持 Push） |

> **背景**：HTTP 和 WebSocket 在以太坊节点上职责不同。HTTP 适合请求-响应式的查询和广播，WSS 适合 `eth_subscribe` 事件推送。混用同一连接在部分节点上会导致订阅不稳定。

---

### 新增常量

v1 只有 `MainNetWork` / `SepoliaNetWork` 两个 WSS 常量，v2 新增对应的 HTTPS 常量：

```go
// v2 新增
const (
    MainWss      = "wss://ethereum.publicnode.com"
    MainHttps    = "https://ethereum.publicnode.com"
    SepoliaWss   = "wss://sepolia.infura.io/ws/v3/..."
    SepoliaHttps = "https://sepolia.infura.io/v3/..."
)
```

---

### 优化项

#### 1. 初始化连通性校验

v2 在 `InitWithUrl` 建立连接后，会立即用 `BlockNumber` 探测两个节点的连通性，超时时间 **5 秒**。节点不可达时提前报错返回 `false`，避免初始化成功但后续 API 静默失败。

```go
// v2 新增：建立连接后立即校验连通性
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
_, err = httpsClient.BlockNumber(ctx)
// wssClient 同样校验
```

v1 没有此校验，`Dial` 成功即返回 `true`，节点实际不可达时要等到业务调用才暴露。

#### 2. `UInit` 双连接释放

v2 的 `UInit` 会同时关闭并置空 `httpsClient` 和 `wssClient`：

```go
// v2
func UInit() {
    if httpsClient != nil { httpsClient.Close(); httpsClient = nil }
    if wssClient != nil   { wssClient.Close();   wssClient = nil   }
}
```

v1 只释放单个 `client`。

#### 3. `GetAddressByPrivateKey` 自动去除 0x 前缀

v1 要求调用方自行去掉 `0x` 再传入，v2 内部自动处理：

```go
// v2 新增
privateKey = strings.TrimPrefix(privateKey, "0x")
```

#### 4. `SendEth` 增加私钥格式前置校验

v1 直接 `crypto.HexToECDSA`，非法私钥会在底层报错。v2 在函数入口增加 `IsValidEthPrivateKey` 校验，并自动去除 `0x` 前缀，更早返回明确的 `false`：

```go
// v2 新增
if !IsValidEthPrivateKey(fromPrivateKey) {
    return false
}
fromPrivateKey = strings.TrimPrefix(fromPrivateKey, "0x")
```

#### 5. `SendErc20Token` 增加私钥格式前置校验

与 `SendEth` 同理，v2 在转账前增加私钥合法性校验 + 自动去 `0x` 前缀：

```go
// v2 新增
if !IsValidEthPrivateKey(fromPrivateKey) {
    return false
}
fromPrivateKey = strings.TrimPrefix(fromPrivateKey, "0x")
```

#### 6. 事件监听增加最大重试次数限制

v1 订阅失败后无限重试，可能导致程序在节点永久失效时陷入死循环。v2 引入 `maxRetries = 3`，超过次数后主动放弃并通过回调上报：

```go
// v2 新增
const maxRetries = 3
failCount := 0

// 订阅失败时
failCount++
if failCount >= maxRetries {
    callback.OnListenerStateChange(1, 3, "订阅失败次数超限，放弃重试")
    return
}

// 订阅成功后重置计数
failCount = 0
```

新增回调状态码 `result = 3`（放弃重试），v1 的状态码只有 0/1/2/4。

#### 7. 订阅失败回调携带重试进度信息

v1 失败回调只有固定字符串，v2 在 `info` 中附带当前失败次数和上限：

```go
// v1
callback.OnListenerStateChange(1, 1, "订阅失败: " + err.Error())

// v2
callback.OnListenerStateChange(1, 1, fmt.Sprintf("订阅失败(%d/%d): %s", failCount, maxRetries, err.Error()))
```

#### 8. `AddErc20TokenReceviceListener` 改用 `wssClient`

v1 订阅时使用的是通用 `client`（可能是 HTTP 连接），HTTP 不支持 `eth_subscribe`，在部分节点会静默失败。v2 明确使用 `wssClient`：

```go
// v1
sub, err := client.SubscribeFilterLogs(...)

// v2
sub, err := wssClient.SubscribeFilterLogs(...)
```

---

### 回调状态码变更

| `result` | v1 含义 | v2 含义 |
|----------|---------|---------|
| `0` | 运行中 | 运行中（不变） |
| `1` | 订阅失败 | 订阅失败（不变） |
| `2` | 断开 | 断开（不变） |
| `3` | — | **新增**：失败次数超限，已放弃重试 |
| `4` | 启动中 | 启动中（不变） |

---

## v1.0.0 (初始版本)

- 基于 `go-ethereum` 实现以太坊 SDK 核心功能
- 支持钱包生成、私钥导入、地址校验
- 支持 ETH 余额查询与转账
- 支持 ERC20 Token 查询与转账
- 支持 WebSocket 事件订阅，监听 ERC20 到账
- 通过 `gomobile` 编译为 Android AAR
- 完整 Android 钱包 Demo
