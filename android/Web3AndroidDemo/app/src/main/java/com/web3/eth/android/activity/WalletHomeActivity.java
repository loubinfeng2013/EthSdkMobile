package com.web3.eth.android.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.web3.eth.android.R;
import com.web3.eth.android.adapter.TokenAdapter;
import com.web3.eth.android.app.App;
import com.web3.eth.android.databinding.ActivityWalletHomeBinding;
import com.web3.eth.android.model.TokenInfo;
import com.web3.eth.android.util.WalletPrefs;
import com.web3.eth.android.activity.NetworkSelectActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import EthClient.EthClient;
import EthClient.EthListenerCallback;


public class WalletHomeActivity extends AppCompatActivity {

    private final EthListenerCallback callback = new EthListenerCallback() {
        @Override
        public void onErc20TokenReceviced(String s) {
            Log.i("eth", "onErc20TokenReceiced:" + s);
            runOnUiThread(() -> refreshTokenBalance(s));
        }

        @Override
        public void onListenerStateChange(long listenerType, long result, String info) {
            Log.i("eth","onListenerStateChange");
            if (listenerType == 1) { //token
                Log.i("eth", "token "+info);
            }
        }
    };

    private ActivityWalletHomeBinding binding;

    private final List<TokenInfo> tokenList = new ArrayList<>();
    private TokenAdapter tokenAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** 添加 Token 对话框中的合约地址输入框（扫码回调时使用） */
    private TextInputEditText pendingContractInput;

    /** ZXing 扫码 Launcher */
    private final ActivityResultLauncher<Intent> qrLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && pendingContractInput != null) {
                            String text = result.getData()
                                    .getStringExtra(CustomScanActivity.SCAN_RESULT_KEY);
                            if (text != null) {
                                pendingContractInput.setText(text);
                            }
                        }
                    });

    /** 网络选择 Launcher */
    private final ActivityResultLauncher<Intent> networkLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String url = result.getData()
                                    .getStringExtra(NetworkSelectActivity.RESULT_NETWORK_URL);
                            if (url != null) {
                                executor.execute(()->{
                                    if (!EthClient.getNetWorkUrl().equals(url)){//判断网络有没有切换
                                        runOnUiThread(()->  updateNetworkDisplay(url));
                                        runOnUiThread(() -> tokenAdapter.clearToken());
                                        EthClient.stopAllListeners();//停止监听
                                        EthClient.uInit();//销毁链接
                                        App.sdkInitialized = EthClient.initWithUrl(url);//用新url重新初始化
                                        String address = WalletPrefs.getWalletAddress(getBaseContext());
                                        EthClient.addErc20TokenReceviceListener(address);//创建监听
                                        //数字资产也要刷新下,token列表要重新获取
                                        // ETH 余额 + Token 列表全部在子线程查询，主线程立即展示框架
                                        String ethBalance = EthClient.getBalanceAtAddress(address);
                                        runOnUiThread(() -> binding.tvEthBalance.setText(ethBalance));
                                        List<String> contracts = WalletPrefs.getTokenContracts(getApplicationContext());
                                        for (String contract : contracts) {
                                            // 优先从缓存读取 name / symbol，避免重复网络请求
                                            String[] meta = WalletPrefs.getTokenMeta(getApplicationContext(), contract);
                                            String name, symbol;
                                            if (meta != null) {
                                                name = meta[0];
                                                symbol = meta[1];
                                            } else {
                                                name = EthClient.getTokenNameAtAddress(contract);
                                                symbol = EthClient.getTokenSymbolAtAddress(contract);
                                                WalletPrefs.saveTokenMeta(getApplicationContext(), contract, name, symbol);
                                            }
                                            String balance = EthClient.getTokenBalanceAtAddress(address, contract);
                                            TokenInfo token = new TokenInfo(contract, name, symbol, balance);
                                            runOnUiThread(() -> tokenAdapter.addToken(token));
                                        }
                                    }
                                });
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setupTokenRecyclerView();
        setupCopyAddress();
        setupSendReceive();
        setupRecreateWallet();
        setupAddToken();
        setupSwipeRefresh();
        setupNetworkCard();
        loadWalletData();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
            executor.execute(() -> {
                EthClient.setEthListenerCallback(callback);
                String wallet = WalletPrefs.getWalletAddress(getApplicationContext());
                EthClient.addErc20TokenReceviceListener(wallet);
            }), 1000);
    }

    // ─────────────────────────────────────────────
    //  Token RecyclerView 初始化
    // ─────────────────────────────────────────────

    private void setupTokenRecyclerView() {
        tokenAdapter = new TokenAdapter(tokenList);
        binding.rvTokenList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTokenList.setAdapter(tokenAdapter);
        binding.rvTokenList.setNestedScrollingEnabled(false);
    }

    // ─────────────────────────────────────────────
    //  下拉刷新
    // ─────────────────────────────────────────────

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary);
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.card_surface);
        binding.swipeRefresh.setOnRefreshListener(this::refreshAssets);
    }

    /**
     * 刷新 ETH 余额 + 所有已添加 Token 的余额，全程异步，完成后收起刷新动画。
     */
    private void refreshAssets() {
        String address = WalletPrefs.getWalletAddress(getApplicationContext());
        executor.execute(() -> {
            String ethBalance = EthClient.getBalanceAtAddress(address);
            runOnUiThread(() -> binding.tvEthBalance.setText(ethBalance));

            List<TokenInfo> snapshot = tokenAdapter.getTokenList();
            for (TokenInfo token : snapshot) {
                String balance = EthClient.getTokenBalanceAtAddress(address, token.getContractAddress());
                token.setBalance(balance);
                runOnUiThread(() -> tokenAdapter.updateToken(token));
            }

            runOnUiThread(() -> binding.swipeRefresh.setRefreshing(false));
        });
    }

    /**
     * 单独刷新指定合约地址的 Token 余额。
     * 若该合约不在当前列表缓存中则忽略，全程异步执行。
     */
    private void refreshTokenBalance(String contractAddress) {
        if (contractAddress == null) return;
        TokenInfo target = tokenAdapter.findByContract(contractAddress);
        if (target == null) return;

        String walletAddress = WalletPrefs.getWalletAddress(getApplicationContext());
        executor.execute(() -> {
            String balance = EthClient.getTokenBalanceAtAddress(walletAddress, contractAddress);
            target.setBalance(balance);
            runOnUiThread(() -> tokenAdapter.updateToken(target));
        });
    }

    // ─────────────────────────────────────────────
    //  添加 Token 对话框
    // ─────────────────────────────────────────────

    private void setupAddToken() {
        binding.btnAddToken.setOnClickListener(v -> showAddTokenDialog());
    }

    private void showAddTokenDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_add_token, null);
        sheet.setContentView(sheetView);

        TextInputEditText etContract = sheetView.findViewById(R.id.etContractAddress);
        MaterialButton btnConfirm = sheetView.findViewById(R.id.btnConfirmAddToken);
        MaterialButton btnScan = sheetView.findViewById(R.id.btnScanContract);
        View layoutLoading = sheetView.findViewById(R.id.layoutLoading);

        // 扫码按钮
        btnScan.setOnClickListener(v -> {
            pendingContractInput = etContract;
            qrLauncher.launch(new Intent(this, CustomScanActivity.class));
        });

        btnConfirm.setOnClickListener(v -> {
            String contract = etContract.getText() != null
                    ? etContract.getText().toString().trim() : "";
            if (TextUtils.isEmpty(contract)) {
                Toast.makeText(this, R.string.toast_contract_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (WalletPrefs.getTokenContracts(getApplicationContext()).contains(contract)) {
                Toast.makeText(this, R.string.toast_token_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            layoutLoading.setVisibility(View.VISIBLE);
            btnConfirm.setEnabled(false);
            sheet.setCancelable(false);

            addToken(contract, sheet, layoutLoading, btnConfirm);
        });

        sheet.setOnDismissListener(d -> pendingContractInput = null);
        sheet.show();
    }

    /**
     * 在子线程校验地址、查询 Token 信息，完成后回到主线程刷新列表并关闭对话框。
     */
    private void addToken(String contractAddress,
                          BottomSheetDialog sheet,
                          View layoutLoading,
                          MaterialButton btnConfirm) {
        executor.execute(() -> {
            // 子线程：校验地址合法性
            if (!EthClient.checkErc20Address(contractAddress)) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.toast_contract_invalid, Toast.LENGTH_SHORT).show();
                    layoutLoading.setVisibility(View.GONE);
                    btnConfirm.setEnabled(true);
                    sheet.setCancelable(true);
                });
                return;
            }

            // 子线程：查询 Token 信息
            TokenInfo token = new TokenInfo(contractAddress);
            String name = EthClient.getTokenNameAtAddress(contractAddress);
            String symbol = EthClient.getTokenSymbolAtAddress(contractAddress);
            token.setName(name);
            token.setSymbol(symbol);
            token.setBalance(EthClient.getTokenBalanceAtAddress(
                    WalletPrefs.getWalletAddress(getApplicationContext()), contractAddress));
            // 写入 name / symbol 缓存，下次加载无需再请求网络
            WalletPrefs.saveTokenMeta(getApplicationContext(), contractAddress, name, symbol);
            WalletPrefs.addTokenContract(getApplicationContext(), contractAddress);

            // 主线程：更新 UI
            runOnUiThread(() -> {
                tokenAdapter.addToken(token);
                Toast.makeText(this, R.string.toast_token_added, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
                layoutLoading.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                sheet.setCancelable(true);
            });
        });
    }

    // ─────────────────────────────────────────────
    //  发送 / 接收
    // ─────────────────────────────────────────────

    private void setupSendReceive() {
        binding.btnReceive.setOnClickListener(v ->
                startActivity(new Intent(this, ReceiveActivity.class)));
        binding.btnSend.setOnClickListener(v ->
                startActivity(new Intent(this, SendActivity.class)));
    }

    // ─────────────────────────────────────────────
    //  切换钱包
    // ─────────────────────────────────────────────

    private void setupRecreateWallet() {
        binding.btnRecreateWallet.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // ─────────────────────────────────────────────
    //  网络卡片
    // ─────────────────────────────────────────────

    private void setupNetworkCard() {
        binding.cardNetwork.setOnClickListener(v ->
                networkLauncher.launch(new Intent(this, NetworkSelectActivity.class)));
    }

    /** 根据 URL 推断显示名并刷新 UI */
    private void updateNetworkDisplay(String url) {
        String name;
        if (url == null || url.isEmpty()) {
            name = getString(R.string.placeholder_network);
        } else if (url.toLowerCase().contains("sepolia.infura.io")) {
            name = getString(R.string.network_sepolia);
        } else if (url.toLowerCase().contains("ethereum.publicnode.com")) {
            name = getString(R.string.network_mainnet);
        } else {
            name = url;
        }
        binding.tvNetwork.setText(name);
        WalletPrefs.setCurrentNetwork(getBaseContext(),url);
    }

    // ─────────────────────────────────────────────
    //  复制地址
    // ─────────────────────────────────────────────

    private void setupCopyAddress() {
        binding.btnCopyAddress.setOnClickListener(v -> {
            String address = binding.tvAddress.getText().toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.label_clipboard_address), address));
            Snackbar.make(binding.getRoot(), R.string.snack_address_copied, Snackbar.LENGTH_SHORT).show();
        });
    }

    // ─────────────────────────────────────────────
    //  加载钱包数据
    // ─────────────────────────────────────────────

    private void loadWalletData() {
        updateNetworkDisplay(EthClient.getNetWorkUrl());
        String address = WalletPrefs.getWalletAddress(getBaseContext());
        binding.tvAddress.setText(address);

        // ETH 余额 + Token 列表全部在子线程查询，主线程立即展示框架
        executor.execute(() -> {
            String ethBalance = EthClient.getBalanceAtAddress(address);
            runOnUiThread(() -> binding.tvEthBalance.setText(ethBalance));

            List<String> contracts = WalletPrefs.getTokenContracts(getApplicationContext());
            for (String contract : contracts) {
                // 优先从缓存读取 name / symbol，避免重复网络请求
                String[] meta = WalletPrefs.getTokenMeta(getApplicationContext(), contract);
                String name, symbol;
                if (meta != null) {
                    name = meta[0];
                    symbol = meta[1];
                } else {
                    name = EthClient.getTokenNameAtAddress(contract);
                    symbol = EthClient.getTokenSymbolAtAddress(contract);
                    WalletPrefs.saveTokenMeta(getApplicationContext(), contract, name, symbol);
                }
                String balance = EthClient.getTokenBalanceAtAddress(address, contract);
                TokenInfo token = new TokenInfo(contract, name, symbol, balance);
                runOnUiThread(() -> tokenAdapter.addToken(token));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wallet_home, menu);
        // 将齿轮图标着色为 text_primary
        android.view.MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null && settingsItem.getIcon() != null) {
            settingsItem.getIcon().setTint(
                    getResources().getColor(R.color.text_primary, getTheme()));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            EthClient.stopAllListeners();
            finishAffinity();
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EthClient.stopAllListeners();
        executor.shutdownNow();
    }
}
