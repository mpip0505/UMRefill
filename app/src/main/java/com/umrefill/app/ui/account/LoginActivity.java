package com.umrefill.app.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.User;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.util.SessionManager;

import java.util.Locale;

/**
 * Login screen (Module 1: FR1.1, FR1.3).
 * Accepts only @um.edu.my and @siswa.um.edu.my emails.
 * OWNER: Dev D (accounts and impact).
 */
public class LoginActivity extends BaseActivity {

    private TextInputLayout layoutEmail;
    private TextInputEditText editEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText editPassword;
    private TextView txtError;
    private Button btnLogin;
    private Button btnForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.title_login);

        layoutEmail = findViewById(R.id.layoutEmail);
        editEmail = findViewById(R.id.editEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        editPassword = findViewById(R.id.editPassword);
        txtError = findViewById(R.id.txtError);
        btnLogin = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutEmail.setError(null);
                layoutPassword.setError(null);
                txtError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editEmail.addTextChangedListener(clearErrorWatcher);
        editPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void attemptLogin() {
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString() : "";

        boolean hasError = false;

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.error_email_required));
            hasError = true;
        } else if (!isValidUmEmail(email)) {
            layoutEmail.setError(getString(R.string.error_invalid_um_email));
            hasError = true;
        } else {
            layoutEmail.setError(null);
        }

        if (password.isEmpty()) {
            layoutPassword.setError(getString(R.string.error_password_required));
            hasError = true;
        } else {
            layoutPassword.setError(null);
        }

        if (hasError) {
            return;
        }

        User user = ServiceLocator.users().login(email, password);
        if (user == null) {
            txtError.setText(R.string.error_login_failed);
            txtError.setVisibility(View.VISIBLE);
            layoutPassword.setError(getString(R.string.error_login_failed));
            return;
        }

        SessionManager.login(user);
        finish();
    }

    /**
     * Validates that an email has a UM domain (@um.edu.my or @siswa.um.edu.my).
     */
    public static boolean isValidUmEmail(String email) {
        if (email == null) {
            return false;
        }
        String clean = email.trim().toLowerCase(Locale.ROOT);
        if (clean.contains(" ")) {
            return false;
        }
        int at = clean.indexOf('@');
        if (at <= 0 || at != clean.lastIndexOf('@')) {
            return false;
        }
        String domain = clean.substring(at);
        return "@um.edu.my".equals(domain) || "@siswa.um.edu.my".equals(domain);
    }
}
