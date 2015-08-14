package ca.triangulationdevice.android.ui.dialog;

import android.app.DialogFragment;

public interface DialogListener {
    public void onDialogPositiveClick(String title, String description);
    public void onDialogNegativeClick();
}