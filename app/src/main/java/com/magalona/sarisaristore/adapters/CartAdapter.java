package com.magalona.sarisaristore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magalona.sarisaristore.databinding.ItemCartBinding;
import com.magalona.sarisaristore.models.CartItem;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface OnRemoveClick  { void onClick(int position); }
    public interface OnTotalChanged { void onChange(); }

    private final List<CartItem> items;
    private final OnRemoveClick  removeListener;
    private final OnTotalChanged totalListener;

    public CartAdapter(List<CartItem> items, OnRemoveClick removeListener, OnTotalChanged totalListener) {
        this.items = items;
        this.removeListener = removeListener;
        this.totalListener  = totalListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding b = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartBinding b;

        ViewHolder(ItemCartBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(CartItem item, int position) {
            if (item == null || item.getProduct() == null) return;
            b.tvCartName.setText(item.getProduct().getName() != null ? item.getProduct().getName() : "");
            b.tvCartQty.setText("Qty: " + item.getQuantity());
            b.tvCartSubtotal.setText(
                    String.format(Locale.getDefault(), "₱%.2f", item.getSubtotal()));

            b.btnRemove.setOnClickListener(v -> removeListener.onClick(position));
        }
    }
}