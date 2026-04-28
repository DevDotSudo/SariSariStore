package com.magalona.sarisaristore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.magalona.sarisaristore.R;
import com.magalona.sarisaristore.databinding.ItemProductBinding;
import com.magalona.sarisaristore.models.Product;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductActionListener {
        void onAddStock(Product product);
        void onEditProduct(Product product);
        void onDeleteProduct(Product product);
    }

    private final List<Product> items;
    private final OnProductActionListener listener;

    public ProductAdapter(List<Product> items, OnProductActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding b = ItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductBinding b;

        ViewHolder(ItemProductBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Product p) {
            if (p == null) return;
            b.tvName.setText(p.getName() != null ? p.getName() : "");
            b.tvCategory.setText(p.getCategory() != null ? p.getCategory() : "");
            b.tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", p.getUnitPrice()));
            b.tvStock.setText("Stock: " + p.getStockQuantity());

            // UI adjustments based on stock level
            if (p.isLowStock()) {
                b.tvStock.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error));
                b.tvStock.setText("Low Stock: " + p.getStockQuantity());
            } else {
                b.tvStock.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.success));
                b.tvStock.setText("In Stock: " + p.getStockQuantity());
            }

            // Product image
            if (p.getImageUri() != null && !p.getImageUri().isEmpty()) {
                Glide.with(b.ivProduct.getContext())
                        .load(p.getImageUri())
                        .placeholder(R.drawable.ic_product_placeholder)
                        .error(R.drawable.ic_product_placeholder)
                        .into(b.ivProduct);
            } else {
                b.ivProduct.setImageResource(R.drawable.ic_product_placeholder);
            }

            b.btnAddStock.setOnClickListener(v -> listener.onAddStock(p));
            
            // Add long click for more actions (Edit/Delete)
            itemView.setOnLongClickListener(v -> {
                listener.onEditProduct(p);
                return true;
            });
        }
    }
}