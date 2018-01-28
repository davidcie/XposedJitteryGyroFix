package net.davidcie.gyroscopebandaid;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

@SuppressLint("Registered")
public class Preferences extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }

}
