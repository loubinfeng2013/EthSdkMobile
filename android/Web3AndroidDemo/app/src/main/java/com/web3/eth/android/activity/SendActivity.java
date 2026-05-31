package com.web3.eth.android.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.web3.eth.android.R;
import com.web3.eth.android.model.TokenInfo;
import com.web3.eth.android.util.WalletPrefs;

import java.util.ArrayList;
import java.util.List;

import EthClient.EthClient;

public class SendActivity extends AppCompatActivity {

    private TextInputEditText etToAddress;
    private TextInputEditText etAmount;
    private Spinner spinnerAsset;

    private MaterialButton btnSend;
    private FrameLayout layoutLoading;

    /** 扫码 Launcher */
    private final ActivityResultLauncher<Intent> qrLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String text = result.getData()
                                    .getStringExtra(CustomScanActivity.SCAN_RESULT_KEY);
                            if (text != null) {
                                etToAddress.setText(text);
                                etToAddress.setSelection(text.length());
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etToAddress = findViewById(R.id.etToAddress);
        etAmount = findViewById(R.id.etAmount);
        spinnerAsset = findViewById(R.id.spinnerAsset);
        layoutLoading = findViewById(R.id.layoutLoading);

        setupAssetSpinner();

        // 扫码按钮
        ImageButton btnScan = findViewById(R.id.btnScanToAddress);
        btnScan.setOnClickListener(v ->
                qrLauncher.launch(new Intent(this, CustomScanActivity.class)));

        // 发送按钮
        btnSend = findViewById(R.id.btnConfirmSend);
        btnSend.setOnClickListener(v -> onSendClicked());
    }

    // ─────────────────────────────────────────────
    //  币种 Spinner：ETH + 已添加的 ERC-20 Token
    // ─────────────────────────────────────────────

    private void setupAssetSpinner() {
        List<String> assets = new ArrayList<>();
        assets.add("ETH");
        List<String> contracts = WalletPrefs.getTokenContracts(this);
        for (String contract : contracts) {
            // 优先用缓存的 symbol 展示；无缓存时退回显示短地址
            String[] meta = WalletPrefs.getTokenMeta(this, contract);
            if (meta != null && !meta[1].isEmpty()) {
                assets.add(meta[1]);  // symbol
            } else {
                TokenInfo tmp = new TokenInfo(contract);
                assets.add(tmp.getShortAddress());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, assets) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(0xFFF0F0FF);  // text_primary
                tv.setTextSize(15f);
                return tv;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(0xFFF0F0FF);  // text_primary
                tv.setBackgroundColor(0xFF1E1E32);  // card_surface
                tv.setPadding(48, 28, 48, 28);
                tv.setTextSize(15f);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAsset.setAdapter(adapter);
    }

    // ─────────────────────────────────────────────
    //  发送逻辑
    // ─────────────────────────────────────────────

    private void onSendClicked() {
        String toAddress = etToAddress.getText() != null
                ? etToAddress.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null
                ? etAmount.getText().toString().trim() : "";
        int selectedIndex = spinnerAsset.getSelectedItemPosition();

        if (toAddress.isEmpty()) {
            showError(getString(R.string.error_to_address_empty));
            return;
        }
        if (amountStr.isEmpty()) {
            showError(getString(R.string.error_amount_empty));
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError(getString(R.string.error_amount_invalid));
            return;
        }

        boolean isEth = (selectedIndex == 0);
        String privateKey = WalletPrefs.getPrivateKey(this);
        String key = privateKey.replaceFirst("^0x", "");
        String contractAddress = isEth ? null
                : WalletPrefs.getTokenContracts(this).get(selectedIndex - 1);

        // 禁用按钮，显示 Loading 覆盖层
        btnSend.setEnabled(false);
        layoutLoading.setVisibility(View.VISIBLE);

        // 子线程执行链上操作
        new Thread(() -> {
            boolean result = false;
            try {
                if (EthClient.checkEthAddress(toAddress)) {
                    if (isEth) {
                        result = EthClient.sendEth(key, toAddress, amountStr);
                    } else {
                        result = EthClient.sendErc20Token(key, toAddress, amountStr, contractAddress);
                    }
                }
            } catch (Exception e) {
                result = false;
            }
            final boolean success = result;

            // 回主线程更新 UI
            runOnUiThread(() -> {
                layoutLoading.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                if (success) {
                    showResultDialog(true, getString(R.string.send_success));
                } else {
                    showResultDialog(false, getString(R.string.send_failed));
                }
            });
        }).start();
    }

    /** 输入校验错误提示 */
    private void showError(String message) {
        new AlertDialog.Builder(this, R.style.Web3AlertDialog)
                .setMessage(message)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    /** 发送结果弹窗：成功时提供"关闭页面"选项，失败时提供"重试"选项 */
    private void showResultDialog(boolean success, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Web3AlertDialog)
                .setTitle(success ? getString(R.string.send_result_title_success)
                                  : getString(R.string.send_result_title_failed))
                .setMessage(message)
                .setCancelable(false);
        if (success) {
            builder.setPositiveButton(R.string.send_result_done, (d, w) -> finish());
        } else {
            builder.setPositiveButton(R.string.send_result_retry, (d, w) -> d.dismiss())
                   .setNegativeButton(R.string.send_result_cancel, (d, w) -> finish());
        }
        builder.show();
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
