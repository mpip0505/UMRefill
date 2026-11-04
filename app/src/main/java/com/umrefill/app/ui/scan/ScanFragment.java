package com.umrefill.app.ui.scan;

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
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.ui.common.Placeholder;
import com.umrefill.app.ui.floorguide.PointDetailActivity;
import com.umrefill.app.util.Extras;

/** Scan QR tab. OWNER: Dev B (floor guide and QR). */
public class ScanFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Placeholder.show(view, "TODO Dev B\n"
                + "FR3.4: scan a station's QR code (e.g. ML Kit barcode scanning), then\n"
                + "repo.findByQrCode(text) -> PointDetailActivity. Unknown code -> error message, no crash.\n"
                + "Sample codes in FakeStore: UMR-FSKTM-G-01, UMR-ENG-G-01.");

        Placeholder.shortcut(view, 1, "Open UMR-FSKTM-G-01 (temp)", v -> {
            RefillPoint p = ServiceLocator.refillPoints().findByQrCode("UMR-FSKTM-G-01");
            if (p == null) return;
            Intent i = new Intent(requireContext(), PointDetailActivity.class);
            i.putExtra(Extras.POINT_ID, p.pointId);
            startActivity(i);
        });
    }
}
