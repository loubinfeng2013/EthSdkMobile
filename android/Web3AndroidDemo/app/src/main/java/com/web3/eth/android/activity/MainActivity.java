package com.web3.eth.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.web3.eth.android.R;
import com.web3.eth.android.databinding.ActivityMainBinding;
import com.web3.eth.android.util.WalletPrefs;

import EthClient.EthClient;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    /** 当前显示中的导入钱包 Dialog（用于扫码回调后回填） */
    private TextInputEditText pendingPrivateKeyInput;

    /** 自定义扫码 Launcher */
    private final ActivityResultLauncher<Intent> qrLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && pendingPrivateKeyInput != null) {
                            String text = result.getData()
                                    .getStringExtra(CustomScanActivity.SCAN_RESULT_KEY);
                            if (text != null) {
                                pendingPrivateKeyInput.setText(text);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cardCreateWallet.setOnClickListener(v -> {
                    startActivity(new Intent(this, CreateWalletActivity.class));
                    finish();
                }
        );

        binding.cardImportWallet.setOnClickListener(v -> showImportWalletSheet());
    }

    // ─────────────────────────────────────────────
    //  导入钱包 BottomSheet
    // ─────────────────────────────────────────────

    private void showImportWalletSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_import_wallet, null);
        dialog.setContentView(sheetView);

        TextInputEditText etPrivateKey = sheetView.findViewById(R.id.etPrivateKey);
        MaterialButton btnEnter = sheetView.findViewById(R.id.btnEnterWallet);
        MaterialButton btnScan = sheetView.findViewById(R.id.btnScanPrivateKey);

        btnScan.setOnClickListener(v -> {
            pendingPrivateKeyInput = etPrivateKey;
            qrLauncher.launch(new Intent(this, CustomScanActivity.class));
        });

        btnEnter.setOnClickListener(v -> {
            String privateKey = etPrivateKey.getText() != null
                    ? etPrivateKey.getText().toString().trim() : "";

            if (privateKey.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.error_private_key_empty, Snackbar.LENGTH_SHORT).show();
                return;
            }

            importWalletByPrivateKey(privateKey, dialog);
        });

        dialog.setOnDismissListener(d -> pendingPrivateKeyInput = null);
        dialog.show();
    }

    // ─────────────────────────────────────────────
    //  通过私钥导入钱包
    // ─────────────────────────────────────────────

    protected void importWalletByPrivateKey(String privateKey, BottomSheetDialog dialog) {
        if (!EthClient.isValidEthPrivateKey(privateKey)){
            Snackbar.make(binding.getRoot(), R.string.error_private_key_invalid, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String key = privateKey.replaceFirst("^0x", "");
        String walletAddress = EthClient.getAddressByPrivateKey(key);
        WalletPrefs.setWalletAddress(this, walletAddress);
        WalletPrefs.setPrivateKey(this, privateKey);
        dialog.dismiss();
        startActivity(new Intent(this, WalletHomeActivity.class));
        finish();
    }
}
