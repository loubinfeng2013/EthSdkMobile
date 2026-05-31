package com.web3.eth.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WalletPrefs {

    private static final String PREFS_NAME = "wallet_prefs";
    private static final String KEY_WALLET_ADDRESS = "wallet_address";
    private static final String KEY_PRIVATE_KEY = "private_key";
    private static final String KEY_TOKEN_CONTRACTS = "token_contracts";
    private static final String KEY_NETWORK_LIST = "network_list";
    private static final String KEY_CURRENT_NETWORK = "current_network";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ─────────────────────────────────────────────
    //  钱包地址
    // ─────────────────────────────────────────────

    public static void setWalletAddress(Context context, String address) {
        getPrefs(context).edit().putString(KEY_WALLET_ADDRESS, address).apply();
    }

    public static String getWalletAddress(Context context) {
        return getPrefs(context).getString(KEY_WALLET_ADDRESS, null);
    }

    // ─────────────────────────────────────────────
    //  私钥
    // ─────────────────────────────────────────────

    public static void setPrivateKey(Context context, String privateKey) {
        getPrefs(context).edit().putString(KEY_PRIVATE_KEY, privateKey).apply();
    }

    public static String getPrivateKey(Context context) {
        return getPrefs(context).getString(KEY_PRIVATE_KEY, null);
    }

    // ─────────────────────────────────────────────
    //  工具方法
    // ─────────────────────────────────────────────

    /** 是否已有钱包 */
    public static boolean hasWallet(Context context) {
        String address = getWalletAddress(context);
        String privateKey = getPrivateKey(context);
        return address != null && !address.isEmpty()
                && privateKey != null && !privateKey.isEmpty();
    }

    /** 清除所有钱包数据 */
    public static void clear(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    // ─────────────────────────────────────────────
    //  Token 合约地址列表
    // ─────────────────────────────────────────────

    /** 读取已保存的合约地址列表 */
    public static List<String> getTokenContracts(Context context) {
        List<String> list = new ArrayList<>();
        String json = getPrefs(context).getString(KEY_TOKEN_CONTRACTS+getCurrentNetwork(context), null);
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    /** 追加一个合约地址（已存在则忽略） */
    public static void addTokenContract(Context context, String contractAddress) {
        List<String> list = getTokenContracts(context);
        if (list.contains(contractAddress)) return;
        list.add(contractAddress);
        saveTokenContracts(context, list);
    }

    /** 移除一个合约地址 */
    public static void removeTokenContract(Context context, String contractAddress) {
        List<String> list = getTokenContracts(context);
        if (list.remove(contractAddress)) {
            saveTokenContracts(context, list);
        }
    }

    private static void saveTokenContracts(Context context, List<String> list) {
        JSONArray arr = new JSONArray(list);
        getPrefs(context).edit().putString(KEY_TOKEN_CONTRACTS+getCurrentNetwork(context), arr.toString()).apply();
    }

    // ─────────────────────────────────────────────
    //  网络列表 & 当前网络
    // ─────────────────────────────────────────────

    /** 内置默认网络，首次使用时返回 */
    private static List<String> defaultNetworks() {
        List<String> list = new ArrayList<>();
        list.add("wss://ethereum.publicnode.com");
        list.add("wss://sepolia.infura.io/ws/v3/1b3dddb22a464e11afd288a372fb8aef");
        return list;
    }

    /** 读取网络列表（含内置 + 用户新增） */
    public static List<String> getNetworkList(Context context) {
        String json = getPrefs(context).getString(KEY_NETWORK_LIST, null);
        if (json == null) return defaultNetworks();
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (JSONException ignored) {}
        return list.isEmpty() ? defaultNetworks() : list;
    }

    /** 追加一个自定义网络 URL */
    public static void addNetwork(Context context, String url) {
        List<String> list = getNetworkList(context);
        if (list.contains(url)) return;
        list.add(url);
        saveNetworkList(context, list);
    }

    private static void saveNetworkList(Context context, List<String> list) {
        getPrefs(context).edit()
                .putString(KEY_NETWORK_LIST, new JSONArray(list).toString())
                .apply();
    }

    /** 保存当前选中的网络 URL */
    public static void setCurrentNetwork(Context context, String url) {
        getPrefs(context).edit().putString(KEY_CURRENT_NETWORK, url).apply();
    }

    /** 读取当前选中的网络 URL，默认返回 sepolia */
    public static String getCurrentNetwork(Context context) {
        return getPrefs(context).getString(KEY_CURRENT_NETWORK, "");
    }

    // ─────────────────────────────────────────────
    //  Token 元信息缓存（name / symbol）
    //  key: "token_meta_<合约地址>"，value: {"name":"...","symbol":"..."}
    // ─────────────────────────────────────────────

    private static String tokenMetaKey(String contractAddress) {
        return "token_meta_" + contractAddress.toLowerCase();
    }

    /**
     * 缓存 Token 的 name 和 symbol。
     * 仅当 name 和 symbol 均非空时才写入，避免缓存错误数据。
     */
    public static void saveTokenMeta(Context context, String contractAddress,
                                     String name, String symbol) {
        if (contractAddress == null || contractAddress.isEmpty()) return;
        if (name == null || name.isEmpty() || symbol == null || symbol.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("symbol", symbol);
            getPrefs(context).edit().putString(tokenMetaKey(contractAddress), obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    /**
     * 读取缓存的 Token 元信息，返回 String[]{name, symbol}。
     * 若缓存不存在或解析失败，返回 null。
     */
    public static String[] getTokenMeta(Context context, String contractAddress) {
        if (contractAddress == null || contractAddress.isEmpty()) return null;
        String json = getPrefs(context).getString(tokenMetaKey(contractAddress), null);
        if (json == null) return null;
        try {
            JSONObject obj = new JSONObject(json);
            String name = obj.optString("name", "");
            String symbol = obj.optString("symbol", "");
            if (name.isEmpty() || symbol.isEmpty()) return null;
            return new String[]{name, symbol};
        } catch (JSONException ignored) {
            return null;
        }
    }
}
