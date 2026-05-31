package com.web3.eth.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.web3.eth.android.R;
import com.web3.eth.android.adapter.NetworkAdapter;
import com.web3.eth.android.util.WalletPrefs;

import java.util.List;

/**
 * 网络选择页：
 *  - 展示内置网络（Sepolia / Mainnet）
 *  - 点击切换，当前选中网络显示勾选状态
 */
public class NetworkSelectActivity extends AppCompatActivity {

    /** 调用方通过此 extra 获取选中的网络 URL */
    public static final String RESULT_NETWORK_URL = "result_network_url";

    private NetworkAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_select);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String currentUrl = WalletPrefs.getCurrentNetwork(this);
        List<String> networks = WalletPrefs.getNetworkList(this);

        RecyclerView rv = findViewById(R.id.rvNetworks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NetworkAdapter(networks, currentUrl, this::onNetworkSelected);
        rv.setAdapter(adapter);
    }

    /**
     * 用户选择网络后的回调，后续切换网络的业务逻辑在此扩展。
     *
     * @param url 选中的网络 RPC URL
     */
    private void onNetworkSelected(String url) {
        WalletPrefs.setCurrentNetwork(this, url);
        Intent result = new Intent();
        result.putExtra(RESULT_NETWORK_URL, url);
        setResult(RESULT_OK, result);
        finish();
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
