package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;

import com.google.common.base.Joiner;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SettingsTab extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tab_preferences);
        updatePreferenceSummaries();
        setValidators();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("GyroBandaid", "Change in " + key);
        updatePreferenceSummary(sharedPreferences, key);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (Objects.equals(preference.getKey(), "calibration_change")) {
            Log.d("GyroBandaid", "Requesting calibration change!");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void updatePreferenceSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        ArrayList<String> keysToUpdate = new ArrayList<>();
        keysToUpdate.add(getString(R.string.pref_calibration_values));
        keysToUpdate.add(getString(R.string.pref_inversion_axes));
        keysToUpdate.add(getString(R.string.pref_smoothing_algorithm));
        keysToUpdate.add(getString(R.string.pref_smoothing_sample));
        keysToUpdate.add(getString(R.string.pref_smoothing_alpha));
        keysToUpdate.add(getString(R.string.pref_thresholding_static));
        keysToUpdate.add(getString(R.string.pref_thresholding_dynamic));
        keysToUpdate.add(getString(R.string.pref_rounding_decimalplaces));
        for (String key : keysToUpdate) updatePreferenceSummary(sharedPreferences, key);
    }

    private void updatePreferenceSummary(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_calibration_values))) {
            // TODO: update calibration summary
        }

        else if (key.equals(getString(R.string.pref_inversion_axes))) {
            // Translate from values to labels
            Set<String> axisValues = sharedPreferences.getStringSet(key, new HashSet<String>());
            ArrayList<String> axisEntries = new ArrayList<>(axisValues.size());
            for (String axisValue : axisValues)
                axisEntries.add(getEntryFromValue(
                        R.array.inversion_axes_entries,
                        R.array.inversion_axes_values,
                        axisValue));
            // Produce a comma-separated list and assign to summary
            String summary = Joiner.on(", ").join(axisEntries);
            findPreference(key).setSummary(summary.length() > 0
                                                   ? summary
                                                   : getString(R.string.inversion_summary_none));
        }

        else if (key.equals(getString(R.string.pref_smoothing_algorithm))) {
            String defaultValue = getString(R.string.pref_smoothing_algorithm_default);
            String value = sharedPreferences.getString(key, defaultValue);
            String entry = getEntryFromValue(
                    R.array.smoothing_algorithm_entries,
                    R.array.smoothing_algorithm_values,
                    value);
            findPreference(key).setSummary(entry);
        }

        else if (key.equals(getString(R.string.pref_smoothing_sample))) {
            int defaultValue = getResources().getInteger(R.integer.pref_smoothing_sample_default);
            int value = sharedPreferences.getInt(key, defaultValue);
            findPreference(key).setSummary(Integer.toString(value));
        }

        else if (key.equals(getString(R.string.pref_smoothing_alpha))) {
            TypedValue defaultValue = new TypedValue();
            getResources().getValue(R.string.pref_smoothing_alpha_default, defaultValue, true);
            Float value = sharedPreferences.getFloat(key, defaultValue.getFloat()) * 100;
            @SuppressLint("DefaultLocale")
            String summary = String.format("%.0f%%", value);
            findPreference(key).setSummary(summary);
        }

        else if (key.equals(getString(R.string.pref_thresholding_static))) {
            Float value = sharedPreferences.getFloat(key, 0.0f);
            String summary = getString(R.string.thresholding_static_summary, Float.toString(value));
            findPreference(key).setSummary(Html.fromHtml(summary));
        }

        else if (key.equals(getString(R.string.pref_thresholding_dynamic))) {
            Float value = sharedPreferences.getFloat(key, 0.0f);
            String summary = getString(R.string.thresholding_dynamic_summary, Float.toString(value));
            findPreference(key).setSummary(Html.fromHtml(summary));
        }

        else if (key.equals(getString(R.string.pref_rounding_decimalplaces))) {
            int value = sharedPreferences.getInt(key, 0);
            findPreference(key).setSummary(Integer.toString(value));
        }
    }

    private void setValidators() {
        // TODO: validate calibration values!

        // Alpha can only be 0.0 to 1.0
        findPreferenceById(R.string.pref_smoothing_alpha).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // TODO: show a notification
                return Util.isValidFloat(newValue, 0.0f, 1.0f);
            }
        });

        // Static threshold can only be 0.0 to infinity
        findPreferenceById(R.string.pref_thresholding_static).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return Util.isValidFloat(newValue, 0.0f, Float.MAX_VALUE);
            }
        });

        // Dynamic threshold can only be 0.0 to infinity
        findPreferenceById(R.string.pref_thresholding_dynamic).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return Util.isValidFloat(newValue, 0.0f, Float.MAX_VALUE);
            }
        });
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

    private Preference findPreferenceById(int resourceId) {
        return findPreference(getString(resourceId));
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

R.string.pref_calibration_values

R.string.pref_inversion_axes

R.string.pref_smoothing_algorithm
R.string.pref_smoothing_sample
R.string.pref_smoothing_alpha

R.string.pref_thresholding_static
R.string.pref_thresholding_dynamic

R.string.pref_rounding_decimalplaces
    }*/
}
