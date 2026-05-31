package com.web3.eth.android.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.web3.eth.android.util.WalletPrefs;

import EthClient.EthClient;

public class App extends Application {

    private static final String TAG = "App";

    private int foregroundCount = 0;
    private boolean isInitialized = false;

    public static boolean sdkInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App onCreate");
        // 仅主进程初始化（多进程防护）
        if (!isMainProcess()) return;

        // 注册前后台监听
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                foregroundCount++;
                if (!isInitialized) {
                    isInitialized = true;
                    initGlobalResource(); // 应用切到前台，初始化
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                foregroundCount--;
                if (foregroundCount == 0 && isInitialized) {
                    isInitialized = false;
                    deinitGlobalResource(); // 应用退到后台，反初始化
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
            // 其他空实现省略
        });
    }

    // 全局初始化（长连接、WebSocket、数据库等）
    private void initGlobalResource() {
        // 连接网络、启动线程、注册监听
        initEthSdk();
    }

    // 全局反初始化（释放资源）
    private void deinitGlobalResource() {
        // 关闭连接、停止线程、注销监听
        uninitEthSdk();
    }

    private void initEthSdk() {
        new Thread(() -> {
            String url = WalletPrefs.getCurrentNetwork(this);
            boolean result;
            if (TextUtils.isEmpty(url)){
                result = EthClient.initWithType(1);
            }else{
                result = EthClient.initWithUrl(url);
            }
            sdkInitialized = result;
            Log.d(TAG, "EthClient init result: " + result);
        }, "eth-init").start();
    }

    private void uninitEthSdk() {
        if (!sdkInitialized) return;
        sdkInitialized = false;
        EthClient.uInit();
        Log.d(TAG, "EthClient.uInit() called");
    }

    // 判断是否为主进程
    private boolean isMainProcess() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == android.os.Process.myPid()) {
                return info.processName.equals(getPackageName());
            }
        }
        return false;
    }
}
