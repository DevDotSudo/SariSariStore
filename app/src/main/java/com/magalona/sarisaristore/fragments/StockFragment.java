package com.magalona.sarisaristore.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.magalona.sarisaristore.BarcodeScannerActivity;
import com.magalona.sarisaristore.R;
import com.magalona.sarisaristore.adapters.ProductAdapter;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentStockBinding;
import com.magalona.sarisaristore.databinding.DialogAddProductBinding;
import com.magalona.sarisaristore.databinding.DialogAddStockBinding;
import com.magalona.sarisaristore.models.Product;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockFragment extends Fragment {

    private FragmentStockBinding binding;
    private ProductAdapter adapter;
    private final List<Product> productList = new ArrayList<>();
    private final List<Product> filteredList = new ArrayList<>();
    private final FirestoreHelper db = FirestoreHelper.getInstance();

    private String pendingBarcode = "";
    private Uri pendingImageUri = null;
    private DialogAddProductBinding dialogBinding;

    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && cameraImageUri != null) {
                    pendingImageUri = cameraImageUri;
                    if (dialogBinding != null)
                        dialogBinding.ivProductImage.setImageURI(pendingImageUri);
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    pendingImageUri = result.getData().getData();
                    if (dialogBinding != null)
                        dialogBinding.ivProductImage.setImageURI(pendingImageUri);
                }
            });

    private final ActivityResultLauncher<Intent> barcodeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String scanned = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE);
                    if (scanned != null && dialogBinding != null) {
                        pendingBarcode = scanned;
                        dialogBinding.etBarcode.setText(scanned);
                        db.getProductByBarcode(scanned).get().addOnSuccessListener(snap -> {
                            if (dialogBinding != null && !snap.isEmpty()) {
                                DocumentSnapshot doc = snap.getDocuments().get(0);
                                Product p = doc.toObject(Product.class);
                                if (p != null) {
                                    dialogBinding.etProductName.setText(p.getName());
                                    dialogBinding.etCategory.setText(p.getCategory());
                                    dialogBinding.etUnitPrice.setText(String.valueOf(p.getUnitPrice()));
                                    DialogHelper.showSuccess(requireContext(), "Product Found", 
                                        "Existing product loaded from barcode", null);
                                }
                            }
                        });
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ProductAdapter(filteredList, new ProductAdapter.OnProductActionListener() {
            @Override
            public void onAddStock(Product product) {
                showAddStockDialog(product);
            }

            @Override
            public void onEditProduct(Product product) {
                showEditProductDialog(product);
            }

            @Override
            public void onDeleteProduct(Product product) {
                confirmDeleteProduct(product);
            }
        });
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProducts.setAdapter(adapter);

        binding.fabAddProduct.setOnClickListener(v -> showAddProductDialog());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listenProducts();
    }

    private void filter(String query) {
        if (binding == null) return;
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(productList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Product p : productList) {
                if (p.getName().toLowerCase().contains(lowerQuery) || 
                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery))) {
                    filteredList.add(p);
                }
            }
        }
        adapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void listenProducts() {
        db.getProductsCollection()
                .orderBy("name")
                .addSnapshotListener((snap, e) -> {
                    if (binding == null) return;
                    if (e != null || snap == null) return;
                    productList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            productList.add(p);
                        }
                    }
                    filter(binding.etSearch.getText().toString());
                });
    }

    private void showAddProductDialog() {
        pendingBarcode = "";
        pendingImageUri = null;
        dialogBinding = DialogAddProductBinding.inflate(getLayoutInflater());
        dialogBinding.tvDialogTitle.setText("New Product Details");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnScanBarcode.setOnClickListener(v ->
                barcodeLauncher.launch(new Intent(requireContext(), BarcodeScannerActivity.class)));

        dialogBinding.btnCamera.setOnClickListener(v -> launchCamera());
        dialogBinding.btnGallery.setOnClickListener(v -> launchGallery());

        View.OnClickListener saveAction = v -> {
            if (saveNewProduct()) {
                dialog.dismiss();
            }
        };
        
        dialogBinding.btnSaveProduct.setOnClickListener(saveAction);
        dialogBinding.btnDialogSave.setOnClickListener(saveAction);
        dialogBinding.btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditProductDialog(Product product) {
        pendingBarcode = product.getBarcode();
        pendingImageUri = (product.getImageUri() != null && !product.getImageUri().isEmpty()) ? Uri.parse(product.getImageUri()) : null;
        dialogBinding = DialogAddProductBinding.inflate(getLayoutInflater());
        dialogBinding.tvDialogTitle.setText("Edit Product");

        dialogBinding.etProductName.setText(product.getName());
        dialogBinding.etCategory.setText(product.getCategory());
        dialogBinding.etUnitPrice.setText(String.valueOf(product.getUnitPrice()));
        dialogBinding.etInitialStock.setText(String.valueOf(product.getStockQuantity()));
        dialogBinding.etBarcode.setText(product.getBarcode());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnScanBarcode.setOnClickListener(v ->
                barcodeLauncher.launch(new Intent(requireContext(), BarcodeScannerActivity.class)));

        dialogBinding.btnCamera.setOnClickListener(v -> launchCamera());
        dialogBinding.btnGallery.setOnClickListener(v -> launchGallery());

        View.OnClickListener updateAction = v -> {
            if (updateProduct(product.getId())) {
                dialog.dismiss();
            }
        };

        dialogBinding.btnSaveProduct.setText("Update Product");
        dialogBinding.btnDialogSave.setText("Update Product");
        dialogBinding.btnSaveProduct.setOnClickListener(updateAction);
        dialogBinding.btnDialogSave.setOnClickListener(updateAction);
        dialogBinding.btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean saveNewProduct() {
        if (dialogBinding == null) return false;
        String name     = dialogBinding.etProductName.getText().toString().trim();
        String category = dialogBinding.etCategory.getText().toString().trim();
        String priceStr = dialogBinding.etUnitPrice.getText().toString().trim();
        String qtyStr   = dialogBinding.etInitialStock.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            DialogHelper.showError(requireContext(), "Validation Error", "Product name and price are required", null);
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int qty = TextUtils.isEmpty(qtyStr) ? 0 : Integer.parseInt(qtyStr);
            String imageRef = pendingImageUri != null ? pendingImageUri.toString() : "";

            Product product = new Product(name, category, price, qty, pendingBarcode, imageRef);
            db.addProduct(product)
                    .addOnSuccessListener(ref -> {
                        if (getContext() != null) {
                            DialogHelper.showSuccess(requireContext(), "Success", "Product created successfully", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            DialogHelper.showError(requireContext(), "Error", 
                                "Failed to save product: " + e.getMessage(), null);
                        }
                    });
            return true;
        } catch (NumberFormatException e) {
            DialogHelper.showError(requireContext(), "Invalid Input", "Please enter valid numbers for price and stock", null);
            return false;
        }
    }

    private boolean updateProduct(String productId) {
        if (dialogBinding == null) return false;
        String name     = dialogBinding.etProductName.getText().toString().trim();
        String category = dialogBinding.etCategory.getText().toString().trim();
        String priceStr = dialogBinding.etUnitPrice.getText().toString().trim();
        String qtyStr   = dialogBinding.etInitialStock.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            DialogHelper.showError(requireContext(), "Validation Error", "Product name and price are required", null);
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int qty = TextUtils.isEmpty(qtyStr) ? 0 : Integer.parseInt(qtyStr);
            String imageRef = pendingImageUri != null ? pendingImageUri.toString() : "";

            Product product = new Product(name, category, price, qty, pendingBarcode, imageRef);
            db.updateProduct(productId, product)
                    .addOnSuccessListener(v -> {
                        if (getContext() != null) {
                            DialogHelper.showSuccess(requireContext(), "Success", "Product updated successfully", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            DialogHelper.showError(requireContext(), "Error", 
                                "Failed to update product: " + e.getMessage(), null);
                        }
                    });
            return true;
        } catch (NumberFormatException e) {
            DialogHelper.showError(requireContext(), "Invalid Input", "Please enter valid numbers for price and stock", null);
            return false;
        }
    }

    private void confirmDeleteProduct(Product product) {
        DialogHelper.showConfirmation(requireContext(), 
            "Delete Product?", 
            "Are you sure you want to delete " + product.getName() + "? This action cannot be undone.",
            () -> {
                db.deleteProduct(product.getId())
                        .addOnSuccessListener(v -> {
                            if (getContext() != null) {
                                DialogHelper.showSuccess(requireContext(), "Success", "Product deleted successfully", null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                DialogHelper.showError(requireContext(), "Error", 
                                    "Failed to delete product: " + e.getMessage(), null);
                            }
                        });
            },
            null
        );
    }

    private void showAddStockDialog(Product product) {
         if (product == null) return;
         DialogAddStockBinding dlg = DialogAddStockBinding.inflate(getLayoutInflater());
         dlg.tvProductName.setText(product.getName() != null ? product.getName() : "");
         dlg.tvCurrentStock.setText("Currently in stock: " + product.getStockQuantity());

         AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                 .setView(dlg.getRoot())
                 .create();

         dlg.btnUpdateStock.setOnClickListener(v -> {
             String qtyStr = dlg.etAddQuantity.getText().toString().trim();
             if (TextUtils.isEmpty(qtyStr)) {
                 DialogHelper.showError(requireContext(), "Invalid Input", "Please enter a quantity", null);
                 return;
             }
             try {
                 int qty = Integer.parseInt(qtyStr);
                 if (qty <= 0) {
                     DialogHelper.showError(requireContext(), "Invalid Input", "Quantity must be greater than 0", null);
                     return;
                 }
                 db.incrementStock(product.getId(), qty)
                         .addOnSuccessListener(res -> {
                             if (getContext() != null) {
                                 DialogHelper.showSuccess(requireContext(), "Success", "Inventory updated successfully", 
                                     () -> dialog.dismiss());
                             }
                         })
                         .addOnFailureListener(e -> {
                             if (getContext() != null) {
                                 DialogHelper.showError(requireContext(), "Error", 
                                     "Failed to update inventory: " + e.getMessage(), null);
                             }
                         });
             } catch (NumberFormatException e) {
                 DialogHelper.showError(requireContext(), "Invalid Input", "Please enter a valid number", null);
             }
         });

         dialog.show();
     }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            DialogHelper.showError(requireContext(), "Camera Error", "Unable to create image file", null);
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("IMG_" + stamp, ".jpg", dir);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dialogBinding = null;
    }
}