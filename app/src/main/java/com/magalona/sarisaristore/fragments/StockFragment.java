package com.magalona.sarisaristore.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
                                    // Set category in dropdown
                                    String category = p.getCategory();
                                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                                            R.array.category_options, R.layout.dropdown_item);
                                    adapter.setDropDownViewResource(R.layout.dropdown_item);
                                    dialogBinding.actvCategory.setAdapter(adapter);
                                    
                                    // Set dropdown background to white
                                    dialogBinding.actvCategory.setDropDownBackgroundResource(R.color.surface);
                                    
                                    int position = adapter.getPosition(category);
                                    if (position >= 0) {
                                        dialogBinding.actvCategory.setText(category, false);
                                    } else {
                                        // Category not in list, set as custom
                                        dialogBinding.tilCustomCategory.setVisibility(View.VISIBLE);
                                        dialogBinding.etCustomCategory.setText(category);
                                    }
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
        }, true); // true = show action buttons
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
        if (binding == null || adapter == null) return;
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(productList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Product p : productList) {
                String productName = p.getName();
                if (productName != null && productName.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(p);
                } else if (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(p);
                }
            }
        }
        adapter.notifyDataSetChanged();
        if (binding != null) {
            binding.tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
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
                    if (binding.etSearch != null && binding.etSearch.getText() != null) {
                        filter(binding.etSearch.getText().toString());
                    }
                });
    }

    private void showAddProductDialog() {
        pendingBarcode = "";
        pendingImageUri = null;
        dialogBinding = DialogAddProductBinding.inflate(getLayoutInflater());
        dialogBinding.tvDialogTitle.setText("New Product Details");
        
        // Initialize category dropdown
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.category_options, R.layout.dropdown_item);
        adapter.setDropDownViewResource(R.layout.dropdown_item);
        dialogBinding.actvCategory.setAdapter(adapter);
        
        // Set dropdown background to white
        dialogBinding.actvCategory.setDropDownBackgroundResource(R.color.surface);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();
        
        dialog.setCanceledOnTouchOutside(false);

        dialogBinding.btnScanBarcode.setOnClickListener(v ->
                barcodeLauncher.launch(new Intent(requireContext(), BarcodeScannerActivity.class)));

        dialogBinding.btnCamera.setOnClickListener(v -> launchCamera());
        dialogBinding.btnGallery.setOnClickListener(v -> launchGallery());

        // Close button in header
        dialogBinding.btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        View.OnClickListener saveAction = v -> {
            if (saveNewProduct()) {
                dialog.dismiss();
            }
        };
        
        dialogBinding.btnSaveProduct.setOnClickListener(saveAction);

        dialog.show();
    }

    private void showEditProductDialog(Product product) {
        if (product == null) return;

        pendingBarcode = product.getBarcode() != null ? product.getBarcode() : "";
        pendingImageUri = null; // ← reset, Base64 is not a URI

        dialogBinding = DialogAddProductBinding.inflate(getLayoutInflater());
        dialogBinding.tvDialogTitle.setText("Edit Product");

        dialogBinding.etProductName.setText(product.getName() != null ? product.getName() : "");
        // Set category in dropdown
        String category = product.getCategory() != null ? product.getCategory() : "";
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.category_options, R.layout.dropdown_item);
        adapter.setDropDownViewResource(R.layout.dropdown_item);
        dialogBinding.actvCategory.setAdapter(adapter);
        
        // Set dropdown background to white
        dialogBinding.actvCategory.setDropDownBackgroundResource(R.color.surface);
        
        int position = adapter.getPosition(category);
        if (position >= 0) {
            dialogBinding.actvCategory.setText(category, false);
        } else {
            // Category not in list, set as custom
            dialogBinding.tilCustomCategory.setVisibility(View.VISIBLE);
            dialogBinding.etCustomCategory.setText(category);
        }
        dialogBinding.etUnitPrice.setText(product.getUnitPrice() > 0 ? String.valueOf(product.getUnitPrice()) : "");
        dialogBinding.etInitialStock.setText(product.getStockQuantity() > 0 ? String.valueOf(product.getStockQuantity()) : "0");
        dialogBinding.etBarcode.setText(pendingBarcode);

        // ── Load existing image into the ImageView ─────────────────────
        String existingImage = product.getImageUri();
        if (existingImage != null && !existingImage.isEmpty()) {
            try {
                String base64 = existingImage.contains(",")
                        ? existingImage.substring(existingImage.indexOf(",") + 1)
                        : existingImage;

                byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    dialogBinding.ivProductImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                android.util.Log.e("StockFragment", "Failed to load existing image: " + e.getMessage());
            }
        }
        // ── End image load ─────────────────────────────────────────────

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        dialog.setCanceledOnTouchOutside(false);

        dialogBinding.btnScanBarcode.setOnClickListener(v ->
                barcodeLauncher.launch(new Intent(requireContext(), BarcodeScannerActivity.class)));

        dialogBinding.btnCamera.setOnClickListener(v -> launchCamera());
        dialogBinding.btnGallery.setOnClickListener(v -> launchGallery());

        dialogBinding.btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveProduct.setText("Update Product");
        dialogBinding.btnSaveProduct.setOnClickListener(v -> {
            if (updateProduct(product.getId(), product.getImageUri())) { // ← pass existing image
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean saveNewProduct() {
        if (dialogBinding == null) return false;
        String name     = dialogBinding.etProductName.getText().toString().trim();
        String category = getSelectedCategory();
        String priceStr = dialogBinding.etUnitPrice.getText().toString().trim();
        String qtyStr   = dialogBinding.etInitialStock.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            DialogHelper.showError(requireContext(), "Validation Error", "Product name and price are required", null);
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int qty = TextUtils.isEmpty(qtyStr) ? 0 : Integer.parseInt(qtyStr);
            
            // Convert image to Base64 if available
            String base64Image = "";
            if (pendingImageUri != null) {
                base64Image = convertUriToBase64(pendingImageUri);
                if (base64Image == null) base64Image = "";
            }

            // Log Base64 string info for debugging
            if (!base64Image.isEmpty()) {
                String preview = base64Image.length() > 50 ? base64Image.substring(0, 50) + "..." : base64Image;
                android.util.Log.d("StockFragment", "Saving Base64 image (length: " + base64Image.length() + "): " + preview);
            }

            // Get current user ID
            String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            Product product = new Product(name, category, price, qty, pendingBarcode, base64Image, userId);
            db.addProduct(product)
                    .addOnSuccessListener(ref -> {
                        if (getContext() != null && isAdded()) {
                            DialogHelper.showSuccess(requireContext(), "Success", "Product created successfully", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null && isAdded()) {
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

    private boolean updateProduct(String productId, String existingBase64Image) {
        if (dialogBinding == null) return false;
        String name     = dialogBinding.etProductName.getText().toString().trim();
        String category = getSelectedCategory();
        String priceStr = dialogBinding.etUnitPrice.getText().toString().trim();
        String qtyStr   = dialogBinding.etInitialStock.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            DialogHelper.showError(requireContext(), "Validation Error", "Product name and price are required", null);
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int qty = TextUtils.isEmpty(qtyStr) ? 0 : Integer.parseInt(qtyStr);

            String base64Image;
            if (pendingImageUri != null) {
                // User picked a new image — convert it
                String uriString = pendingImageUri.toString();
                if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    base64Image = convertUriToBase64(pendingImageUri);
                    if (base64Image == null) base64Image = existingBase64Image != null ? existingBase64Image : "";
                } else {
                    base64Image = existingBase64Image != null ? existingBase64Image : "";
                }
            } else {
                // No new image picked — keep the existing one
                base64Image = existingBase64Image != null ? existingBase64Image : "";
            }

            // Get current user ID
            String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            Product product = new Product(name, category, price, qty, pendingBarcode, base64Image, userId);
            db.updateProduct(productId, product)
                    .addOnSuccessListener(v -> {
                        if (getContext() != null && isAdded()) {
                            DialogHelper.showSuccess(requireContext(), "Success", "Product updated successfully", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null && isAdded()) {
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
        if (product == null || product.getId() == null || getContext() == null) return;
        
        DialogHelper.showConfirmation(requireContext(), 
            "Delete Product?", 
            "Are you sure you want to delete " + product.getName() + "? This action cannot be undone.",
            () -> {
                db.deleteProduct(product.getId())
                        .addOnSuccessListener(v -> {
                            if (getContext() != null && isAdded()) {
                                DialogHelper.showSuccess(requireContext(), "Success", "Product deleted successfully", null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null && isAdded()) {
                                DialogHelper.showError(requireContext(), "Error", 
                                    "Failed to delete product: " + e.getMessage(), null);
                            }
                        });
            },
            null
        );
    }

    private void showAddStockDialog(Product product) {
         if (product == null || product.getId() == null || getContext() == null) return;
         DialogAddStockBinding dlg = DialogAddStockBinding.inflate(getLayoutInflater());
         dlg.tvProductName.setText(product.getName() != null ? product.getName() : "");
         dlg.tvCurrentStock.setText("Currently in stock: " + product.getStockQuantity());

         AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SariSariStore_Dialog)
                 .setView(dlg.getRoot())
                 .setCancelable(false)
                 .create();
         
         dialog.setCanceledOnTouchOutside(false);

         // Close button in header
         dlg.btnStockClose.setOnClickListener(v -> dialog.dismiss());

         dlg.btnUpdateStock.setOnClickListener(v -> {
             String qtyStr = dlg.etAddQuantity.getText() != null ? dlg.etAddQuantity.getText().toString().trim() : "";
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
                             if (getContext() != null && isAdded()) {
                                 DialogHelper.showSuccess(requireContext(), "Success", "Inventory updated successfully", 
                                     () -> dialog.dismiss());
                             }
                         })
                         .addOnFailureListener(e -> {
                             if (getContext() != null && isAdded()) {
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

    private String getSelectedCategory() {
        String selected = dialogBinding.actvCategory.getText().toString().trim();
        if (selected.equals("Other")) {
            return dialogBinding.etCustomCategory.getText().toString().trim();
        }
        return selected;
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

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                android.util.Log.e("StockFragment", "Cannot open input stream for URI: " + uri);
                return null;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                android.util.Log.e("StockFragment", "Failed to decode bitmap from URI");
                return null;
            }
            
            // Resize to reduce size (max 512px for better Firestore performance)
            int maxDimension = 512;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
            
            if (ratio < 1) {
                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            
            // Compress to JPEG with 60% quality (smaller size for Firestore)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Log size for debugging
            int sizeKB = imageBytes.length / 1024;
            android.util.Log.d("StockFragment", "Image size after compression: " + sizeKB + " KB");
            
            // Warn if image is too large for Firestore
            if (sizeKB > 500) {
                android.util.Log.w("StockFragment", "Image is large (" + sizeKB + " KB). Consider reducing quality further.");
            }
            
            // Encode to Base64
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            
            // Recycle bitmap to free memory
            bitmap.recycle();
            
            return base64String;
        } catch (Exception e) {
            android.util.Log.e("StockFragment", "Error converting image to Base64: " + e.getMessage(), e);
            return null;
        }
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