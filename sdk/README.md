# EthSdkMobile

A Go-based Ethereum SDK for mobile platforms (Android & iOS), compiled via `gomobile`. It provides wallet management, ETH/ERC20 transfers, and real-time event listening through a simple native interface.

## Table of Contents

- [Installation](#installation)
- [Initialization](#initialization)
- [API Reference](#api-reference)
  - [Setup](#setup)
  - [Wallet](#wallet)
  - [ETH](#eth)
  - [ERC20](#erc20)
  - [Event Listener](#event-listener)
- [Callback Interface](#callback-interface)
- [Constants](#constants)

---

## Installation

Import the prebuilt Android AAR into your project:

```
release/
└── android/
    ├── eth-sdk-android.aar
    └── eth-sdk-android-sources.jar
```

Add to your Android `build.gradle`:

```groovy
implementation fileTree(include: ['*.aar'], dir: 'libs')
```

---

## Initialization

Before calling any network-dependent API, you must initialize the SDK with an Ethereum node URL.

### `InitWithUrl(url string) bool`

Initialize the SDK with a custom WebSocket node URL.

| Parameter | Type   | Description              |
|-----------|--------|--------------------------|
| `url`     | string | WebSocket RPC endpoint   |

**Returns:** `true` on success, `false` on failure.

```go
ok := InitWithUrl("wss://ethereum.publicnode.com")
```

---

### `InitWithType(netWorkType int) bool`

Initialize the SDK with a preset network type.

| Parameter      | Type | Description                              |
|----------------|------|------------------------------------------|
| `netWorkType`  | int  | `1` = Sepolia Testnet, other = Mainnet   |

**Returns:** `true` on success, `false` on failure.

```go
ok := InitWithType(1) // Sepolia
ok := InitWithType(2) // Mainnet
```

---

### `UInit()`

Disconnect and release the client connection. Call this when the SDK is no longer needed.

```go
UInit()
```

---

### `GetNetWorkUrl() string`

Returns the currently connected network URL.

**Returns:** The active WebSocket URL string.

```go
url := GetNetWorkUrl()
```

---

## API Reference

### Setup

| Function | Description |
|---|---|
| `InitWithUrl(url string) bool` | Connect to a custom node URL |
| `InitWithType(netWorkType int) bool` | Connect using a preset network type |
| `UInit()` | Disconnect and clean up |
| `GetNetWorkUrl() string` | Get the current network URL |
| `SetEthListenerCallback(cb EthListenerCallback)` | Register event callback |

---

### Wallet

#### `GenerateWallet() string`

Generates a new Ethereum wallet (private key + address).

**Returns:** A string in the format `address&&&&privateKey`, or empty string on failure.

```go
result := GenerateWallet()
// "0xAbc...&&&&0x1a2b3c..."
parts := strings.Split(result, "&&&&")
address    := parts[0]
privateKey := parts[1]
```

---

#### `GetAddressByPrivateKey(privateKey string) string`

Derives the wallet address from a private key.

| Parameter    | Type   | Description                        |
|--------------|--------|------------------------------------|
| `privateKey` | string | Hex-encoded private key (with or without `0x` prefix) |

**Returns:** The corresponding Ethereum address, or empty string on error.

```go
addr := GetAddressByPrivateKey("0x1a2b3c...")
```

---

#### `CheckEthAddress(address string) bool`

Validates whether a string is a valid Ethereum address.

| Parameter | Type   | Description         |
|-----------|--------|---------------------|
| `address` | string | Address to validate |

**Returns:** `true` if valid (`0x` + 40 hex chars), `false` otherwise.

```go
ok := CheckEthAddress("0xAbc123...")
```

---

#### `IsValidEthPrivateKey(key string) bool`

Validates whether a string is a valid secp256k1 private key.

| Parameter | Type   | Description                        |
|-----------|--------|------------------------------------|
| `key`     | string | Hex-encoded private key to validate |

**Returns:** `true` if valid, `false` otherwise.

```go
ok := IsValidEthPrivateKey("0x1a2b3c...")
```

---

### ETH

#### `GetEthAddressType(address string) int`

Checks whether an address is a contract or an EOA (externally owned account).

| Parameter | Type   | Description          |
|-----------|--------|----------------------|
| `address` | string | Ethereum address     |

**Returns:**

| Value | Meaning              |
|-------|----------------------|
| `0`   | Error / not initialized |
| `1`   | Contract address     |
| `2`   | Regular (EOA) address |

```go
addrType := GetEthAddressType("0xAbc123...")
```

---

#### `GetBalanceAtAddress(address string) string`

Queries the ETH balance of an address.

| Parameter | Type   | Description      |
|-----------|--------|------------------|
| `address` | string | Wallet address   |

**Returns:** Balance as a decimal string in ETH (e.g. `"1.5"`), or empty string on error.

```go
balance := GetBalanceAtAddress("0xAbc123...")
// "1.234567"
```

---

#### `SendEth(fromPrivateKey string, toAddress string, val string) bool`

Sends ETH from one address to another. Waits up to 30 seconds for on-chain confirmation.

> Gas price is automatically fetched and increased by **20%** to improve confirmation speed.

| Parameter      | Type   | Description                      |
|----------------|--------|----------------------------------|
| `fromPrivateKey` | string | Sender's private key           |
| `toAddress`    | string | Recipient's Ethereum address     |
| `val`          | string | Amount to send in ETH (e.g. `"0.1"`) |

**Returns:** `true` if the transaction is confirmed successfully, `false` otherwise.

```go
ok := SendEth("0x1a2b...", "0xRecipient...", "0.5")
```

---

### ERC20

#### `CheckErc20Address(address string) bool`

Validates whether an address is a deployed ERC20 contract by querying `name`, `symbol`, and `totalSupply`.

| Parameter | Type   | Description             |
|-----------|--------|-------------------------|
| `address` | string | Contract address to check |

**Returns:** `true` if it is a valid ERC20 contract, `false` otherwise.

```go
ok := CheckErc20Address("0xTokenContract...")
```

---

#### `GetTokenBalanceAtAddress(address string, erc20Addr string) string`

Queries the ERC20 token balance of a wallet address.

| Parameter   | Type   | Description                  |
|-------------|--------|------------------------------|
| `address`   | string | Wallet address               |
| `erc20Addr` | string | ERC20 contract address       |

**Returns:** Token balance as a human-readable decimal string (e.g. `"100.5"`), or empty string on error.

```go
balance := GetTokenBalanceAtAddress("0xWallet...", "0xToken...")
// "100.5"
```

---

#### `GetTokenNameAtAddress(erc20Addr string) string`

Gets the `name` field of an ERC20 token contract.

| Parameter   | Type   | Description            |
|-------------|--------|------------------------|
| `erc20Addr` | string | ERC20 contract address |

**Returns:** Token name string (e.g. `"USD Coin"`), or empty string on error.

```go
name := GetTokenNameAtAddress("0xToken...")
// "USD Coin"
```

---

#### `GetTokenSymbolAtAddress(erc20Addr string) string`

Gets the `symbol` field of an ERC20 token contract.

| Parameter   | Type   | Description            |
|-------------|--------|------------------------|
| `erc20Addr` | string | ERC20 contract address |

**Returns:** Token symbol string (e.g. `"USDC"`), or empty string on error.

```go
symbol := GetTokenSymbolAtAddress("0xToken...")
// "USDC"
```

---

#### `SendErc20Token(fromPrivateKey string, toAddress string, val string, erc20Addr string) bool`

Transfers ERC20 tokens from one address to another. Waits up to 30 seconds for on-chain confirmation.

| Parameter      | Type   | Description                              |
|----------------|--------|------------------------------------------|
| `fromPrivateKey` | string | Sender's private key                   |
| `toAddress`    | string | Recipient's Ethereum address             |
| `val`          | string | Amount to send in token units (e.g. `"10.5"`) |
| `erc20Addr`    | string | ERC20 contract address                   |

**Returns:** `true` if the transaction is confirmed successfully, `false` otherwise.

```go
ok := SendErc20Token("0x1a2b...", "0xRecipient...", "10.5", "0xToken...")
```

---

### Event Listener

#### `SetEthListenerCallback(cb EthListenerCallback)`

Registers the callback implementation for receiving events. Must be called before starting any listener.

| Parameter | Type                   | Description              |
|-----------|------------------------|--------------------------|
| `cb`      | EthListenerCallback    | Callback implementation  |

```go
SetEthListenerCallback(myCallbackImpl)
```

---

#### `AddErc20TokenReceviceListener(walletAddr string)`

Starts listening for incoming ERC20 token transfers to the specified wallet address via WebSocket subscription. Automatically reconnects on disconnection.

> Calling this again with a new address will stop the previous listener and start a new one.

| Parameter    | Type   | Description               |
|--------------|--------|---------------------------|
| `walletAddr` | string | Wallet address to monitor |

```go
AddErc20TokenReceviceListener("0xWallet...")
```

---

#### `StopAllListeners()`

Stops all active event subscriptions and cleans up resources.

```go
StopAllListeners()
```

---

## Callback Interface

Implement `EthListenerCallback` to receive asynchronous events:

```go
type EthListenerCallback interface {
    // Called when listener state changes
    // listenerType: 1 = ERC20 token listener
    // result: 0 = running, 1 = subscribe failed, 2 = disconnected, 4 = starting
    // info: human-readable description
    OnListenerStateChange(listenerType int, result int, info string)

    // Called when an ERC20 token transfer is received
    // tokenAddr: the ERC20 contract address of the received token
    OnErc20TokenReceviced(tokenAddr string)
}
```

### Listener State Codes

| `result` | Meaning         |
|----------|-----------------|
| `0`      | Running normally |
| `1`      | Subscribe failed |
| `2`      | Disconnected     |
| `4`      | Starting         |

---

## Constants

| Constant        | Value                                                      | Description       |
|-----------------|------------------------------------------------------------|-------------------|
| `MainNetWork`   | `wss://ethereum.publicnode.com`                            | Ethereum Mainnet  |
| `SepoliaNetWork`| `wss://sepolia.infura.io/ws/v3/...`                        | Sepolia Testnet   |

---

## License

MIT
