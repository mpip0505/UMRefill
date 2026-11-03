package com.umrefill.app.ui.account;

import android.os.Bundle;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;

/** OWNER: Dev D (accounts and impact). */
public class ResetPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        setTitle(R.string.title_reset);
        Placeholder.show(findViewById(android.R.id.content), "TODO Dev D\n"
                + "FR1.2: reset a forgotten password (ServiceLocator.users().resetPassword).\n"
                + "Decide how to verify the user in a local-only app (e.g. a security question) and note it in the report.");
    }
}
