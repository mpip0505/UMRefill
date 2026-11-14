package com.umrefill.app.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.User;
import com.umrefill.app.util.SessionManager;

/**
 * Account tab (Module 1: FR1.1, FR1.3).
 * Shows who is logged in and their role, with Log in and Log out buttons.
 * OWNER: Dev D (accounts and impact).
 */
public class AccountFragment extends Fragment {

    private TextView txtUserName;
    private TextView txtUserEmail;
    private TextView txtUserRole;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnTempStudent;
    private Button btnTempAdmin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        txtUserName = view.findViewById(R.id.txtUserName);
        txtUserEmail = view.findViewById(R.id.txtUserEmail);
        txtUserRole = view.findViewById(R.id.txtUserRole);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnTempStudent = view.findViewById(R.id.btnTempStudent);
        btnTempAdmin = view.findViewById(R.id.btnTempAdmin);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));
        btnLogout.setOnClickListener(v -> logout());

        // TEMP: lets other devs test logged-in and admin screens before login is built.
        btnTempStudent.setOnClickListener(v ->
                tempLogin("student@siswa.um.edu.my", "student123"));
        btnTempAdmin.setOnClickListener(v ->
                tempLogin("admin@um.edu.my", "admin123"));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        User user = SessionManager.currentUser();
        if (user != null) {
            String name = user.displayName != null && !user.displayName.trim().isEmpty()
                    ? user.displayName
                    : user.email;
            txtUserName.setText(name);
            txtUserEmail.setText(user.email);
            txtUserRole.setText(getString(R.string.account_role_format, user.role));
            txtUserRole.setVisibility(View.VISIBLE);

            btnLogin.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            txtUserName.setText(R.string.account_guest);
            txtUserEmail.setText(R.string.account_not_logged_in);
            txtUserRole.setVisibility(View.GONE);

            btnLogin.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }
    }

    private void logout() {
        SessionManager.logout();
        requireActivity().recreate(); // refreshes the Admin tab visibility
    }

    private void tempLogin(String email, String password) {
        User user = ServiceLocator.users().login(email, password);
        if (user != null) {
            SessionManager.login(user);
            requireActivity().recreate(); // refreshes the Admin tab visibility
        }
    }
}
