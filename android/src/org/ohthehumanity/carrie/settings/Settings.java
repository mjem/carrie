package org.ohthehumanity.carrie.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import org.ohthehumanity.carrie.R;

/** Handle opening our Preferences page, defined in res/xml/settings.xml
 **/

public class Settings extends PreferenceActivity { //implements OnSharedPreferenceChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
    }
}
