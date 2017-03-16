/*
 * Copyright (c) 2017 knilch - freeshisen@cwde.de
 * Copyright (c) 2013 contact.proofofconcept@gmail.com
 *
 * Licensed under GNU General Public License, version 2 or later.
 * Some rights reserved. See COPYING.
 */

package de.cwde.freeshisen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

public class ShisenShoActivity extends Activity {
	private ShisenShoView view;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		view = ShisenSho.app().getView();
		ShisenSho.app().activity = this;
		setContentView(view);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (view != null) {
			view.resumeTime();
		}
	}

	@Override
	protected void onPause() {
		if (view != null) {
			view.pauseTime();
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		ViewGroup vg = (ViewGroup) (view.getParent());
		vg.removeView(view);
		ShisenSho.app().activity = null;
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.hint:
			case R.id.undo:
			case R.id.clean:
				return view.onOptionsItemSelected(item);
			case R.id.hiscore:
				startActivity(new Intent("de.cwde.freeshisen.HISCORE", null));
				return true;
			case R.id.options:
				startActivity(new Intent("de.cwde.freeshisen.SETTINGS", null));
				return true;
			case R.id.about:
				onAboutActivate();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void onAboutActivate() {
		// Try to load the a package matching the name of our own package
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			final String appname = getString(R.string.app_name);
			final String aboutTitle = "About " + appname;
			String versionString = appname + " " + pInfo.versionName;
			String aboutText = getString(R.string.aboutText);

			// Set up the TextView
			final TextView message = new TextView(this);
			// We'll use a spannablestring to be able to make links clickable
			final SpannableString s = new SpannableString(aboutText);

			// Set some padding
			message.setPadding(5, 5, 5, 5);
			// Set up the final string
			message.setText(versionString + s);
			// Now linkify the text
			Linkify.addLinks(message, Linkify.ALL);

			new AlertDialog.Builder(this)
					.setTitle(aboutTitle)
					.setCancelable(true)
					.setIcon(R.drawable.icon)
					.setPositiveButton(android.R.string.ok, null)
					.setView(message).create()
					.show();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
