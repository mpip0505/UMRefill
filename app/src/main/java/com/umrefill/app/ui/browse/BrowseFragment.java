package com.umrefill.app.ui.browse;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.Faculty;
import com.umrefill.app.ui.common.TwoLineAdapter;
import com.umrefill.app.util.Extras;

import java.util.ArrayList;
import java.util.List;

/** Browse tab: list of faculties (FR2.1). WORKED EXAMPLE: copy this pattern for your own lists. Dev A may restyle it. */
public class BrowseFragment extends Fragment {
    private final List<Faculty> faculties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((TextView) view.findViewById(R.id.txtHeader)).setText(R.string.choose_faculty);

        TwoLineAdapter adapter = new TwoLineAdapter(position -> {
            Intent intent = new Intent(requireContext(), FloorListActivity.class);
            intent.putExtra(Extras.FACULTY_ID, faculties.get(position).facultyId);
            startActivity(intent);
        });
        RecyclerView recycler = view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        faculties.clear();
        faculties.addAll(ServiceLocator.refillPoints().getFaculties());
        List<String> titles = new ArrayList<>();
        List<String> subtitles = new ArrayList<>();
        for (Faculty f : faculties) {
            titles.add(f.shortCode);
            subtitles.add(f.name);
        }
        adapter.submit(titles, subtitles);
    }
}
