package com.web3.eth.android.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.web3.eth.android.R;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * 自定义扫码界面：
 *  - 深色 UI 风格，与 App 整体主题一致
 *  - 顶部返回按钮
 *  - 底部"从相册选取"按钮，本地解码二维码后返回结果
 */
public class CustomScanActivity extends AppCompatActivity {

    /** 调用方读取扫码结果时使用的 Intent extra key */
    public static final String SCAN_RESULT_KEY = "SCAN_RESULT";

    private DecoratedBarcodeView barcodeView;

    /** 相机权限请求 Launcher */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    showPermissionDeniedDialog();
                }
            });

    /** 相册选图 Launcher */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    decodeQrFromUri(uri);
                }
            });

    /** 摄像头扫码回调 */
    private final BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() != null) {
                returnResult(result.getText());
            }
        }

        @Override
        public void possibleResultPoints(List possiblePoints) { }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scan);

        barcodeView = findViewById(R.id.barcodeView);
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(
                Collections.singletonList(com.google.zxing.BarcodeFormat.QR_CODE)));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnGallery = findViewById(R.id.btnPickGallery);
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // 检查并申请相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        barcodeView.decodeContinuous(barcodeCallback);
    }

    /** 权限被拒绝时提示用户去设置手动开启 */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this, R.style.Web3AlertDialog)
                .setTitle(R.string.scan_permission_title)
                .setMessage(R.string.scan_permission_message)
                .setPositiveButton(R.string.scan_permission_settings, (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.send_result_cancel, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    // ─────────────────────────────────────────────
    //  本地图片二维码解码
    // ─────────────────────────────────────────────

    private void decodeQrFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) {
                Toast.makeText(this, R.string.scan_error_read_image, Toast.LENGTH_SHORT).show();
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(binaryBitmap);
            returnResult(result.getText());

        } catch (NotFoundException e) {
            Toast.makeText(this, R.string.scan_error_no_qr, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.scan_error_read_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────
    //  返回扫码结果
    // ─────────────────────────────────────────────

    private void returnResult(String text) {
        Intent intent = new Intent();
        intent.putExtra(SCAN_RESULT_KEY, text);
        setResult(RESULT_OK, intent);
        finish();
    }
}
