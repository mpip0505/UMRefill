package com.umrefill.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.Placeholder;

/** Admin tab (visible to admin accounts only). OWNER: Dev C (reporting and admin). */
public class AdminFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Placeholder.show(view, "TODO Dev C\n"
                + "Admin home: buttons to the report queue, manage points, and CSV export (FR5.3).");
        Placeholder.shortcut(view, 1, "Report queue (temp)",
                v -> startActivity(new Intent(requireContext(), ReportQueueActivity.class)));
        Placeholder.shortcut(view, 2, "Manage points (temp)",
                v -> startActivity(new Intent(requireContext(), ManagePointsActivity.class)));
    }
}
