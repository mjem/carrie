package org.ohthehumanity.carrie;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.util.Log;
import android.content.Intent;

import org.ohthehumanity.carrie.R;

/** Handle opening our Preferences page, defined in res/xml/settings.xml
 **/

public class CarriePreferences extends PreferenceActivity implements OnPreferenceClickListener {
	//implements OnSharedPreferenceChangeListener {
	private static final String TAG = "carrie";

	Preference mScan;
	Preference mHomepage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
			mScan = findPreference("scan");
			mScan.setOnPreferenceClickListener(this);
			mHomepage = findPreference("homepage");
			mHomepage.setOnPreferenceClickListener(this);
    }

	public boolean onPreferenceClick (Preference preference) {
		Log.i(TAG, "onPreferencesClick " + preference.getTitle());
		if (preference ==mScan) {
			Log.i(TAG, "Scan request");
			Intent i = new Intent();
			i.putExtra("scan", true);
			setResult(RESULT_OK, i);
			finish();
			return true;
		} else if (preference == mHomepage) {
			Log.i(TAG, "Open homepage");
			finish();
			return true;
		} else {
			return false;
		}
	}
}
