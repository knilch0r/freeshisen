package de.cwde.freeshisen;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class HighscoreActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.highscore);
		// now fill the values, argh...
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		fillTextView(sp, R.id.textViewHL1, "hiscore_HL1");
		fillTextView(sp, R.id.textViewHL2, "hiscore_HL2");
		fillTextView(sp, R.id.textViewHM1, "hiscore_HM1");
		fillTextView(sp, R.id.textViewHM2, "hiscore_HM2");
		fillTextView(sp, R.id.textViewHS1, "hiscore_HS1");
		fillTextView(sp, R.id.textViewHS2, "hiscore_HS2");
		fillTextView(sp, R.id.textViewEL1, "hiscore_EL1");
		fillTextView(sp, R.id.textViewEL2, "hiscore_EL2");
		fillTextView(sp, R.id.textViewEM1, "hiscore_EM1");
		fillTextView(sp, R.id.textViewEM2, "hiscore_EM2");
		fillTextView(sp, R.id.textViewES1, "hiscore_ES1");
		fillTextView(sp, R.id.textViewES2, "hiscore_ES2");
	}

	private void fillTextView(SharedPreferences sp, int id, String key) {
		TextView tv = (TextView) findViewById(id);
		tv.setText(sp.getString(key, "9:99:99"));
	}

	public void clearHiscore(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(R.string.clearhiscore_confirm_text);
		builder.setTitle(R.string.clearhiscore_confirm_title);

		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User clicked OK button - delete hiscores
						SharedPreferences sp = PreferenceManager
								.getDefaultSharedPreferences(
										((AlertDialog) dialog).getContext());
						SharedPreferences.Editor editor = sp.edit();
						editor.remove("hiscore_HL1");
						editor.remove("hiscore_HL2");
						editor.remove("hiscore_HM1");
						editor.remove("hiscore_HM2");
						editor.remove("hiscore_HS1");
						editor.remove("hiscore_HS2");
						editor.remove("hiscore_EL1");
						editor.remove("hiscore_EL2");
						editor.remove("hiscore_EM1");
						editor.remove("hiscore_EM2");
						editor.remove("hiscore_ES1");
						editor.remove("hiscore_ES2");
						editor.commit();
					}
				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User cancelled the dialog - nothing to do
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
