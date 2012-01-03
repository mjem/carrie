// Carrie Remote Control
// Copyright (C) 2012 Mike Elson

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.ohthehumanity.carrie;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.util.Log;
import android.net.Uri;
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
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.homepage))));

			finish();
			return true;
		} else {
			return false;
		}
	}
}
