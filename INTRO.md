# EthSdkMobile —— 为移动端开发者打造的以太坊 Web3 SDK

> 开源地址：[https://github.com/loubinfeng2013/EthSdkMobile](https://github.com/loubinfeng2013/EthSdkMobile)

---

## 背景

目前市面上已经有不少 Web3 相关的 JS/Python SDK，但**针对移动原生端（Android / iOS / Flutter）的以太坊 SDK 却相对稀缺**，开发者要么需要自己封装 go-ethereum，要么借助 Web 容器走 JS 桥，体验和性能都打了折扣。

**EthSdkMobile** 的目标就是填补这个空白：

- 底层使用 **Go** 语言，基于 `go-ethereum` 官方库实现，保证链上交互的安全性与可靠性；
- 通过 **gomobile** 编译为各平台原生库（Android AAR / iOS Framework）；
- 提供**极简的 API**，让移动端开发者无需了解区块链底层细节，即可快速接入以太坊网络。

---

## 当前状态

| 平台 | SDK | Demo |
|------|-----|------|
| Android | ✅ 已完成（AAR） | ✅ 完整以太坊钱包 Demo |
| iOS | 🚧 开发中 | — |
| Flutter | 🚧 开发中 | — |

---

## SDK 核心能力

### 一、连接以太坊网络

支持两种初始化方式：

```java
// 方式一：使用预置节点（1=Sepolia 测试网，其他=主网）
EthClient.initWithType(1);

// 方式二：使用自定义 WebSocket 节点
EthClient.initWithUrl("wss://your-node-url");
```

资源释放也只需一行：

```java
EthClient.uInit();
```

---

### 二、钱包管理

**生成新钱包**（一键生成私钥和地址）：

```java
String result = EthClient.generateWallet();
// 返回格式："0xAddress&&&&0xPrivateKey"
String[] parts = result.split("&&&&");
String address    = parts[0];
String privateKey = parts[1];
```

**通过私钥恢复钱包**：

```java
String address = EthClient.getAddressByPrivateKey("0x私钥...");
```

**地址 / 私钥格式校验**：

```java
boolean ok1 = EthClient.checkEthAddress("0xAbc...");      // 校验地址
boolean ok2 = EthClient.isValidEthPrivateKey("0x1a2b.."); // 校验私钥
```

---

### 三、ETH 查询与转账

**查询余额**（直接返回 ETH 字符串，无需手动处理 Wei 转换）：

```java
String balance = EthClient.getBalanceAtAddress("0xYourAddress...");
// 返回示例："1.234567"
```

**发送 ETH**（自动获取 Gas 并提高 20%，等待链上确认）：

```java
boolean success = EthClient.sendEth("0x私钥", "0x收款地址", "0.1");
```

---

### 四、ERC20 Token 支持

```java
// 验证是否为有效 ERC20 合约
boolean isErc20 = EthClient.checkErc20Address("0xTokenContract...");

// 查询 Token 余额
String balance = EthClient.getTokenBalanceAtAddress("0x钱包地址", "0xToken合约");

// 获取 Token 基本信息
String name   = EthClient.getTokenNameAtAddress("0xToken合约");   // 如 "USD Coin"
String symbol = EthClient.getTokenSymbolAtAddress("0xToken合约"); // 如 "USDC"

// 发送 ERC20 Token
boolean success = EthClient.sendErc20Token("0x私钥", "0x收款地址", "10.5", "0xToken合约");
```

---

### 五、实时事件监听

通过 WebSocket 长连接监听钱包的 ERC20 Token 到账事件，**断线自动重连**：

```java
// 注册回调
EthClient.setEthListenerCallback(new EthListenerCallback() {
    @Override
    public void onErc20TokenReceviced(String tokenAddr) {
        // 收到 Token 转入，tokenAddr 为合约地址
        // 可在此刷新余额或弹出通知
    }

    @Override
    public void onListenerStateChange(long listenerType, long result, String info) {
        // 监听状态变化：0=运行中, 1=订阅失败, 2=断开, 4=启动中
    }
});

// 开始监听
EthClient.addErc20TokenReceviceListener("0x你的钱包地址");

// 停止监听
EthClient.stopAllListeners();
```

---

## Android 集成教程

### 第一步：引入 AAR

将 `eth-sdk-android.aar` 放入 `app/libs/` 目录，然后在 `build.gradle` 中添加：

```groovy
dependencies {
    implementation fileTree(include: ['*.aar'], dir: 'libs')
}
```

### 第二步：SDK 初始化

建议在 `Application` 的 `onCreate` 或前台回调中异步初始化（避免主线程网络请求）：

```java
new Thread(() -> {
    boolean ok = EthClient.initWithType(1); // Sepolia 测试网
    Log.d("EthSdk", "init result: " + ok);
}).start();
```

### 第三步：调用 API

所有网络相关 API（余额查询、转账、监听等）均应在**子线程**执行，结果通过 `runOnUiThread` 回到主线程更新 UI。

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {
    String balance = EthClient.getBalanceAtAddress(walletAddress);
    runOnUiThread(() -> tvBalance.setText(balance));
});
```

### 第四步：资源释放

应用退到后台时及时释放连接：

```java
EthClient.stopAllListeners();
EthClient.uInit();
```

---

## Android Demo 预览

项目内置了一个**完整的以太坊钱包 Demo App**（`android/Web3AndroidDemo`），展示了 SDK 全部核心功能：

| 功能 | 说明 |
|------|------|
| 创建钱包 | 一键生成助记词/私钥，自动派生地址 |
| 导入钱包 | 支持手动输入私钥或扫码二维码导入 |
| 查看余额 | 显示 ETH 余额及所有已添加 ERC20 Token 余额 |
| 添加 Token | 输入合约地址或扫码，自动识别 Token 信息 |
| 发送 ETH | 输入地址和金额，支持扫码填写收款地址 |
| 发送 Token | 选择 Token，转账至任意地址 |
| 接收资产 | 展示钱包二维码，供对方扫码转账 |
| 实时监听 | WebSocket 实时接收 Token 到账通知，自动刷新余额 |
| 切换网络 | 支持主网 / Sepolia 测试网 / 自定义节点切换 |
| 下拉刷新 | 手动刷新 ETH 及所有 Token 余额 |

---

## 项目结构

```
EthSdkMobile/
├── sdk/
│   ├── src/              # Go 源码（go-ethereum 封装）
│   │   ├── ethsdk.go     # 核心 API 实现
│   │   ├── common/       # 工具函数（Wei/ETH 转换等）
│   │   └── contract/     # ERC20 合约 ABI 绑定
│   └── release/
│       └── android/      # 编译产物：eth-sdk-android.aar
├── android/
│   └── Web3AndroidDemo/  # Android 完整钱包 Demo
├── ios/                  # iOS 支持（开发中）
└── flutter/              # Flutter 支持（开发中）
```

---

## 技术亮点

- **Go + gomobile**：底层逻辑统一用 Go 编写，跨平台编译，保持 Android / iOS / Flutter 行为一致；
- **WebSocket 长连接**：使用 `wss://` 节点，支持事件订阅和断线重连，告别低效的轮询；
- **自动 Gas 管理**：发送交易时自动获取建议 Gas Price 并上浮 20%，提高打包成功率；
- **链上确认等待**：`SendEth` / `SendErc20Token` 均等待交易上链（最长 30 秒），避免"已发送但未确认"的歧义；
- **API 极简设计**：所有复杂的链上交互（单位换算、签名、Nonce 管理等）对上层完全透明，移动端一行代码完成转账。

---

## 快速开始（Android）

1. Clone 仓库：

   ```bash
   git clone https://github.com/loubinfeng2013/EthSdkMobile.git
   ```

2. 用 Android Studio 打开 `android/Web3AndroidDemo`；

3. 运行 Demo，体验完整钱包功能；

4. 将 `sdk/release/android/eth-sdk-android.aar` 集成到你自己的项目中。

---

## 路线图

- [x] Go SDK 核心实现
- [x] Android AAR 编译 & Demo
- [ ] iOS Framework 编译 & Demo
- [ ] Flutter Plugin & Demo
- [ ] 支持更多 ERC 标准（ERC721 NFT 等）
- [ ] 支持 HD 钱包 / BIP39 助记词

---

## 开源协议

MIT License，自由使用，欢迎 PR 和 Issue！

---

如果这个项目对你有帮助，欢迎到 GitHub 给个 ⭐ Star 支持一下：

**[https://github.com/loubinfeng2013/EthSdkMobile](https://github.com/loubinfeng2013/EthSdkMobile)**
