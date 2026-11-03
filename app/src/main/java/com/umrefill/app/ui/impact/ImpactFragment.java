package com.umrefill.app.ui.impact;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.Placeholder;

/** Impact tab. OWNER: Dev D (accounts and impact). */
public class ImpactFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_impact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Placeholder.show(view, "TODO Dev D\n"
                + "FR5.1: my refills logged and plastic bottles avoided (impact().getRefillCount(userId)).\n"
                + "FR5.2: totals per faculty (impact().getRefillTotalsByFaculty()).\n"
                + "Guests see a 'log in to track your refills' message.");
    }
}
