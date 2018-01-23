package net.davidcie.gyroscopebandaid;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import static android.preference.PreferenceManager.getDefaultSharedPreferencesName;

public class SettingsActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup a non-default and world readable shared preferences, so that
        // (1) we know the name (necessary for XSharedPreferences())
        // (2) the preferences are accessible from inside the hook.
        //PreferenceManager prefMgr = getPreferenceManager();
        Log.d(Util.LOG_TAG, "getDefaultSharedPreferencesName=" + getDefaultSharedPreferencesName(this));
        //prefMgr.setSharedPreferencesName("pref_median");
        //prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }

}
