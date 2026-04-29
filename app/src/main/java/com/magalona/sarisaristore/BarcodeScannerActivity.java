package com.magalona.sarisaristore;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.magalona.sarisaristore.databinding.ActivityBarcodeScannerBinding;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "extra_barcode";

    private ActivityBarcodeScannerBinding binding;
    private ExecutorService cameraExecutor;
    private boolean resultSent = false;
    
    // Center validation and delay
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;
    private long barcodeDetectedTime = 0;
    private static final long SCAN_DELAY_MS = 2000; // 2 seconds delay before scanning
    private static final String TAG = "BarcodeScanner";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    DialogHelper.showError(this, "Permission Denied", 
                        "Camera permission is required to scan barcodes", () -> finish());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBarcodeScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();
                BarcodeScanner scanner = BarcodeScanning.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    @SuppressWarnings("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage == null) {
                        imageProxy.close();
                        return;
                    }

                    InputImage image = InputImage.fromMediaImage(
                            mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                    scanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                if (!barcodes.isEmpty() && !resultSent) {
                                    Barcode barcode = barcodes.get(0);
                                    
                                    // Check if barcode is in the center viewfinder
                                    if (isBarcodeInCenterViewfinder(barcode, imageProxy.getWidth(), imageProxy.getHeight())) {
                                        // Show scanning indicator
                                        runOnUiThread(() -> {
                                            binding.tvScanningIndicator.setVisibility(android.view.View.VISIBLE);
                                            binding.tvInstruction.setVisibility(android.view.View.GONE);
                                        });
                                        
                                        // If this is the first detection or barcode changed, start timer
                                        if (barcodeDetectedTime == 0) {
                                            barcodeDetectedTime = System.currentTimeMillis();
                                            
                                            // Schedule scan after delay
                                            scanRunnable = () -> {
                                                if (!resultSent) {
                                                    resultSent = true;
                                                    String value = barcode.getRawValue();
                                                    Log.d(TAG, "Barcode scanned after delay: " + value);
                                                    Intent result = new Intent();
                                                    result.putExtra(EXTRA_BARCODE, value);
                                                    setResult(RESULT_OK, result);
                                                    finish();
                                                }
                                            };
                                            mainHandler.postDelayed(scanRunnable, SCAN_DELAY_MS);
                                        }
                                        // If same barcode detected again, check if delay has passed
                                        else if (System.currentTimeMillis() - barcodeDetectedTime >= SCAN_DELAY_MS) {
                                            // Delay already passed, scan immediately
                                            if (scanRunnable != null) {
                                                mainHandler.removeCallbacks(scanRunnable);
                                            }
                                            resultSent = true;
                                            String value = barcode.getRawValue();
                                            Log.d(TAG, "Barcode scanned: " + value);
                                            Intent result = new Intent();
                                            result.putExtra(EXTRA_BARCODE, value);
                                            setResult(RESULT_OK, result);
                                            finish();
                                        }
                                    } else {
                                        // Barcode not in center, reset timer and hide indicator
                                        barcodeDetectedTime = 0;
                                        if (scanRunnable != null) {
                                            mainHandler.removeCallbacks(scanRunnable);
                                        }
                                        runOnUiThread(() -> {
                                            binding.tvScanningIndicator.setVisibility(android.view.View.GONE);
                                            binding.tvInstruction.setVisibility(android.view.View.VISIBLE);
                                        });
                                    }
                                }
                            })
                            .addOnCompleteListener(t -> imageProxy.close());
                });

                provider.unbindAll();
                provider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                DialogHelper.showError(this, "Camera Error", 
                    "An error occurred while accessing the camera: " + e.getMessage(), () -> finish());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (scanRunnable != null) {
            mainHandler.removeCallbacks(scanRunnable);
        }
    }
    
    /**
     * Check if the barcode is within the center viewfinder rectangle
     */
    private boolean isBarcodeInCenterViewfinder(Barcode barcode, int imageWidth, int imageHeight) {
        Rect boundingBox = barcode.getBoundingBox();
        if (boundingBox == null) {
            return false;
        }
        
        // Get the center point of the barcode
        int barcodeCenterX = boundingBox.centerX();
        int barcodeCenterY = boundingBox.centerY();
        
        // Viewfinder dimensions from layout (240dp x 160dp)
        // Convert to pixels (approximately, will be close enough for this use case)
        float density = getResources().getDisplayMetrics().density;
        int viewfinderWidthPx = (int) (240 * density);
        int viewfinderHeightPx = (int) (160 * density);
        
        // Calculate viewfinder center in image coordinates
        // The viewfinder is centered on screen, so we need to map it to image coordinates
        int viewfinderCenterX = imageWidth / 2;
        int viewfinderCenterY = imageHeight / 2;
        
        // Calculate viewfinder bounds in image coordinates
        int viewfinderLeft = viewfinderCenterX - (viewfinderWidthPx / 2);
        int viewfinderRight = viewfinderCenterX + (viewfinderWidthPx / 2);
        int viewfinderTop = viewfinderCenterY - (viewfinderHeightPx / 2);
        int viewfinderBottom = viewfinderCenterY + (viewfinderHeightPx / 2);
        
        // Check if barcode center is within viewfinder bounds
        boolean isInCenter = barcodeCenterX >= viewfinderLeft && 
                            barcodeCenterX <= viewfinderRight &&
                            barcodeCenterY >= viewfinderTop && 
                            barcodeCenterY <= viewfinderBottom;
        
        Log.d(TAG, String.format("Barcode center: (%d, %d), Viewfinder: (%d,%d)-(%d,%d), In center: %b",
                barcodeCenterX, barcodeCenterY, viewfinderLeft, viewfinderTop, viewfinderRight, viewfinderBottom, isInCenter));
        
        return isInCenter;
    }
}