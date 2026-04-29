package com.magalona.sarisaristore.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.magalona.sarisaristore.BarcodeScannerActivity;
import com.magalona.sarisaristore.R;
import com.magalona.sarisaristore.adapters.CartAdapter;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentPosBinding;
import com.magalona.sarisaristore.databinding.DialogSelectProductBinding;
import com.magalona.sarisaristore.models.CartItem;
import com.magalona.sarisaristore.models.Product;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PosFragment extends Fragment {

    private FragmentPosBinding binding;
    private CartAdapter cartAdapter;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final FirestoreHelper db = FirestoreHelper.getInstance();

    private final ActivityResultLauncher<Intent> barcodeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String barcode = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE);
                    if (barcode != null) fetchProductByBarcode(barcode);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartAdapter = new CartAdapter(cartItems, this::removeFromCart, this::updateTotal);
        binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCart.setAdapter(cartAdapter);

        binding.btnScanAdd.setOnClickListener(v ->
                barcodeLauncher.launch(new Intent(requireContext(), BarcodeScannerActivity.class)));

        binding.btnBrowseAdd.setOnClickListener(v -> showProductPickerDialog());

        binding.btnCheckout.setOnClickListener(v -> confirmCheckout());

        binding.btnClearCart.setOnClickListener(v -> {
            if (cartItems.isEmpty()) return;
            DialogHelper.showConfirmation(requireContext(),
                    "Clear Cart?",
                    "Are you sure you want to remove all items from the cart?",
                    () -> {
                        cartItems.clear();
                        cartAdapter.notifyDataSetChanged();
                        updateTotal();
                        DialogHelper.showSuccessAutoClose(requireContext(), "Success", "Cart cleared");
                    },
                    null);
        });

        updateTotal();
    }

    private void fetchProductByBarcode(String barcode) {
        db.getProductByBarcode(barcode).get()
                .addOnSuccessListener(snap -> {
                    if (binding == null || getContext() == null) return;
                    if (snap.isEmpty()) {
                        DialogHelper.showError(requireContext(), "Product Not Found", "No product found with barcode: " + barcode, null);
                        return;
                    }
                    Product p = snap.getDocuments().get(0).toObject(Product.class);
                    if (p != null) {
                        p.setId(snap.getDocuments().get(0).getId());
                        promptQuantity(p);
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null || getContext() == null) return;
                    DialogHelper.showError(requireContext(), "Error", e.getMessage(), null);
                });
    }

    private void showProductPickerDialog() {
         db.getProductsCollection().orderBy("name").get()
                 .addOnSuccessListener(snap -> {
                     if (binding == null || getContext() == null) return;
                     List<Product> products = new ArrayList<>();
                     for (DocumentSnapshot doc : snap.getDocuments()) {
                         Product p = doc.toObject(Product.class);
                         if (p != null) {
                             p.setId(doc.getId());
                             products.add(p);
                         }
                     }
                     if (products.isEmpty()) {
                         DialogHelper.showError(requireContext(), "No Products", "Your inventory is empty. Add products in the Stock tab first.", null);
                         return;
                     }

                     String[] names = new String[products.size()];
                     for (int i = 0; i < products.size(); i++) {
                         Product prod = products.get(i);
                         String prodName = prod.getName() != null ? prod.getName() : "Unknown";
                         names[i] = String.format(Locale.getDefault(), "%s (₱%.2f) - Stock: %d",
                                 prodName, prod.getUnitPrice(), prod.getStockQuantity());
                     }

                     new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                             .setTitle("Select Product")
                             .setItems(names, (d, which) -> promptQuantity(products.get(which)))
                             .setNegativeButton("Cancel", null)
                             .show();
                 })
                 .addOnFailureListener(e -> {
                     if (binding == null || getContext() == null) return;
                     DialogHelper.showError(requireContext(), "Error", e.getMessage(), null);
                 });
     }

    private void promptQuantity(Product product) {
         if (getContext() == null || product == null || binding == null) return;
         DialogSelectProductBinding dlg = DialogSelectProductBinding.inflate(LayoutInflater.from(requireContext()));
         dlg.tvProductName.setText(product.getName() != null ? product.getName() : "");
         dlg.tvStock.setText("Available Stock: " + product.getStockQuantity());

         androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                 .setView(dlg.getRoot())
                 .setCancelable(false)
                 .create();
         
         dialog.setCanceledOnTouchOutside(false);

         // Close button in header
         if (dlg.btnClose != null) {
             dlg.btnClose.setOnClickListener(v -> dialog.dismiss());
         }

         dlg.btnConfirm.setOnClickListener(v -> {
             String qtyStr = dlg.etQuantity.getText() != null ? dlg.etQuantity.getText().toString().trim() : "";
             if (TextUtils.isEmpty(qtyStr)) return;
             try {
                 int qty = Integer.parseInt(qtyStr);
                 if (qty <= 0) {
                     if (getContext() != null) {
                         DialogHelper.showError(requireContext(), "Invalid Input", "Please enter a quantity greater than 0", null);
                     }
                     return;
                 }
                 if (qty > product.getStockQuantity()) {
                     if (getContext() != null) {
                         DialogHelper.showError(requireContext(), "Insufficient Stock", "Only " + product.getStockQuantity() + " items available.", null);
                     }
                     return;
                 }
                 addToCart(product, qty);
                 dialog.dismiss();
             } catch (NumberFormatException e) {
                 if (getContext() != null) {
                     DialogHelper.showError(requireContext(), "Invalid Input", "Please enter a valid number for quantity", null);
                 }
             }
         });

         dialog.show();
     }

    private void addToCart(Product product, int qty) {
         if (product == null || product.getId() == null || getContext() == null) return;
         for (CartItem item : cartItems) {
             if (item != null && item.getProduct() != null && item.getProduct().getId() != null && item.getProduct().getId().equals(product.getId())) {
                 int newQty = item.getQuantity() + qty;
                 if (newQty > product.getStockQuantity()) {
                     DialogHelper.showError(requireContext(), "Limit Reached", "Total in cart exceeds available stock.", null);
                     return;
                 }
                 item.setQuantity(newQty);
                 cartAdapter.notifyDataSetChanged();
                 updateTotal();
                 return;
             }
         }
         cartItems.add(new CartItem(product, qty));
         cartAdapter.notifyItemInserted(cartItems.size() - 1);
         updateTotal();
     }

    private void removeFromCart(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.remove(position);
            cartAdapter.notifyItemRemoved(position);
            updateTotal();
        }
    }

    private void updateTotal() {
        if (binding == null) return;
        double total = 0;
        for (CartItem item : cartItems) total += item.getSubtotal();
        binding.tvTotalAmount.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
        
        boolean hasItems = !cartItems.isEmpty();
        binding.btnCheckout.setEnabled(hasItems);
        binding.btnCheckout.setAlpha(hasItems ? 1.0f : 0.5f);
        
        binding.tvEmptyCart.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmCheckout() {
        if (cartItems.isEmpty()) return;
        double total = 0;
        for (CartItem item : cartItems) total += item.getSubtotal();
        final double finalTotal = total;
        String totalStr = String.format(Locale.getDefault(), "₱ %.2f", total);

        DialogHelper.showConfirmation(requireContext(),
                "Confirm Sale",
                "Total Amount: " + totalStr + "\nProceed with checkout?",
                () -> executeCheckout(finalTotal),
                null);
    }

    private void executeCheckout(double total) {
        if (binding == null || getContext() == null) return;
        binding.btnCheckout.setEnabled(false);
        binding.btnCheckout.setAlpha(0.5f);
        
        List<CartItem> snapshot = new ArrayList<>(cartItems);
        
        db.checkout(snapshot, total)
                .addOnSuccessListener(v -> {
                    if (binding == null || getContext() == null) return;
                    DialogHelper.showSuccess(requireContext(), "Sale Completed", "Transaction recorded successfully.", null);
                    cartItems.clear();
                    cartAdapter.notifyDataSetChanged();
                    updateTotal();
                })
                .addOnFailureListener(e -> {
                    if (binding == null || getContext() == null) return;
                    DialogHelper.showError(requireContext(), "Checkout Failed", e.getMessage(), null);
                    binding.btnCheckout.setEnabled(true);
                    binding.btnCheckout.setAlpha(1.0f);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
