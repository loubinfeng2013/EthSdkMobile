package com.web3.eth.android.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.web3.eth.android.R;
import com.web3.eth.android.app.App;
import com.web3.eth.android.databinding.ActivitySplashBinding;
import com.web3.eth.android.util.WalletPrefs;

public class SplashActivity extends AppCompatActivity {

    /** 入场动画结束后延迟跳转（ms） */
    private static final long JUMP_DELAY_MS = 800;

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 隐藏系统 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        startEnterAnimation();
    }

    // ─────────────────────────────────────────────
    //  入场动画：logo 区域淡入上移，底部加载条淡入
    // ─────────────────────────────────────────────

    private void startEnterAnimation() {
        // Logo 区域：透明度 0→1 + 位移
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(binding.centerContent, View.ALPHA, 0f, 1f);
        logoAlpha.setDuration(1000);

        ObjectAnimator logoTransY = ObjectAnimator.ofFloat(binding.centerContent, View.TRANSLATION_Y, 60f, 0f);
        logoTransY.setDuration(1000);
        logoTransY.setInterpolator(AnimationUtils.loadInterpolator(this,
                android.R.interpolator.decelerate_cubic));

        // 底部加载条：延迟 400ms 淡入
        ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(binding.bottomLoading, View.ALPHA, 0f, 1f);
        loadingAlpha.setDuration(500);
        loadingAlpha.setStartDelay(500);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(logoAlpha, logoTransY, loadingAlpha);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 动画结束后检测钱包
                handler.postDelayed(SplashActivity.this::checkWalletAndNavigate, JUMP_DELAY_MS);
            }
        });
        set.start();
    }

    // ─────────────────────────────────────────────
    //  检测钱包 → 决定跳转目标
    // ─────────────────────────────────────────────

    private void checkWalletAndNavigate() {
        if(App.sdkInitialized) {
            boolean hasWallet = hasWalletStored();
            Class<?> target = hasWallet ? WalletHomeActivity.class : MainActivity.class;
            navigateTo(target);
        }else{
            //sdk可能还没初始化成功，也可能是因为网络原因没办法初始化了，2s以后再试一次
            handler.postDelayed(()->{
                if(App.sdkInitialized) {
                    boolean hasWallet = hasWalletStored();
                    Class<?> target = hasWallet ? WalletHomeActivity.class : MainActivity.class;
                    navigateTo(target);
                }else{//sdk初始化失败无法使用
                    Toast.makeText(this, R.string.splash_sdk_init_fail, Toast.LENGTH_LONG).show();
                    finishAffinity();
                }
            },5000);
        }
    }

    private boolean hasWalletStored() {
        return WalletPrefs.hasWallet(this);
    }

    // ─────────────────────────────────────────────
    //  退出动画 → 跳转
    // ─────────────────────────────────────────────

    private void navigateTo(Class<?> target) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(binding.getRoot(), View.ALPHA, 1f, 0f);
        fadeOut.setDuration(350);
        fadeOut.setInterpolator(AnimationUtils.loadInterpolator(this,
                android.R.interpolator.accelerate_quad));
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(SplashActivity.this, target);
                startActivity(intent);
                // 自定义转场动画
                overridePendingTransition(R.anim.activity_enter, android.R.anim.fade_out);
                finish();
            }
        });
        fadeOut.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
