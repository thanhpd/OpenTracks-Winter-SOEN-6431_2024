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
        ListPreference countryPreference = findPreference("country_preference_key");
        String defaultCountry = getResources().getString(R.string.default_country_value); // Set your default country value here
        countryPreference.setDefaultValue(defaultCountry);


        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        String selectedCountryValue = PreferencesUtils.getSelectedCountry(sharedPreferences);
        if (selectedCountryValue != null) {
            countryPreference.setSummary(selectedCountryValue);
        } else {
            countryPreference.setSummary(defaultCountry);
        }


        countryPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            // Save the selected country to SharedPreferences
            PreferencesUtils.saveSelectedCountry(getPreferenceManager().getSharedPreferences(), (String) newValue);


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
        EditTextPreference nickNameInput = findPreference(getString(R.string.settings_profile_nickname_key));
        nickNameInput.setDialogTitle(getString(R.string.settings_profile_nickname_dialog_title));
        nickNameInput.setOnBindEditTextListener(editText -> {
            editText.setSingleLine(true);
            editText.selectAll(); // select all text
            int maxNicknameLength = 20;
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxNicknameLength)});
        });
        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);


        String countryKey = "country_preference_key";
        ListPreference countryPreference = findPreference(countryKey);
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        String selectedCountryValue = PreferencesUtils.getSelectedCountry(sharedPreferences);
        if (selectedCountryValue != null) {
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
