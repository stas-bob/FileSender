package de.stas;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity{
	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.xml.settings);
	}
}
