package com.fightevil.yota;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
        PreferenceScreen ps = (PreferenceScreen)findPreference("ratePreferenceScreen");
        if (YotaNavigator.RateNames != null){
            Preference button = new Preference(this);
            button.setTitle(R.string.tarif_reload_title);
            button.setSummary(R.string.tarif_reload_summary);
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ClearRateNames();
                    SettingsActivity.this.finish();
                    return true;
                }
            });
            ps.addPreference(button);
            for (int i = 0; i < YotaNavigator.RateNames.length; ++i){
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this);
                checkBoxPreference.setKey("ratePreference"+i);
                checkBoxPreference.setTitle(YotaNavigator.RateNames[i]);
                checkBoxPreference.setChecked(DefaultVisibilityListViewElem(i));
                ps.addPreference(checkBoxPreference);
            }
        } else {
            Preference rateError = new Preference(this);
            rateError.setTitle(R.string.tarif_not_determed_title);
            rateError.setSummary(R.string.tarif_not_determed_summary);
            ps.addPreference(rateError);
        }
    }

    private void ClearRateNames(){
        YotaNavigator.RateNames = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("rateCount");
        editor.commit();
    }

    static public Boolean DefaultVisibilityListViewElem(int rateNumber){
        if (rateNumber == 0)
            return false;
        return true;
    }
}