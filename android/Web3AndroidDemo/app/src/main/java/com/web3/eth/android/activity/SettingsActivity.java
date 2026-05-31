package com.web3.eth.android.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.web3.eth.android.R;
import com.web3.eth.android.util.WalletPrefs;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.cardViewPrivateKey).setOnClickListener(v -> showPrivateKeyDialog());
    }

    /**
     * 显示私钥弹窗：默认密文，支持明文/密文切换和一键复制。
     */
    private void showPrivateKeyDialog() {
        String privateKey = WalletPrefs.getPrivateKey(this);
        if (privateKey == null || privateKey.isEmpty()) {
            privateKey = getString(R.string.settings_private_key_empty);
        }
        final String key = privateKey.startsWith("0x")?privateKey:"0x"+privateKey;

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_private_key, null);

        TextView tvKey = dialogView.findViewById(R.id.tvPrivateKey);
        MaterialButton btnToggle = dialogView.findViewById(R.id.btnToggleVisibility);
        MaterialButton btnCopy = dialogView.findViewById(R.id.btnCopyKey);

        // 初始密文显示
        tvKey.setText(key);
        tvKey.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // 明文/密文切换
        final boolean[] isVisible = {false};
        btnToggle.setOnClickListener(v -> {
            isVisible[0] = !isVisible[0];
            if (isVisible[0]) {
                tvKey.setTransformationMethod(null);
                btnToggle.setText(R.string.btn_hide_private_key);
                btnToggle.setIconResource(R.drawable.ic_visibility);
            } else {
                tvKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggle.setText(R.string.btn_show_private_key);
                btnToggle.setIconResource(R.drawable.ic_visibility_off);
            }
        });

        // 复制到剪切板
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("private_key", key));
            Toast.makeText(this, R.string.toast_key_copied, Toast.LENGTH_SHORT).show();
        });

        new AlertDialog.Builder(this, R.style.Web3AlertDialog)
                .setTitle(R.string.settings_view_private_key)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
