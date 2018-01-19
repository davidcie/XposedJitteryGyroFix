package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import kotlin.Suppress;

public class SettingsTab extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tab_preferences);
        setChangeListeners();

        Context hostActivity = getActivity();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(hostActivity);
        updateAllSummaries(prefs);
    }

    private void setChangeListeners() {
        Preference.OnPreferenceChangeListener summaryUpdater = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateSummary(preference, newValue);
                return true;
            }
        };
        Preference.OnPreferenceChangeListener zeroToOneValidatorUpdater = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!Util.isValidFloat(newValue, 0.0f, 1.0f)) {
                    Log.d("GyroBandaid", "Invalid value of " + newValue.toString());
                    return false;
                }
                updateSummary(preference, newValue);
                return true;
            }
        };
        Preference.OnPreferenceChangeListener thresholdValidatorUpdater = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!Util.isValidFloat(newValue, 0.0f, Float.MAX_VALUE)) {
                    Log.d("GyroBandaid", "Invalid value of " + newValue.toString());
                    return false;
                }
                updateSummary(preference, newValue);
                return true;
            }
        };

        // These properties do not need any validation, just summary update
        ArrayList<Preference> simpleSummaryUpdate = new ArrayList<>();
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_calibration_values));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_smoothing_sample));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_rounding_decimalplaces));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_smoothing_algorithm));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_inversion_axes));
        for (Preference preference : simpleSummaryUpdate) {
            preference.setOnPreferenceChangeListener(summaryUpdater);
        }

        // Validate than update summary: alpha can only be 0.0 to 1.0
        findPreferenceById(R.string.pref_smoothing_alpha).setOnPreferenceChangeListener(zeroToOneValidatorUpdater);

        // Validate than update summary: thresholds only be 0.0 to infinity
        findPreferenceById(R.string.pref_thresholding_static).setOnPreferenceChangeListener(thresholdValidatorUpdater);
        findPreferenceById(R.string.pref_thresholding_dynamic).setOnPreferenceChangeListener(thresholdValidatorUpdater);

        // TODO: validate calibration values!
    }

    private Preference findPreferenceById(int resourceId) {
        return findPreference(getString(resourceId));
    }

    @SuppressLint("DefaultLocale")
    private void updateSummary(Preference preference, Object newValue) {
        Log.d("GyroBandaid", "Updating summary for " + preference.getKey() + ", got " + newValue.toString() + " (instance of " +
                  newValue.getClass().getName() + ")");
        String key = preference.getKey();

        ArrayList<String> simpleSummaries = new ArrayList<>();
        simpleSummaries.add(getString(R.string.pref_calibration_values));
        simpleSummaries.add(getString(R.string.pref_smoothing_sample));
        simpleSummaries.add(getString(R.string.pref_rounding_decimalplaces));

        if (simpleSummaries.contains(key)) {
            preference.setSummary(newValue.toString());
        } else if (Objects.equals(key, getString(R.string.pref_smoothing_alpha))) {
            preference.setSummary(String.format("%d%%", (int) (((Float) newValue) * 100)));
        } else if (Objects.equals(key, getString(R.string.pref_smoothing_algorithm))) {
            int entriesResId = R.array.smoothing_algorithm_entries;
            int valuesResId = R.array.smoothing_algorithm_values;
            String entry = getEntryFromValue(entriesResId, valuesResId, newValue.toString());
            preference.setSummary(entry);
        } else if (Objects.equals(key, getString(R.string.pref_inversion_axes))) {
            HashSet<String> axes = (HashSet<String>) newValue;
            boolean first = true;
            StringBuilder summaryBuilder = new StringBuilder();
            for (String axisValue : axes) {
                String entry = getEntryFromValue(R.array.inversion_axes_entries, R.array.inversion_axes_values, axisValue);
                if (entry != null) {
                    if (first) first = false;
                    else summaryBuilder.append(", ");
                    summaryBuilder.append(entry);
                }
            }
            String summary = summaryBuilder.toString();
            preference.setSummary(summary.length() > 0 ? summary : getString(R.string.inversion_summary_none));
        } else if (Objects.equals(key, getString(R.string.pref_thresholding_static))) {
            preference.setSummary(getString(R.string.thresholding_static_summary, newValue));
        } else if (Objects.equals(key, getString(R.string.pref_thresholding_dynamic))) {
            preference.setSummary(getString(R.string.thresholding_dynamic_summary, newValue));
        }
    }

    private String getEntryFromValue(int entriesResId, int valuesResId, String value) {
        String[] entries = getResources().getStringArray(entriesResId);
        String[] values = getResources().getStringArray(valuesResId);
        if (entries.length < values.length) return null;
        for (int v = 0; v < values.length; v++) {
            if (Objects.equals(values[v], value)) return entries[v];
        }
        return null;
    }

    private void updateAllSummaries(SharedPreferences prefs) {
        ArrayList<Preference> simpleSummaryUpdate = new ArrayList<>();
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_calibration_values));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_smoothing_sample));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_rounding_decimalplaces));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_smoothing_algorithm));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_inversion_axes));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_smoothing_alpha));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_thresholding_static));
        simpleSummaryUpdate.add(findPreferenceById(R.string.pref_thresholding_dynamic));
        for (Preference preference : simpleSummaryUpdate) {
            //updateSummary(preference, prefs.getString());
        }

        // Calibration
        //temp = ((SwitchPreference) findPreferenceById(R.string.pref_calibration_enabled)).
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (Objects.equals(preference.getKey(), "calibration_change")) {
            Log.d("GyroBandaid", "Requesting calibration change!");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
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
