package net.davidcie.gyroscopebandaid.gui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import net.davidcie.gyroscopebandaid.R;

public class SettingsTab extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    /*@Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_settings, container, false);

        Button calibrateButton = rootView.findViewById(R.id.calibrateButton);
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Please place your phone on a flat surface, calibration will start in a few seconds.", Snackbar.LENGTH_LONG)
                        .setAction("Cancel", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d("GyroBandaid", "User just clicked cancel");
                            }
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event == DISMISS_EVENT_TIMEOUT) {
                                    Log.d("GyroBandaid", "Disappeared, we can proceed!");
                                } else if (event == DISMISS_EVENT_ACTION) {
                                    Log.d("GyroBandaid", "Cancelled :-(");
                                }
                            }
                        })
                        .show();
            }
        });

        return rootView;
    }*/
}
