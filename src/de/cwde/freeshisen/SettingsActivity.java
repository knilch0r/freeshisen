package de.cwde.freeshisen;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;

public class SettingsActivity extends PreferenceActivity
implements OnSharedPreferenceChangeListener {

	private ShisenSho app;

	private static final String KEY_PREF_DIFF = "pref_diff";
	private static final String KEY_PREF_SIZE = "pref_size";
	private static final String KEY_PREF_TILE = "pref_tile";
	//private static final String KEY_PREF_GRAV = "pref_grav";
	//private static final String KEY_PREF_TIME = "pref_time";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = ShisenSho.app();
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		updateSummary(sharedPreferences, KEY_PREF_DIFF, KEY_PREF_DIFF, R.array.difficulties);
		updateSummary(sharedPreferences, KEY_PREF_SIZE, KEY_PREF_SIZE, R.array.sizes);
		updateTileSummary(sharedPreferences, KEY_PREF_TILE);
	}

	@Override
	public void onBackPressed() {
		app.checkForChangedOptions();
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
		updateSummary(sharedPreferences, key, KEY_PREF_DIFF, R.array.difficulties);
		updateSummary(sharedPreferences, key, KEY_PREF_SIZE, R.array.sizes);
		updateTileSummary(sharedPreferences, key);
	}

	private void updateSummary(SharedPreferences sharedPreferences, String changedkey, String mykey, int myresource) {
		if (changedkey.equals(mykey)) {
			// FIXME: handle NumberFormatException here?
			int i = Integer.parseInt(sharedPreferences.getString(changedkey, "1"));

			Resources res = getResources();
			String[] mystrings = res.getStringArray(myresource);
			String name = mystrings[i-1];

			@SuppressWarnings("deprecation")
			Preference myPref = findPreference(changedkey);
			myPref.setSummary("Currently: " + name);
		}
	}

	private void updateTileSummary(SharedPreferences sharedPreferences, String changedkey) {
		if (changedkey.equals(KEY_PREF_TILE)) {
			String name = sharedPreferences.getString(KEY_PREF_TILE, "classic");

			@SuppressWarnings("deprecation")
			Preference myPref = findPreference(KEY_PREF_TILE);
			myPref.setSummary("Current Tileset: " + name);
		}
	}

}
