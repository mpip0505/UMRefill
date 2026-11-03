package com.umrefill.app.ui.common;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.umrefill.app.R;

/**
 * TEMPORARY helper for placeholder screens. Shows the TODO list and up to three shortcut buttons
 * so other devs can reach their own screens. Delete the calls when you build your real screen,
 * and delete this class once no screen uses it.
 */
public final class Placeholder {
    private Placeholder() {}

    public static void show(View root, String todo) {
        ((TextView) root.findViewById(R.id.txtTodo)).setText(todo);
    }

    /** slot = 1, 2 or 3. */
    public static void shortcut(View root, int slot, String label, View.OnClickListener action) {
        int id = slot == 1 ? R.id.btnTemp1 : slot == 2 ? R.id.btnTemp2 : R.id.btnTemp3;
        Button b = root.findViewById(id);
        b.setText(label);
        b.setVisibility(View.VISIBLE);
        b.setOnClickListener(action);
    }
}
