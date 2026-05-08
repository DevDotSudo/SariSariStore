package com.magalona.sarisaristore.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.magalona.sarisaristore.R;
import com.magalona.sarisaristore.databinding.ItemProductBinding;
import com.magalona.sarisaristore.models.Product;

import java.util.List;
import java.util.Locale;

public class ProductSelectionAdapter extends RecyclerView.Adapter<ProductSelectionAdapter.ViewHolder> {

    private final List<Product> items;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductSelectionAdapter(List<Product> items) {
        this.items = items;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
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
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductBinding b;

        ViewHolder(ItemProductBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Product p) {
            if (p == null) return;

            // Text fields
            b.tvName.setText(p.getName() != null ? p.getName() : "Unknown");
            b.tvCategory.setText(p.getCategory() != null ? p.getCategory() : "");
            b.tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", p.getUnitPrice()));
            
            // Stock info
            b.tvStock.setText("Stock: " + p.getStockQuantity());
            
            // Hide action buttons
            if (b.layoutActions != null) {
                b.layoutActions.setVisibility(View.GONE);
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

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565; // lighter memory footprint

                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

                        if (bitmap != null) {
                            // Set directly — avoids Glide recycling issues with pre-decoded bitmaps
                            b.ivProduct.setImageBitmap(bitmap);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid Base64
                } catch (OutOfMemoryError e) {
                    // Out of memory
                } catch (Exception e) {
                    // Unexpected error
                }
            }
            // ── End image loading ──────────────────────────────────────
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(p);
                }
            });
        }
    }
}