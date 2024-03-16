package de.dennisguse.opentracks.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
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
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}
