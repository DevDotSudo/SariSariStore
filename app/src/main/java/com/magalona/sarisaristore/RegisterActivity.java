package com.magalona.sarisaristore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.magalona.sarisaristore.databinding.ActivityRegisterBinding;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.etRegisterUsername.getText().toString().trim();
            String email = binding.etRegisterEmail.getText().toString().trim();
            String password = binding.etRegisterPassword.getText().toString().trim();
            String confirmPassword = binding.etRegisterConfirmPassword.getText().toString().trim();
            
            if (validateInput(username, email, password, confirmPassword)) {
                registerUser(username, email, password);
            }
        });

        binding.tvLoginLink.setOnClickListener(v -> {
            finish();
        });
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username)) {
            DialogHelper.showError(this, "Validation Error", "Please enter a username", null);
            return false;
        }
        
        if (TextUtils.isEmpty(email)) {
            DialogHelper.showError(this, "Validation Error", "Please enter an email", null);
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            DialogHelper.showError(this, "Validation Error", "Please enter a valid email", null);
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            DialogHelper.showError(this, "Validation Error", "Please enter a password", null);
            return false;
        }
        
        if (password.length() < 6) {
            DialogHelper.showError(this, "Validation Error", "Password must be at least 6 characters", null);
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            DialogHelper.showError(this, "Validation Error", "Passwords do not match", null);
            return false;
        }
        
        return true;
    }

    private void registerUser(String username, String email, String password) {
        DialogHelper.showLoading(this, "Creating account...");
        
        // Check if username already exists
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DialogHelper.hideLoading();
                        DialogHelper.showError(this, "Registration Failed", 
                                "Username already exists", null);
                        return;
                    }
                    
                    // Create user with Firebase Auth
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                // Save user data to Firestore
                                String userId = authResult.getUser().getUid();
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("userId", userId);
                                userData.put("username", username);
                                userData.put("email", email);
                                userData.put("createdAt", System.currentTimeMillis());
                                
                                db.collection("users")
                                        .document(userId)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            DialogHelper.hideLoading();
                                            // Show success dialog
                                            DialogHelper.showSuccess(this, "Registration Successful", 
                                                    "Your account has been created successfully!",
                                                    () -> {
                                                        // Navigate to login
                                                        finish();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            DialogHelper.hideLoading();
                                            DialogHelper.showError(this, "Error", 
                                                    "Failed to save user data: " + e.getMessage(), null);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                DialogHelper.hideLoading();
                                DialogHelper.showError(this, "Registration Failed", 
                                        "Failed to create account: " + e.getMessage(), null);
                            });
                })
                .addOnFailureListener(e -> {
                    DialogHelper.hideLoading();
                    DialogHelper.showError(this, "Error", 
                            "Failed to check username: " + e.getMessage(), null);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DialogHelper.hideLoading();
    }
}
