package com.web3.eth.android.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.web3.eth.android.R;
import com.web3.eth.android.databinding.ActivityCreateWalletBinding;
import com.web3.eth.android.util.WalletPrefs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import EthClient.EthClient;

public class CreateWalletActivity extends AppCompatActivity {

    private ActivityCreateWalletBinding binding;

    /** 生成结果 */
    private String walletAddress = null;
    private String privateKey = null;

    /** 私钥是否明文显示 */
    private boolean privateKeyVisible = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.btnGenerate.setOnClickListener(v -> onGenerateClicked());
        binding.btnCopyAddress.setOnClickListener(v -> copyToClipboard(getString(R.string.label_clipboard_address), walletAddress));
        binding.btnCopyPrivateKey.setOnClickListener(v -> copyToClipboard(getString(R.string.label_clipboard_private_key), privateKey));
        binding.btnTogglePrivateKey.setOnClickListener(v -> togglePrivateKeyVisibility());
        binding.btnEnterWallet.setOnClickListener(v -> {
            WalletPrefs.setWalletAddress(this, walletAddress);
            WalletPrefs.setPrivateKey(this, privateKey);
            startActivity(new Intent(this, WalletHomeActivity.class));
            finish();
        });
    }

    // ─────────────────────────────────────────────
    //  点击生成
    // ─────────────────────────────────────────────

    private void onGenerateClicked() {
        showLoading(true);

        executor.execute(() -> {
            WalletResult result = null;
            Exception error = null;
            try {
                result = generateWallet();
            } catch (Exception e) {
                error = e;
            }

            final WalletResult finalResult = result;
            final Exception finalError = error;

            mainHandler.post(() -> {
                showLoading(false);
                if (finalResult != null) {
                    onWalletGenerated(finalResult);
                } else {
                    Snackbar.make(binding.getRoot(),
                            getString(R.string.error_generate_failed,
                                    finalError != null ? finalError.getMessage() : getString(R.string.error_generate_unknown)),
                            Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    private void onWalletGenerated(WalletResult result) {
        this.walletAddress = result.address;
        this.privateKey = result.privateKey;
        this.privateKeyVisible = false;

        binding.tvAddress.setText(result.address);
        // 默认隐藏私钥
        binding.tvPrivateKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
        binding.tvPrivateKey.setText(result.privateKey);
        binding.btnTogglePrivateKey.setText(R.string.btn_show_private_key);

        binding.layoutResult.setVisibility(View.VISIBLE);
        // 生成后禁用按钮，防止重复生成覆盖
        binding.btnGenerate.setText(R.string.btn_regenerate_wallet);
        binding.btnEnterWallet.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────
    //  复制到剪切板
    // ─────────────────────────────────────────────

    private void copyToClipboard(String label, String content) {
        if (content == null || content.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, content));
        Snackbar.make(binding.getRoot(),
                getString(R.string.copied),
                Snackbar.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────
    //  私钥显示/隐藏切换
    // ─────────────────────────────────────────────

    private void togglePrivateKeyVisibility() {
        privateKeyVisible = !privateKeyVisible;
        if (privateKeyVisible) {
            binding.tvPrivateKey.setTransformationMethod(null);
            binding.btnTogglePrivateKey.setText(R.string.btn_hide_private_key);
        } else {
            binding.tvPrivateKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.btnTogglePrivateKey.setText(R.string.btn_show_private_key);
        }
        // 刷新显示
        binding.tvPrivateKey.requestLayout();
    }

    // ─────────────────────────────────────────────
    //  Loading 状态切换
    // ─────────────────────────────────────────────

    private void showLoading(boolean loading) {
        binding.btnGenerate.setEnabled(!loading);
        binding.layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.layoutResult.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────
    //  调用 SDK 生成钱包（子线程调用）
    // ─────────────────────────────────────────────

    protected WalletResult generateWallet() throws Exception {
        String[] walletInfo = EthClient.generateWallet().split("&&&&");
        return new WalletResult(walletInfo[0], walletInfo[1]);
    }

    // ─────────────────────────────────────────────
    //  数据模型
    // ─────────────────────────────────────────────

    /**
     * 钱包生成结果
     */
    public static class WalletResult {
        /** 钱包地址，如 0xABCD... */
        public final String address;
        /** 十六进制私钥 */
        public final String privateKey;

        public WalletResult(String address, String privateKey) {
            this.address = address;
            this.privateKey = privateKey;
        }
    }

    // ─────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            showExitDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExitDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Web3AlertDialog)
                .setTitle(R.string.dialog_exit_title)
                .setMessage(R.string.dialog_exit_message)
                .setPositiveButton(R.string.dialog_exit_back, (dialog, which) -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                )
                .setNegativeButton(R.string.dialog_exit_quit, (dialog, which) -> finishAffinity())
                .setNeutralButton(R.string.dialog_exit_cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
