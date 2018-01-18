package net.davidcie.gyroscopebandaid.gui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import net.davidcie.gyroscopebandaid.R;

public class SettingsTab extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        final EditTextPreference test = (EditTextPreference) findPreference("filter_size");
        test.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                test.setSummary("aaa");
                return false;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("GyroBandaid", "Change in " + key);
    }

    @Override
    public void onPause() {
        Log.d("GyroBandaid", "Pause!");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d("GyroBandaid", "Resume!");
        super.onResume();
    }

    @Override
    public void onStart() {
        Log.d("GyroBandaid", "Start!");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d("GyroBandaid", "Stop!");
        super.onStop();
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
