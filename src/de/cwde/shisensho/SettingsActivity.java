package de.cwde.shisensho;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.*;

public class SettingsActivity extends PreferenceActivity
    implements OnSharedPreferenceChangeListener {
	
	private ShisenSho app;
	
	private static final String KEY_PREF_DIFF = "pref_diff";
	private static final String KEY_PREF_SIZE = "pref_size";
	//private static final String KEY_PREF_GRAV = "pref_grav";
	//private static final String KEY_PREF_TIME = "pref_time";
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ShisenSho.app();
        addPreferencesFromResource(R.xml.preferences);
    }

	@Override
	public void onBackPressed() {
		app.setOptions();
		super.onBackPressed();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}
	
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_PREF_DIFF)) {
            @SuppressWarnings("deprecation")
			Preference myPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            myPref.setSummary(sharedPreferences.getString(key, ""));
        }
        if (key.equals(KEY_PREF_SIZE)) {
            @SuppressWarnings("deprecation")
			Preference myPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            myPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }	
}
