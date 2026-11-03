package com.umrefill.app.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.User;
import com.umrefill.app.ui.common.Placeholder;
import com.umrefill.app.util.SessionManager;

/** Account tab. OWNER: Dev D (accounts and impact). */
public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        User u = SessionManager.currentUser();
        Placeholder.show(view, "TODO Dev D\n"
                + "Show who is logged in, with Log in / Log out buttons.\n"
                + "Currently: " + (u == null ? "guest" : u.email + " (" + u.role + ")"));

        Placeholder.shortcut(view, 1, "Open login screen (temp)",
                v -> startActivity(new Intent(requireContext(), LoginActivity.class)));
        // TEMP: lets other devs test logged-in and admin screens before login is built.
        Placeholder.shortcut(view, 2, "Log in as demo student (temp)",
                v -> tempLogin("student@siswa.um.edu.my", "student123"));
        Placeholder.shortcut(view, 3, "Log in as demo admin (temp)",
                v -> tempLogin("admin@um.edu.my", "admin123"));
    }

    private void tempLogin(String email, String password) {
        User user = ServiceLocator.users().login(email, password);
        if (user != null) {
            SessionManager.login(user);
            requireActivity().recreate(); // refreshes the Admin tab visibility
        }
    }
}
