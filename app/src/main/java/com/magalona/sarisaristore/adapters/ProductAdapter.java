package com.magalona.sarisaristore.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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
    private final boolean showActions;

    public ProductAdapter(List<Product> items, OnProductActionListener listener) {
        this.items = items;
        this.listener = listener;
        this.showActions = false;
    }

    public ProductAdapter(List<Product> items, OnProductActionListener listener, boolean showActions) {
        this.items = items;
        this.listener = listener;
        this.showActions = showActions;
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

            // Text fields
            b.tvName.setText(p.getName() != null ? p.getName() : "");
            b.tvCategory.setText(p.getCategory() != null ? p.getCategory() : "");
            b.tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", p.getUnitPrice()));

            // Stock label with color
            if (p.isLowStock()) {
                b.tvStock.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error));
                b.tvStock.setText("Low Stock: " + p.getStockQuantity());
            } else {
                b.tvStock.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.success));
                b.tvStock.setText("In Stock: " + p.getStockQuantity());
            }

            // ── Image loading ──────────────────────────────────────────
            // Always reset first to avoid stale images on recycled rows
            b.ivProduct.setImageResource(R.drawable.ic_product_placeholder);

            String imageUri = p.getImageUri();
            if (imageUri != null && !imageUri.isEmpty()) {
                try {
                    // Strip data URI prefix if present: "data:image/jpeg;base64,..."
                    if (imageUri.contains(",")) {
                        imageUri = imageUri.substring(imageUri.indexOf(",") + 1);
                    }

                    if (!imageUri.isEmpty()) {
                        byte[] imageBytes = Base64.decode(imageUri, Base64.NO_WRAP);
                        android.util.Log.d("ProductAdapter", "Decoded " + imageBytes.length + " bytes");

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565; // lighter memory footprint

                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

                        if (bitmap != null) {
                            // Set directly — avoids Glide recycling issues with pre-decoded bitmaps
                            b.ivProduct.setImageBitmap(bitmap);
                            android.util.Log.d("ProductAdapter", "Successfully loaded image for: " + p.getName());
                        } else {
                            android.util.Log.e("ProductAdapter", "Bitmap decode returned null for: " + p.getName());
                        }
                    }
                } catch (IllegalArgumentException e) {
                    android.util.Log.e("ProductAdapter", "Invalid Base64 for " + p.getName() + ": " + e.getMessage());
                } catch (OutOfMemoryError e) {
                    android.util.Log.e("ProductAdapter", "Out of memory loading image for: " + p.getName());
                } catch (Exception e) {
                    android.util.Log.e("ProductAdapter", "Unexpected error loading image for " + p.getName() + ": " + e.getMessage(), e);
                }
            }
            // ── End image loading ──────────────────────────────────────

            // Show/hide action buttons
            if (b.layoutActions != null) {
                b.layoutActions.setVisibility(showActions ? View.VISIBLE : View.GONE);
            }

            // Action buttons
            if (showActions && listener != null) {
                if (b.btnAddStock != null) {
                    b.btnAddStock.setOnClickListener(v -> listener.onAddStock(p));
                }
                if (b.btnDelete != null) {
                    b.btnDelete.setOnClickListener(v -> listener.onDeleteProduct(p));
                }
            }

            // Long press to edit
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onEditProduct(p);
                }
                return true;
            });
        }
    }
}