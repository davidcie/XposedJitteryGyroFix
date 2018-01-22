package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.base.Joiner;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static android.content.Context.SENSOR_SERVICE;
import static java.lang.System.currentTimeMillis;

public class SettingsTab extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    AlertDialog mCalibrationDialog;
    View mCalibrationDialogContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tab_preferences);
        updatePreferenceSummaries();
        setValidators();
        createCalibrationDialog();
    }

    private void createCalibrationDialog() {
        // Needed to make a Snackbar
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Create a dialog to allow editing calibration values
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.calibration_dialog_message).setTitle(getString(R.string.calibration_change));

        // Add text edit fields and progress bar
        mCalibrationDialogContent = inflater.inflate(R.layout.calibration, null);
        builder.setView(mCalibrationDialogContent);
        //builder.setView(R.layout.calibration);

        // Add buttons, their actions will be defined later
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setNeutralButton(R.string.calibration_perform, null);
        mCalibrationDialog = builder.create();

        // Ok button: store values, update summary, close dialog
        final View.OnClickListener okAction = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Store new values in preferences
                persistAxis(R.id.edit_calibration_x, R.string.pref_calibration_value_x);
                persistAxis(R.id.edit_calibration_y, R.string.pref_calibration_value_y);
                persistAxis(R.id.edit_calibration_z, R.string.pref_calibration_value_z);
                // Update summary
                updatePreferenceSummary(
                        getPreferenceManager().getSharedPreferences(),
                        getString(R.string.pref_calibration_values));
                // Close dialog
                mCalibrationDialog.dismiss();
            }
        };

        // Cancel button: only close dialog
        final View.OnClickListener cancelAction = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCalibrationDialog.cancel();
            }
        };

        // Calibrate button: run calibration
        final View.OnClickListener calibrateAction = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CalibrateTask().execute();
            }
        };

        // Assign button actions
        mCalibrationDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                mCalibrationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(okAction);
                mCalibrationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(cancelAction);
                mCalibrationDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(calibrateAction);
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary(sharedPreferences, key);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // We're only interested in user clicks on the 'Change calibration values' menu entry
        if (Objects.equals(preference.getKey(), getString(R.string.pref_calibration_values))) {
            // Update dialog's EditText fields for x,y,z axes with currently stored calibration values
            updateAxisFromPreference(R.id.edit_calibration_x, R.string.pref_calibration_value_x);
            updateAxisFromPreference(R.id.edit_calibration_y, R.string.pref_calibration_value_y);
            updateAxisFromPreference(R.id.edit_calibration_z, R.string.pref_calibration_value_z);
            mCalibrationDialog.show();
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
            Float x = sharedPreferences.getFloat(getString(R.string.pref_calibration_value_x), 0.0f);
            Float y = sharedPreferences.getFloat(getString(R.string.pref_calibration_value_y), 0.0f);
            Float z = sharedPreferences.getFloat(getString(R.string.pref_calibration_value_z), 0.0f);
            String summary = getString(R.string.calibration_summary, x, y, z);
            findPreference(key).setSummary(summary);
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
            // Enable or disable changing alpha
            findPreferenceById(R.string.pref_smoothing_alpha).setEnabled(
                    Objects.equals(value, getString(R.string.pref_smoothing_algorithm_addsmooth)) ||
                    Objects.equals(value, getString(R.string.pref_smoothing_algorithm_lowpass))
            );
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

    private void updateAxisFromPreference(int editTextResId, int preferenceResId) {
        EditText editText = mCalibrationDialogContent.findViewById(editTextResId);
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        Float value = sharedPreferences.getFloat(getString(preferenceResId), 0.0f);
        editText.setText(Float.toString(value));
    }

    private void updateAxisFromValue(int editTextResId, Float value) {
        EditText editText = mCalibrationDialogContent.findViewById(editTextResId);
        editText.setText(Float.toString(value));
    }

    private void persistAxis(int editTextResId, int preferenceResId) {
        EditText editText = mCalibrationDialogContent.findViewById(editTextResId);
        Float value = Float.valueOf(editText.getText().toString());
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.edit().putFloat(getString(preferenceResId), value).apply();
    }

    private class CalibrateTask extends AsyncTask<Void, Integer, float[]> {

        private final int WANT_READINGS = 100;

        @Override
        protected void onPreExecute() {
            ProgressBar progressBar = mCalibrationDialogContent.findViewById(R.id.calibrationProgress);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(getActivity(), R.string.calibration_dialog_prepare, Toast.LENGTH_LONG).show();
        }

        @Override
        protected float[] doInBackground(Void... voids) {
            // Give user time to stop interacting with phone
            try { Thread.sleep(3500); }
            catch (InterruptedException ignored) { }

            // TODO: disable corrections
            long startTime = currentTimeMillis();

            // Time start
            final ArrayList<SensorEvent> readings = new ArrayList<>(WANT_READINGS);
            final SensorManager sensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
            // TODO: check if sensor manager is not null
            Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            SensorEventListener gyroscopeSensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    Log.d("GyroBandaid", "Received SensorEvent number " + (readings.size()));
                    readings.add(sensorEvent);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };

            // Register listener
            sensorManager.registerListener(gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);

            // Wait before we gather enough readings OR 10s passes by
            while (readings.size() < WANT_READINGS && (currentTimeMillis()-startTime) < 10*1000) {
                try { Thread.sleep(100); }
                catch (InterruptedException ignored) {}
                int progress = (int) ((readings.size() / (float) WANT_READINGS) * 100);
                publishProgress(Math.min(progress, 100));
            }

            // Done!
            Log.d("GyroBandaid", "Got enough readings, unregistering listener");
            sensorManager.unregisterListener(gyroscopeSensorListener);

            // Calculate and return average
            float[] axes = new float[3];
            axes[0] = axes[1] = axes[2] = 0.0f;
            if (readings.size() > 0) {
                for (SensorEvent reading : readings) {
                    axes[0] += reading.values[0];
                    axes[1] += reading.values[1];
                    axes[2] += reading.values[2];
                }
                axes[0] /= readings.size();
                axes[1] /= readings.size();
                axes[2] /= readings.size();
            }
            Log.d("GyroBandaid", "Returning values " + Util.printArray(axes));
            return axes;
        }

        protected void onProgressUpdate(Integer... progress) {
            ProgressBar progressBar = mCalibrationDialogContent.findViewById(R.id.calibrationProgress);
            progressBar.setProgress(progress[0]);
        }

        protected void onPostExecute(float[] result) {
            ProgressBar progressBar = mCalibrationDialogContent.findViewById(R.id.calibrationProgress);
            progressBar.setVisibility(View.INVISIBLE);

            Toast.makeText(getActivity(), R.string.calibration_dialog_done, Toast.LENGTH_LONG).show();
            updateAxisFromValue(R.id.edit_calibration_x, result[0]);
            updateAxisFromValue(R.id.edit_calibration_y, result[1]);
            updateAxisFromValue(R.id.edit_calibration_z, result[2]);
        }
    }
}
