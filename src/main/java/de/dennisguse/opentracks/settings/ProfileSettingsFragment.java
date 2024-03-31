package de.dennisguse.opentracks.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;


import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;


import de.dennisguse.opentracks.R;


public class ProfileSettingsFragment extends PreferenceFragmentCompat {


    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.night_mode_key, key)) {
            getActivity().runOnUiThread(PreferencesUtils::applyNightMode);
        }
    };


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_profile);
        ListPreference countryPreference = findPreference(getString(R.string.settings_profile_country_key));


        String selectedCountryValue = PreferencesUtils.getSelectedCountry();
        countryPreference.setSummary(selectedCountryValue);

        countryPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            // Save the selected country to SharedPreferences
            PreferencesUtils.setSelectedCountry((String) newValue);

            // Update summary with selected country
            preference.setSummary((String) newValue);
            return true;
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_profile_title);
    }


    @Override
    public void onResume() {
        super.onResume();

        ListPreference countryPreference = findPreference(getString(R.string.settings_profile_country_key));
        String selectedCountryValue = PreferencesUtils.getSelectedCountry();
        if (selectedCountryValue != null && !selectedCountryValue.isEmpty()) {
            // Update summary with saved selected country
            countryPreference.setSummary(selectedCountryValue);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}
