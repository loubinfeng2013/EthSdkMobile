package EthClient

import (
	utils "EthSdkMobile/src/common"
	erc20 "EthSdkMobile/src/contract"
	"context"
	"fmt"
	"math/big"
	"regexp"
	"strings"
	"sync"
	"time"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethclient"
)

// 全局变量和常量
const (
	MainNetWork    = "wss://ethereum.publicnode.com"
	SepoliaNetWork = "wss://sepolia.infura.io/ws/v3/1b3dddb22a464e11afd288a372fb8aef"
)

var (
	client        *ethclient.Client
	netWorkUrl    string
	callback      EthListenerCallback
	lastSubToken  ethereum.Subscription
	mutex         sync.Mutex
	stopTokenChan chan struct{}
)

// listener 预留给Android实现的回调
type EthListenerCallback interface {
	// 监听状态改变
	OnListenerStateChange(listenerType int, result int, info string)
	// 接受Token回调
	OnErc20TokenReceviced(tokenAddr string)
}

// 设置监听回调
func SetEthListenerCallback(cb EthListenerCallback) {
	callback = cb
}

// 根据url初始化sdk
// url: 网络url
// return: 成功返回true，失败false
func InitWithUrl(url string) bool {
	if url == "" {
		return false
	}
	var err error
	client, err = ethclient.Dial(url)
	if err != nil {
		fmt.Println("InitWithUrl Fail: ", url, " Error: ", err)
		return false
	}
	fmt.Println("InitWithUrl Success: ", url)
	netWorkUrl = url
	return true
}

// 根据类型初始化sdk
// netWorkType: 网络类型(1: MainNetWork,2: SepoliaNetWork)
// return: 成功返回true，失败false
func InitWithType(netWorkType int) bool {
	if netWorkType == 1 {
		return InitWithUrl(SepoliaNetWork)
	}
	return InitWithUrl(MainNetWork)
}

// 反初始化sdk
func UInit() {
	if client != nil {
		client.Close()
		client = nil
	}
}

// 返回当前web3网络
// return: 网络url
func GetNetWorkUrl() string {
	return netWorkUrl
}

// 根据私钥获取钱包地址
// return: 钱包地址
func GetAddressByPrivateKey(privateKey string) string {
	if !IsValidEthPrivateKey(privateKey) {
		return ""
	}
	privKey, err := crypto.HexToECDSA(privateKey)
	if err != nil {
		fmt.Println("GetAddressByPrivateKey Fail : ", err)
		return ""
	}
	return crypto.PubkeyToAddress(privKey.PublicKey).Hex()
}

// 生成钱包账户
// return: 钱包地址&&&&钱包私钥
func GenerateWallet() string {
	privateKey, err := crypto.GenerateKey()
	if err != nil {
		fmt.Println("GenerateWallet Fail : ", err)
		return ""
	}
	walletAddress := crypto.PubkeyToAddress(privateKey.PublicKey)
	privateKeyBytes := crypto.FromECDSA(privateKey)
	return walletAddress.Hex() + "&&&&" + hexutil.Encode(privateKeyBytes)
}

// 检查是否是以太地址
// address: 账户地址
// return: 是返回true，否false
func CheckEthAddress(address string) bool {
	re := regexp.MustCompile("^0x[0-9a-fA-F]{40}$")
	return re.MatchString(address)
}

// 检查是否是以太私钥
// key: 私钥
// return: 是返回true，否false
func IsValidEthPrivateKey(key string) bool {
	// 1. 去掉 0x 前缀
	key = strings.TrimPrefix(key, "0x")

	// 2. 私钥必须是 64 位十六进制字符串（32字节）
	if len(key) != 64 {
		return false
	}

	// 3. 必须是合法十六进制字符
	if !common.IsHexAddress("0x" + key[:40]) { // 借用地址校验判断hex合法性
		// 更严谨的 hex 校验
		if _, err := common.ParseHexOrString(key); err != nil {
			return false
		}
	}

	// 4. 最关键：校验 secp256k1 私钥范围合法性
	_, err := crypto.HexToECDSA(key)
	return err == nil
}

/******************* ETH ************************/

// 检查以太地址类型
// address: 账户地址
// return: 合约地址返回1，普通地址返回2
func GetEthAddressType(address string) int {
	if client == nil {
		return 0
	}
	addr := common.HexToAddress(address)
	byteCode, err := client.CodeAt(context.Background(), addr, nil)
	if err != nil {
		fmt.Println("GetEthAddressType Fail : ", err)
		return 0
	}
	isContract := len(byteCode) > 0
	if isContract {
		return 1
	} else {
		return 2
	}
}

// 根据地址查询余额
// address: 账户地址
// return: 账户余额(ETH)
func GetBalanceAtAddress(address string) string {
	if client == nil {
		fmt.Println("GetBalanceAtAddress Fail : Need Init")
		return ""
	}
	account := common.HexToAddress(address)
	balance, err := client.BalanceAt(context.Background(), account, nil)
	if err != nil {
		fmt.Println("GetBalanceAtAddress Fail : ", err)
	}

	return utils.WeiToEth(balance).Text('f', -1)
}

// 发送以太
// fromPrivateKey: 发送方私钥
// toAddress: 接受方地址
// toAddress: 数额(ETH)
// return: 是返回true，否false
func SendEth(fromPrivateKey string, toAddress string, val string) bool {
	if client == nil {
		return false
	}
	num, ok := new(big.Float).SetString(val)
	if !ok {
		fmt.Println("SendEth Fail : Val Error ,", val)
		return false
	}
	privateKey, err := crypto.HexToECDSA(fromPrivateKey)
	if err != nil {
		fmt.Println("SendEth Fail : ", err)
		return false
	}
	fromAddr := crypto.PubkeyToAddress(privateKey.PublicKey)
	toAddr := common.HexToAddress(toAddress)
	nonce, err := client.PendingNonceAt(context.Background(), fromAddr)
	if err != nil {
		fmt.Println("SendEth Fail : ", err)
		return false
	}
	gasPrice, err := client.SuggestGasPrice(context.Background())

	gasPrice = new(big.Int).Mul(gasPrice, big.NewInt(12))
	gasPrice = new(big.Int).Div(gasPrice, big.NewInt(10)) // 提高 20%

	gasLimit := uint64(21000) // in units
	if err != nil {
		fmt.Println("SendEth Fail : ", err)
		return false
	}
	tx := types.NewTransaction(nonce, toAddr, utils.EthToWei(num), gasLimit, gasPrice, []byte{})
	chainID, err := client.ChainID(context.Background())
	if err != nil {
		fmt.Println("SendEth Fail : ", err)
		return false
	}
	signedTx, err := types.SignTx(tx, types.NewEIP155Signer(chainID), privateKey)
	if err != nil {
		fmt.Println("SendEth Fail : ", err)
		return false
	}
	fmt.Println("SendEth Start")
	err = client.SendTransaction(context.Background(), signedTx)
	if err != nil {
		fmt.Println("SendEth Fail : ", err)
		return false
	}
	ctxTimeout, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	fmt.Println("WaitMined Start")
	receipt, err2 := bind.WaitMined(ctxTimeout, client, signedTx)
	fmt.Println("WaitMined End")
	if err2 != nil {
		fmt.Println("SendEth Fail : ", err2)
		return false
	}
	if receipt.Status != 1 {
		fmt.Println("SendEth Fail : Status Error")
		return false
	}
	return true
}

/******************* ERC20 ************************/

// 根据地址查询余额
// address: 账户地址
// return: 账户余额(ETH)
func GetTokenBalanceAtAddress(address string, erc20Addr string) string {
	if client == nil {
		fmt.Println("GetTokenBalanceAtAddress Fail : Need Init")
		return ""
	}
	if !CheckEthAddress(erc20Addr) {
		return ""
	}
	if GetEthAddressType(erc20Addr) != 1 {
		return ""
	}
	tokenAddr := common.HexToAddress(erc20Addr)
	token, err := erc20.NewErc20(tokenAddr, client)
	if err != nil {
		return ""
	}
	account := common.HexToAddress(address)
	balance, err := token.BalanceOf(nil, account)
	if err != nil {
		fmt.Println("GetTokenBalanceAtAddress Fail : ", err)
	}
	decimals, _ := token.Decimals(nil)

	return utils.RawToToken(balance, decimals)
}

// 获取erc20 token的Name
// address: 合约地址
// return: Name
func GetTokenNameAtAddress(erc20Addr string) string {
	if client == nil {
		fmt.Println("GetTokenNameAtAddress Fail : Need Init")
		return ""
	}
	if !CheckEthAddress(erc20Addr) {
		return ""
	}
	if GetEthAddressType(erc20Addr) != 1 {
		return ""
	}
	tokenAddr := common.HexToAddress(erc20Addr)
	token, err := erc20.NewErc20(tokenAddr, client)
	if err != nil {
		return ""
	}
	name, err := token.Name(nil)
	if err != nil {
		return ""
	}
	return name
}

// 获取erc20 token的Symbol
// address: 合约地址
// return: Symbol
func GetTokenSymbolAtAddress(erc20Addr string) string {
	if client == nil {
		fmt.Println("GetTokenNameAtAddress Fail : Need Init")
		return ""
	}
	if !CheckEthAddress(erc20Addr) {
		return ""
	}
	if GetEthAddressType(erc20Addr) != 1 {
		return ""
	}
	tokenAddr := common.HexToAddress(erc20Addr)
	token, err := erc20.NewErc20(tokenAddr, client)
	if err != nil {
		return ""
	}
	symbol, err := token.Symbol(nil)
	if err != nil {
		return ""
	}
	return symbol
}

// 检查是否是erc20的合约地址
// address: 账户地址
// return: 是返回true，否false
func CheckErc20Address(address string) bool {
	if client == nil {
		fmt.Println("CheckErc20Address Fail : Need Init")
		return false
	}
	if !CheckEthAddress(address) {
		return false
	}
	if GetEthAddressType(address) != 1 {
		return false
	}
	erc20Addr := common.HexToAddress(address)
	token, err := erc20.NewErc20(erc20Addr, client)
	if err != nil {
		return false
	}
	name, err := token.Name(nil)
	if err != nil {
		return false
	}
	symbol, err := token.Symbol(nil)
	if err != nil {
		return false
	}
	totalSupply, err := token.TotalSupply(nil)
	if err != nil {
		return false
	}
	fmt.Println(name, symbol, totalSupply)
	return true
}

// 发送erc20 token
func SendErc20Token(fromPrivateKey string, toAddress string, val string, erc20Addr string) bool {
	if client == nil {
		return false
	}
	if !CheckErc20Address(erc20Addr) {
		return false
	}
	tokenAddr := common.HexToAddress(erc20Addr)
	token, err := erc20.NewErc20(tokenAddr, client)
	if err != nil {
		return false
	}
	privKey, _ := crypto.HexToECDSA(fromPrivateKey)
	chainID, _ := client.ChainID(context.Background())
	decimals, _ := token.Decimals(nil)
	value, _ := utils.TokenToRaw(val, decimals)
	toAddr := common.HexToAddress(toAddress)
	//写操作必须要有auth
	auth, _ := bind.NewKeyedTransactorWithChainID(privKey, chainID)
	//转账交易
	tx, err := token.Transfer(auth, toAddr, value)
	//确认交易
	ctxTimeout, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	fmt.Println("WaitMined Start")
	receipt, err2 := bind.WaitMined(ctxTimeout, client, tx)
	fmt.Println("WaitMined End")
	if err2 != nil {
		fmt.Println("SendErc20Token Fail : ", err2)
		return false
	}
	if receipt.Status != 1 {
		fmt.Println("SendErc20Token Fail : Status Error")
		return false
	}
	return true
}

// 设置接受token监听
func AddErc20TokenReceviceListener(walletAddr string) {

	if client == nil {
		return
	}

	mutex.Lock()
	// 停止旧监听
	if stopTokenChan != nil {
		close(stopTokenChan)
	}
	if lastSubToken != nil {
		lastSubToken.Unsubscribe()
	}
	stopTokenChan = make(chan struct{})
	mutex.Unlock()

	toAddr := common.HexToHash(walletAddr)
	transferEvent := crypto.Keccak256Hash([]byte("Transfer(address,address,uint256)"))

	topics := [][]common.Hash{
		{transferEvent},
		nil,
		{toAddr},
	}

	query := ethereum.FilterQuery{Topics: topics}

	// 🔥 独立 goroutine，不阻塞
	go func() {
		for {
			select {
			case <-stopTokenChan:
				return
			default:
			}

			logs := make(chan types.Log)
			if callback != nil {
				callback.OnListenerStateChange(1, 4, "订阅开始")
			}
			sub, err := client.SubscribeFilterLogs(context.Background(), query, logs)

			if err != nil {
				if callback != nil {
					callback.OnListenerStateChange(1, 1, "订阅失败: "+err.Error())
				}
				time.Sleep(3 * time.Second)
				continue
			}

			mutex.Lock()
			lastSubToken = sub
			mutex.Unlock()

			if callback != nil {
				callback.OnListenerStateChange(1, 0, "订阅运行中")
			}
			for {
				select {
				case <-stopTokenChan:
					sub.Unsubscribe()
					return
				case err := <-sub.Err():
					if callback != nil && err != nil {
						callback.OnListenerStateChange(1, 2, "订阅断开: "+err.Error())
					}
					sub.Unsubscribe()
					time.Sleep(2 * time.Second)
					goto RETRY
				case vLog := <-logs:
					if callback != nil {
						callback.OnErc20TokenReceviced(vLog.Address.Hex())
					}
				}
			}
		RETRY:
		}
	}()
}

// 停止监听函数
func StopAllListeners() {
	mutex.Lock()
	defer mutex.Unlock()
	if stopTokenChan != nil {
		close(stopTokenChan)
		stopTokenChan = nil
	}
	if lastSubToken != nil {
		lastSubToken.Unsubscribe()
		lastSubToken = nil
	}
}
