package org.proofofconcept.shisensho;

import java.io.Serializable;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class ShisenShoOptionsActivity extends Activity {

	Bundle state;
	ShisenSho app;

	private void appToState (boolean merge) {
		String[] fields = { "size", "difficulty", "gravity", "timeCounter" };
		Bundle options = app.getOptions();
		if (state == null) state = new Bundle();
		for (int i=0; i<fields.length; i++) {
			if (!merge || !state.containsKey(fields[i])) {
				state.putSerializable(fields[i], (Serializable)(options.get(fields[i])));
			}
		}
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.options);

        app = ShisenSho.app();
        state = savedInstanceState;
        appToState(true);

        Spinner s;
        ToggleButton tb;
        ArrayAdapter adapter;

        s = (Spinner) findViewById(R.id.size);
        adapter = ArrayAdapter.createFromResource(
                this, R.array.sizes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setSelection(state.getInt("size")-1);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				state.putInt("size", pos+1);
			}

			public void onNothingSelected(AdapterView<?> arg0) { }
        });

        s = (Spinner) findViewById(R.id.difficulty);
        adapter = ArrayAdapter.createFromResource(
                this, R.array.difficulties, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setSelection(2-state.getInt("difficulty"));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				state.putInt("difficulty", 2-pos);
			}

			public void onNothingSelected(AdapterView<?> arg0) { }
        });

        tb = (ToggleButton) findViewById(R.id.gravity);
        tb.setChecked(state.getBoolean("gravity"));
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				state.putBoolean("gravity", arg1);
			}
        });

        tb = (ToggleButton) findViewById(R.id.timeCounter);
        tb.setChecked(state.getBoolean("timeCounter"));
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				state.putBoolean("timeCounter", arg1);
			}
        });
    }

	@Override
	public void onBackPressed() {
		app.setOptions(state);
		super.onBackPressed();
	}

}
