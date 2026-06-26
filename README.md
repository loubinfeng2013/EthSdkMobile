# EthSdkMobile

基于 Go 语言开发的以太坊移动端 SDK，通过 `gomobile` 编译为原生库（Android AAR / iOS Framework）。提供钱包管理、ETH/ERC20 转账、实时事件监听等功能，接口简单易用。

## 目录

- [安装](#安装)
- [初始化](#初始化)
- [API 参考](#api-参考)
  - [初始化相关](#初始化相关)
  - [钱包](#钱包)
  - [ETH](#eth)
  - [ERC20](#erc20)
  - [事件监听](#事件监听)
- [回调接口](#回调接口)
- [常量](#常量)

---

## 安装

将预编译的 Android AAR 导入项目：

```
sdk/release/android/
├── eth-sdk-android.aar
└── eth-sdk-android-sources.jar
```

在 Android `build.gradle` 中添加依赖：

```groovy
implementation fileTree(include: ['*.aar'], dir: 'libs')
```

---

## 初始化

调用任何需要网络的 API 之前，必须先初始化 SDK。v2 严格区分 HTTPS 节点（用于查询和交易广播）和 WSS 节点（用于事件订阅）。

### `InitWithUrl(httpsUrl string, wssUrl string) bool`

使用自定义节点初始化 SDK。

| 参数 | 类型 | 说明 |
|------|------|------|
| `httpsUrl` | string | HTTPS RPC 节点地址，必须以 `https://` 开头 |
| `wssUrl` | string | WebSocket 节点地址，必须以 `wss://` 开头 |

**返回值：** 成功返回 `true`，失败返回 `false`。

> 初始化时会对两个节点分别做连通性探测（超时 5 秒），节点不可达时提前返回 `false`。

```go
ok := InitWithUrl("https://ethereum.publicnode.com", "wss://ethereum.publicnode.com")
```

---

### `InitWithType(netWorkType int) bool`

使用预置网络类型初始化 SDK。

| 参数 | 类型 | 说明 |
|------|------|------|
| `netWorkType` | int | `1` = Sepolia 测试网，其他值 = 以太坊主网 |

**返回值：** 成功返回 `true`，失败返回 `false`。

```go
ok := InitWithType(1) // Sepolia 测试网
ok := InitWithType(2) // 以太坊主网
```

---

### `UInit()`

断开连接并释放资源。不再使用 SDK 时调用（如应用退到后台）。

```go
UInit()
```

---

### `GetNetWorkUrl() string`

获取当前连接的网络标识。

**返回值：** 当前 HTTPS 节点的域名字符串（去掉 `https://` 前缀）。

```go
url := GetNetWorkUrl()
```

---

## API 参考

### 初始化相关

| 函数 | 说明 |
|------|------|
| `InitWithUrl(httpsUrl, wssUrl string) bool` | 使用自定义节点初始化 |
| `InitWithType(netWorkType int) bool` | 使用预置网络类型初始化 |
| `UInit()` | 断开连接，释放资源 |
| `GetNetWorkUrl() string` | 获取当前网络标识 |
| `SetEthListenerCallback(cb EthListenerCallback)` | 注册事件回调 |

---

### 钱包

#### `GenerateWallet() string`

生成一个新的以太坊钱包（私钥 + 地址）。

**返回值：** 格式为 `地址&&&&私钥` 的字符串，失败返回空字符串。

```go
result := GenerateWallet()
// "0xAbc...&&&&0x1a2b3c..."
parts := strings.Split(result, "&&&&")
address    := parts[0]
privateKey := parts[1]
```

---

#### `GetAddressByPrivateKey(privateKey string) string`

通过私钥推导钱包地址。支持带或不带 `0x` 前缀。

| 参数 | 类型 | 说明 |
|------|------|------|
| `privateKey` | string | 十六进制私钥（支持 `0x` 前缀） |

**返回值：** 对应的以太坊地址，失败返回空字符串。

```go
addr := GetAddressByPrivateKey("0x1a2b3c...")
```

---

#### `CheckEthAddress(address string) bool`

校验字符串是否为合法的以太坊地址。

| 参数 | 类型 | 说明 |
|------|------|------|
| `address` | string | 待校验的地址 |

**返回值：** 合法（`0x` + 40位十六进制）返回 `true`，否则 `false`。

```go
ok := CheckEthAddress("0xAbc123...")
```

---

#### `IsValidEthPrivateKey(key string) bool`

校验字符串是否为合法的 secp256k1 私钥。

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | string | 待校验的十六进制私钥 |

**返回值：** 合法返回 `true`，否则 `false`。

```go
ok := IsValidEthPrivateKey("0x1a2b3c...")
```

---

### ETH

#### `GetEthAddressType(address string) int`

判断一个地址是合约地址还是普通账户（EOA）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `address` | string | 以太坊地址 |

**返回值：**

| 值 | 含义 |
|----|------|
| `0` | 错误 / 未初始化 |
| `1` | 合约地址 |
| `2` | 普通账户（EOA） |

```go
addrType := GetEthAddressType("0xAbc123...")
```

---

#### `GetBalanceAtAddress(address string) string`

查询地址的 ETH 余额。

| 参数 | 类型 | 说明 |
|------|------|------|
| `address` | string | 钱包地址 |

**返回值：** ETH 余额字符串（如 `"1.234567"`），失败返回空字符串。

```go
balance := GetBalanceAtAddress("0xAbc123...")
// "1.234567"
```

---

#### `SendEth(fromPrivateKey string, toAddress string, val string) bool`

发送 ETH，等待链上确认（最长 30 秒）。

> Gas 价格自动获取并上浮 **20%** 以提升打包速度。支持带 `0x` 前缀的私钥。

| 参数 | 类型 | 说明 |
|------|------|------|
| `fromPrivateKey` | string | 发送方私钥 |
| `toAddress` | string | 收款方以太坊地址 |
| `val` | string | 发送金额，单位 ETH（如 `"0.1"`） |

**返回值：** 交易上链成功返回 `true`，否则 `false`。

```go
ok := SendEth("0x1a2b...", "0x收款地址...", "0.5")
```

---

### ERC20

#### `CheckErc20Address(address string) bool`

通过查询合约的 `name`、`symbol`、`totalSupply` 验证是否为有效的 ERC20 合约。

| 参数 | 类型 | 说明 |
|------|------|------|
| `address` | string | 待验证的合约地址 |

**返回值：** 是有效 ERC20 合约返回 `true`，否则 `false`。

```go
ok := CheckErc20Address("0xToken合约...")
```

---

#### `GetTokenBalanceAtAddress(address string, erc20Addr string) string`

查询钱包地址持有的 ERC20 Token 余额。

| 参数 | 类型 | 说明 |
|------|------|------|
| `address` | string | 钱包地址 |
| `erc20Addr` | string | ERC20 合约地址 |

**返回值：** 可读格式的余额字符串（如 `"100.5"`），失败返回空字符串。

```go
balance := GetTokenBalanceAtAddress("0x钱包地址...", "0xToken合约...")
// "100.5"
```

---

#### `GetTokenNameAtAddress(erc20Addr string) string`

获取 ERC20 合约的 `name` 字段。

| 参数 | 类型 | 说明 |
|------|------|------|
| `erc20Addr` | string | ERC20 合约地址 |

**返回值：** Token 名称（如 `"USD Coin"`），失败返回空字符串。

```go
name := GetTokenNameAtAddress("0xToken合约...")
// "USD Coin"
```

---

#### `GetTokenSymbolAtAddress(erc20Addr string) string`

获取 ERC20 合约的 `symbol` 字段。

| 参数 | 类型 | 说明 |
|------|------|------|
| `erc20Addr` | string | ERC20 合约地址 |

**返回值：** Token 符号（如 `"USDC"`），失败返回空字符串。

```go
symbol := GetTokenSymbolAtAddress("0xToken合约...")
// "USDC"
```

---

#### `SendErc20Token(fromPrivateKey string, toAddress string, val string, erc20Addr string) bool`

转账 ERC20 Token，等待链上确认（最长 30 秒）。支持带 `0x` 前缀的私钥。

| 参数 | 类型 | 说明 |
|------|------|------|
| `fromPrivateKey` | string | 发送方私钥 |
| `toAddress` | string | 收款方以太坊地址 |
| `val` | string | 转账金额，单位为 Token（如 `"10.5"`） |
| `erc20Addr` | string | ERC20 合约地址 |

**返回值：** 交易上链成功返回 `true`，否则 `false`。

```go
ok := SendErc20Token("0x1a2b...", "0x收款地址...", "10.5", "0xToken合约...")
```

---

### 事件监听

#### `SetEthListenerCallback(cb EthListenerCallback)`

注册事件回调实现。必须在启动监听前调用。

| 参数 | 类型 | 说明 |
|------|------|------|
| `cb` | EthListenerCallback | 回调接口实现 |

```go
SetEthListenerCallback(myCallbackImpl)
```

---

#### `AddErc20TokenReceviceListener(walletAddr string)`

通过 WebSocket 订阅监听指定钱包地址的 ERC20 Token 到账事件，在独立 goroutine 中运行，不阻塞调用方。

重试策略：
- **订阅失败**：最多连续失败 3 次，超限后回调 `result=3` 并放弃，每次失败间隔 3 秒。
- **运行中断线**：自动触发重连（间隔 2 秒），重连失败计数独立重新计算，同样最多 3 次。

> 用新地址再次调用时，会自动停止旧监听并启动新监听。

| 参数 | 类型 | 说明 |
|------|------|------|
| `walletAddr` | string | 要监听的钱包地址 |

```go
AddErc20TokenReceviceListener("0x钱包地址...")
```

---

#### `StopAllListeners()`

停止所有事件订阅，释放相关资源。

```go
StopAllListeners()
```

---

## 回调接口

实现 `EthListenerCallback` 接口以接收异步事件：

```go
type EthListenerCallback interface {
    // 监听状态发生变化时回调
    // listenerType: 1 = ERC20 Token 监听
    // result: 见下方状态码说明
    // info: 可读描述信息
    OnListenerStateChange(listenerType int, result int, info string)

    // 收到 ERC20 Token 转入时回调
    // tokenAddr: 收到的 Token 合约地址
    OnErc20TokenReceviced(tokenAddr string)
}
```

### 监听状态码

| `result` | 含义 |
|----------|------|
| `0` | 订阅运行中 |
| `1` | 订阅失败（含重试次数信息） |
| `2` | 订阅断开，准备重连 |
| `3` | 失败次数超限（3次），已放弃重试 |
| `4` | 订阅启动中 |

---

## 常量

| 常量 | 值 | 说明 |
|------|----|------|
| `MainHttps` | `https://ethereum.publicnode.com` | 以太坊主网 HTTPS 节点 |
| `MainWss` | `wss://ethereum.publicnode.com` | 以太坊主网 WSS 节点 |
| `SepoliaHttps` | `https://sepolia.infura.io/v3/...` | Sepolia 测试网 HTTPS 节点 |
| `SepoliaWss` | `wss://sepolia.infura.io/ws/v3/...` | Sepolia 测试网 WSS 节点 |

---

## 开源协议

MIT

## 联系方式(交个朋友)

loubinfeng2013@gmail.com
