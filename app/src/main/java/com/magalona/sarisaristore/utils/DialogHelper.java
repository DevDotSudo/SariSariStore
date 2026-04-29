package com.magalona.sarisaristore.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.magalona.sarisaristore.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Utility class for displaying custom themed dialogs throughout the app.
 * Provides methods for success, error, confirmation, and loading dialogs.
 */
public class DialogHelper {

    private static AlertDialog loadingDialog;

    public interface DialogCallback {
        void onAction();
    }

    /**
     * Display a success dialog with a message and action button.
     */
    public static AlertDialog showSuccess(Context context, String title, String message, DialogCallback callback) {
        return showCustomDialog(context, R.layout.dialog_success, title, message, callback);
    }

    /**
     * Display an error dialog with a message and dismiss button.
     */
    public static AlertDialog showError(Context context, String title, String message, DialogCallback callback) {
        return showCustomDialog(context, R.layout.dialog_error, title, message, callback);
    }

    /**
     * Display a confirmation dialog with confirm and cancel buttons.
     */
    public static AlertDialog showConfirmation(Context context, String title, String message, 
                                               DialogCallback confirmCallback, DialogCallback cancelCallback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_confirmation, null);

        TextView tvTitle = view.findViewById(R.id.tv_confirmation_title);
        TextView tvMessage = view.findViewById(R.id.tv_confirmation_message);
        Button btnConfirm = view.findViewById(R.id.btn_confirmation_action);
        Button btnCancel = view.findViewById(R.id.btn_confirmation_cancel);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_SariSariStore_Dialog)
                .setView(view)
                .setCancelable(false)
                .create();
        
        dialog.setCanceledOnTouchOutside(false);

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (confirmCallback != null) confirmCallback.onAction();
                dialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (cancelCallback != null) cancelCallback.onAction();
                dialog.dismiss();
            });
        }

        dialog.show();
        return dialog;
    }

    /**
     * Display a loading dialog with optional message.
     */
    public static AlertDialog showLoading(Context context, String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_loading, null);

        TextView tvMessage = view.findViewById(R.id.tv_loading_message);
        if (tvMessage != null && message != null) {
            tvMessage.setText(message);
        }

        loadingDialog = new MaterialAlertDialogBuilder(context, R.style.Theme_SariSariStore_Dialog)
                .setView(view)
                .setCancelable(false)
                .create();
        
        loadingDialog.setCanceledOnTouchOutside(false);

        loadingDialog.show();
        return loadingDialog;
    }

    /**
     * Hide the loading dialog if it's showing.
     */
    public static void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    /**
     * Display an error dialog with auto-dismiss after 3 seconds.
     */
    public static AlertDialog showErrorAutoClose(Context context, String title, String message) {
        AlertDialog dialog = showError(context, title, message, null);
        if (dialog != null) {
            dialog.getWindow().getDecorView().postDelayed(dialog::dismiss, 3000);
        }
        return dialog;
    }

    /**
     * Display a success dialog with auto-dismiss after 2 seconds.
     */
    public static AlertDialog showSuccessAutoClose(Context context, String title, String message) {
        AlertDialog dialog = showSuccess(context, title, message, null);
        if (dialog != null) {
            dialog.getWindow().getDecorView().postDelayed(dialog::dismiss, 2000);
        }
        return dialog;
    }

    /**
     * Generic method to display custom dialogs with view binding.
     */
    private static AlertDialog showCustomDialog(Context context, int layoutRes, String title, 
                                                 String message, DialogCallback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutRes, null);

        // For success dialog
        if (layoutRes == R.layout.dialog_success) {
            TextView tvTitle = view.findViewById(R.id.tv_success_title);
            TextView tvMessage = view.findViewById(R.id.tv_success_message);
            Button btnAction = view.findViewById(R.id.btn_success_action);

            if (tvTitle != null) tvTitle.setText(title);
            if (tvMessage != null) tvMessage.setText(message);

            AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_SariSariStore_Dialog)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            
            dialog.setCanceledOnTouchOutside(false);

            if (btnAction != null) {
                btnAction.setOnClickListener(v -> {
                    if (callback != null) callback.onAction();
                    dialog.dismiss();
                });
            }

            dialog.show();
            return dialog;
        }

        // For error dialog
        if (layoutRes == R.layout.dialog_error) {
            TextView tvTitle = view.findViewById(R.id.tv_error_title);
            TextView tvMessage = view.findViewById(R.id.tv_error_message);
            Button btnAction = view.findViewById(R.id.btn_error_action);

            if (tvTitle != null) tvTitle.setText(title);
            if (tvMessage != null) tvMessage.setText(message);

            AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_SariSariStore_Dialog)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            
            dialog.setCanceledOnTouchOutside(false);

            if (btnAction != null) {
                btnAction.setOnClickListener(v -> {
                    if (callback != null) callback.onAction();
                    dialog.dismiss();
                });
            }

            dialog.show();
            return dialog;
        }

        return null;
    }
}


