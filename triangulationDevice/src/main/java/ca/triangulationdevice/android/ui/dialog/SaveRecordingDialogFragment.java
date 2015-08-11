package ca.triangulationdevice.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import ca.triangulationdevice.android.R;

public class SaveRecordingDialogFragment extends DialogFragment {

    private TextView counter;
    DialogListener mListener;

    private final TextWatcher counterWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            counter.setText(getString(R.string.character_count_zero, s.length()));
        }

        public void afterTextChanged(Editable s) {}
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.save_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogPositiveClick();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogNegativeClick();
            }
        });

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.save_dialog, null);
        builder.setView(view);

        counter = (TextView) view.findViewById(R.id.counter);
        TextView description = (TextView) view.findViewById(R.id.description);
        description.addTextChangedListener(counterWatcher);

        return builder.create();
    }

    public void setListener(DialogListener listener) {
        this.mListener = listener;
    }
}