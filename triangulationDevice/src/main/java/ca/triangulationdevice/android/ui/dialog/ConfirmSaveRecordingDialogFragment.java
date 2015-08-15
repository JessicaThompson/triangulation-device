package ca.triangulationdevice.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ca.triangulationdevice.android.R;

public class ConfirmSaveRecordingDialogFragment extends DialogFragment {

    DialogListener mListener;
    private ViewGroup view;
    private String location;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.save_dialog_title);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogPositiveClick("", "");
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogNegativeClick();
            }
        });

        String location = getArguments().getString("location");
        String duration = getArguments().getString("duration");

        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = (ViewGroup) inflater.inflate(R.layout.confirm_save_dialog, null);
        TextView locationView = (TextView) view.findViewById(R.id.mini_location);
        locationView.setText(location);
        TextView durationView = (TextView) view.findViewById(R.id.mini_length);
        durationView.setText(duration);

        builder.setView(view);

        return builder.create();
    }

    public void setListener(DialogListener listener) {
        this.mListener = listener;
    }
}