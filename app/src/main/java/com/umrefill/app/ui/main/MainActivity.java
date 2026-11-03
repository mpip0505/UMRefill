package com.umrefill.app.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.umrefill.app.R;
import com.umrefill.app.ui.account.AccountFragment;
import com.umrefill.app.ui.admin.AdminFragment;
import com.umrefill.app.ui.browse.BrowseFragment;
import com.umrefill.app.ui.impact.ImpactFragment;
import com.umrefill.app.ui.scan.ScanFragment;
import com.umrefill.app.util.SessionManager;

/** App shell: the five bottom-navigation tabs (lateral navigation). Each tab is a Fragment. */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            showTab(R.id.nav_browse);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FR1.3: the Admin tab exists only for admin accounts.
        boolean admin = SessionManager.isAdmin();
        bottomNav.getMenu().findItem(R.id.nav_admin).setVisible(admin);
        if (!admin && bottomNav.getSelectedItemId() == R.id.nav_admin) {
            bottomNav.setSelectedItemId(R.id.nav_browse);
        }
    }

    private void showTab(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_impact) {
            fragment = new ImpactFragment();
        } else if (itemId == R.id.nav_scan) {
            fragment = new ScanFragment();
        } else if (itemId == R.id.nav_account) {
            fragment = new AccountFragment();
        } else if (itemId == R.id.nav_admin) {
            fragment = new AdminFragment();
        } else {
            fragment = new BrowseFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.tabContainer, fragment)
                .commit();
    }
}
