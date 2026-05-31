package com.web3.eth.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.web3.eth.android.R;

import java.util.List;

public class NetworkAdapter extends RecyclerView.Adapter<NetworkAdapter.NetworkViewHolder> {

    public interface OnNetworkClickListener {
        void onNetworkClick(String url);
    }

    private final List<String> networks;
    private String selectedUrl;
    private final OnNetworkClickListener listener;

    public NetworkAdapter(List<String> networks, String selectedUrl, OnNetworkClickListener listener) {
        this.networks = networks;
        this.selectedUrl = selectedUrl;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NetworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network, parent, false);
        return new NetworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NetworkViewHolder holder, int position) {
        String url = networks.get(position);
        boolean isSelected = url.equals(selectedUrl);

        // 根据 URL 推断显示名称
        holder.tvNetworkName.setText(resolveNetworkName(holder.itemView, url));
        holder.tvNetworkUrl.setText(url);
        holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            String prev = selectedUrl;
            selectedUrl = url;
            notifyItemChanged(networks.indexOf(prev));
            notifyItemChanged(position);
            if (listener != null) listener.onNetworkClick(url);
        });
    }

    @Override
    public int getItemCount() {
        return networks.size();
    }

    public void addNetwork(String url) {
        if (!networks.contains(url)) {
            networks.add(url);
            notifyItemInserted(networks.size() - 1);
        }
    }

    private String resolveNetworkName(View v, String url) {
        String lower = url.toLowerCase();
        if (lower.contains("sepolia.infura.io")) {
            return v.getContext().getString(R.string.network_sepolia);
        } else if (lower.contains("ethereum.publicnode.com")) {
            return v.getContext().getString(R.string.network_mainnet);
        } else {
            return v.getContext().getString(R.string.network_custom);
        }
    }

    static class NetworkViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNetworkName;
        final TextView tvNetworkUrl;
        final ImageView ivCheck;

        NetworkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNetworkName = itemView.findViewById(R.id.tvNetworkName);
            tvNetworkUrl  = itemView.findViewById(R.id.tvNetworkUrl);
            ivCheck       = itemView.findViewById(R.id.ivCheck);
        }
    }
}
