package utils

import (
	"math/big"
	"strings"
)

var (
	zero = big.NewInt(0)
	one  = big.NewInt(1)
	ten  = big.NewInt(10)
)

// TokenToRaw 人类可读金额 → 链上最小单位（类似 eth2wei）
// 例：1 USDC(6位) → 1000000
// 例：1 ETH(18位) → 1000000000000000000
func TokenToRaw(amountStr string, decimals uint8) (*big.Int, error) {
	// 处理小数
	parts := strings.Split(amountStr, ".")
	integer := parts[0]
	decimal := ""
	if len(parts) > 1 {
		decimal = parts[1]
	}

	// 小数部分补0 / 截断
	if uint8(len(decimal)) < decimals {
		decimal += strings.Repeat("0", int(decimals)-len(decimal))
	} else {
		decimal = decimal[:decimals]
	}

	// 拼接成完整字符串
	full := integer + decimal

	// 转 big.Int
	res := new(big.Int)
	_, ok := res.SetString(full, 10)
	if !ok {
		return zero, nil
	}
	return res, nil
}

// RawToToken 链上最小单位 → 人类可读金额（类似 wei2eth）
// 例：1000000 USDC → 1
// 例：1e18 wei → 1 ETH
func RawToToken(raw *big.Int, decimals uint8) string {
	if raw.Cmp(zero) == 0 {
		return "0"
	}

	// 10^decimals
	pow := new(big.Int).Exp(ten, big.NewInt(int64(decimals)), nil)

	// 整数部分
	integer := new(big.Int).Div(raw, pow)

	// 小数部分
	remainder := new(big.Int).Mod(raw, pow)
	decimalStr := remainder.String()

	// 前面补 0
	decimalStr = strings.Repeat("0", int(decimals)-len(decimalStr)) + decimalStr

	// 去掉末尾无用的 0
	decimalStr = strings.TrimRight(decimalStr, "0")
	if decimalStr == "" {
		return integer.String()
	}

	return integer.String() + "." + decimalStr
}

// Wei → ETH
func WeiToEth(wei *big.Int) *big.Float {
	return new(big.Float).Quo(new(big.Float).SetInt(wei), big.NewFloat(1e18))
}

// ETH → Wei
func EthToWei(eth *big.Float) *big.Int {
	wei := new(big.Float).Mul(eth, big.NewFloat(1e18))
	res, _ := wei.Int(nil)
	return res
}
