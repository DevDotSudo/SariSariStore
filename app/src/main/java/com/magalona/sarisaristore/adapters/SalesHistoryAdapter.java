package com.magalona.sarisaristore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magalona.sarisaristore.databinding.ItemSaleBinding;
import com.magalona.sarisaristore.models.SaleRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SalesHistoryAdapter extends RecyclerView.Adapter<SalesHistoryAdapter.ViewHolder> {

    public interface OnSaleActionListener {
        void onDeleteSale(SaleRecord record);
    }

    private final List<SaleRecord> items;
    private final OnSaleActionListener listener;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    public SalesHistoryAdapter(List<SaleRecord> items, OnSaleActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSaleBinding b = ItemSaleBinding.inflate(
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
        private final ItemSaleBinding b;

        ViewHolder(ItemSaleBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(SaleRecord record) {
            if (record == null) return;
            b.tvSaleTotal.setText(
                    String.format(Locale.getDefault(), "₱%.2f", record.getTotalAmount()));
            b.tvSaleTimestamp.setText(
                    record.getTimestamp() != null ? sdf.format(record.getTimestamp()) : "—");

            // Build item summary
            if (record.getItems() != null) {
                StringBuilder sb = new StringBuilder();
                for (java.util.Map<String, Object> item : record.getItems()) {
                    Object productName = item.get("productName");
                    Object quantity = item.get("quantity");
                    sb.append("• ").append(productName != null ? productName : "Unknown")
                            .append(" x").append(quantity != null ? quantity : "0")
                            .append("\n");
                }
                b.tvSaleItems.setText(sb.toString().trim());
            } else {
                b.tvSaleItems.setText("");
            }

            if (listener != null) {
                itemView.setOnLongClickListener(v -> {
                    listener.onDeleteSale(record);
                    return true;
                });
            }
        }
    }
}