package com.umrefill.app.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;

/** OWNER: Dev D (accounts and impact). */
public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.title_login);

        View root = findViewById(android.R.id.content);
        Placeholder.show(root, "TODO Dev D\n"
                + "FR1.1: email + password login (ServiceLocator.users().login) and 'Create account'.\n"
                + "Only @um.edu.my and @siswa.um.edu.my emails may register.\n"
                + "On success: SessionManager.login(user), then finish().\n"
                + "FR1.3: admins see the Admin tab (MainActivity already checks SessionManager.isAdmin()).");
        Placeholder.shortcut(root, 1, "Forgot password (temp)",
                v -> startActivity(new Intent(this, ResetPasswordActivity.class)));
    }
}
