package com.magalona.sarisaristore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.magalona.sarisaristore.databinding.ActivityLoginBinding;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    
    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if user is already authenticated
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Check if we should auto-login with saved credentials
        if (sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                // Auto login with saved credentials
                loginUser(savedEmail, savedPassword, false);
            }
        }

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etLoginEmail.getText().toString().trim();
            String password = binding.etLoginPassword.getText().toString().trim();
            
            if (validateInput(email, password)) {
                loginUser(email, password, binding.cbRememberMe.isChecked());
            }
        });

        binding.tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            DialogHelper.showError(this, "Validation Error", "Please enter your email", null);
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            DialogHelper.showError(this, "Validation Error", "Please enter a valid email address", null);
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            DialogHelper.showError(this, "Validation Error", "Please enter your password", null);
            return false;
        }
        
        return true;
    }

    private void loginUser(String email, String password, boolean rememberMe) {
        DialogHelper.showLoading(this, "Signing in...");
        
        // Sign in with Firebase Auth using email
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult == null || authResult.getUser() == null) {
                        DialogHelper.hideLoading();
                        DialogHelper.showError(this, "Login Failed", 
                                "Authentication failed", null);
                        return;
                    }
                    
                    DialogHelper.hideLoading();
                    
                    // Save credentials if remember me is checked
                    if (rememberMe) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(KEY_REMEMBER_ME, true);
                        editor.putString(KEY_EMAIL, email);
                        editor.putString(KEY_PASSWORD, password);
                        editor.apply();
                    } else {
                        // Clear saved credentials
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();
                    }
                    
                    // Navigate to main activity
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    DialogHelper.hideLoading();
                    DialogHelper.showError(this, "Login Failed", 
                            "Invalid email or password", null);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DialogHelper.hideLoading();
    }
}
