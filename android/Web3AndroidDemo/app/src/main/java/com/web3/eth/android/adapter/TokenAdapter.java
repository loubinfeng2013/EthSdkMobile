package com.web3.eth.android.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.web3.eth.android.R;
import com.web3.eth.android.model.TokenInfo;

import java.util.List;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.TokenViewHolder> {

    private final List<TokenInfo> tokens;

    public TokenAdapter(List<TokenInfo> tokens) {
        this.tokens = tokens;
    }

    @NonNull
    @Override
    public TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token, parent, false);
        return new TokenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position) {
        TokenInfo token = tokens.get(position);

        // 取 symbol 首字母作为图标文字
        String symbol = token.getSymbol();
        String iconText = (symbol != null && !symbol.isEmpty())
                ? String.valueOf(symbol.charAt(0))
                : "?";
        holder.tvIcon.setText(iconText);

        holder.tvName.setText(token.getName());
        holder.tvAddress.setText(token.getShortAddress());
        holder.tvBalance.setText(token.getBalance());
        holder.tvSymbol.setText(token.getSymbol());
    }

    @Override
    public int getItemCount() {
        return tokens.size();
    }

    // ─────────────────────────────────────────────
    //  新增 / 更新 / 删除
    // ─────────────────────────────────────────────

    public void addToken(TokenInfo token) {
        tokens.add(token);
        notifyItemInserted(tokens.size() - 1);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearToken(){
        if (!tokens.isEmpty()) {
            tokens.clear();
            notifyDataSetChanged();
        }
    }

    public void updateToken(int index, TokenInfo token) {
        if (index >= 0 && index < tokens.size()) {
            tokens.set(index, token);
            notifyItemChanged(index);
        }
    }

    /** 按合约地址匹配更新（用于刷新余额等场景） */
    public void updateToken(TokenInfo token) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getContractAddress().equals(token.getContractAddress())) {
                tokens.set(i, token);
                notifyItemChanged(i);
                return;
            }
        }
    }

    /** 按合约地址查找 Token，不存在返回 null */
    public TokenInfo findByContract(String contractAddress) {
        for (TokenInfo token : tokens) {
            if (token.getContractAddress().equalsIgnoreCase(contractAddress)) {
                return token;
            }
        }
        return null;
    }

    /** 返回当前列表快照，用于刷新余额等只读遍历 */
    public List<TokenInfo> getTokenList() {
        return new java.util.ArrayList<>(tokens);
    }

    // ─────────────────────────────────────────────
    //  ViewHolder
    // ─────────────────────────────────────────────

    static class TokenViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIcon;
        final TextView tvName;
        final TextView tvAddress;
        final TextView tvBalance;
        final TextView tvSymbol;

        TokenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon    = itemView.findViewById(R.id.tvTokenIcon);
            tvName    = itemView.findViewById(R.id.tvItemTokenName);
            tvAddress = itemView.findViewById(R.id.tvItemTokenAddress);
            tvBalance = itemView.findViewById(R.id.tvItemTokenBalance);
            tvSymbol  = itemView.findViewById(R.id.tvItemTokenSymbol);
        }
    }
}
