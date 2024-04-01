package de.dennisguse.opentracks.settings;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;

public class DefaultsSettingsFragment extends PreferenceFragmentCompat implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    // Used to forward update from ChooseActivityTypeDialogFragment; TODO Could be replaced with LiveData.
    private ActivityTypePreference.ActivityPreferenceDialog activityPreferenceDialog;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            getActivity().runOnUiThread(this::updateUnits);
        }
    };
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.ski_season_start_key))) {
            showCustomDatePickerDialog(); // Call method to show the dialog
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_defaults);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_defaults_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUnits();
        updateSkiSeasonStartPreferenceSummary(); // This will update the summary on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof ActivityTypePreference) {
            activityPreferenceDialog = ActivityTypePreference.ActivityPreferenceDialog.newInstance(preference.getKey());
            dialogFragment = activityPreferenceDialog;
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), getClass().getSimpleName());
            return;
        }

        super.onDisplayPreferenceDialog(preference);
    }
  
    private void showCustomDatePickerDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.custom_date_picker_dialog, null);
        builder.setView(dialogView);
    
        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        NumberPicker dayPicker = dialogView.findViewById(R.id.dayPicker);
        Preference preference = findPreference(getString(R.string.ski_season_start_key));
        String defaultStartDate = prefs.getString(getString(R.string.ski_season_start_key), "09-01");
    
        // Initialize month and day pickers
        String[] months = new DateFormatSymbols().getShortMonths();
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(months.length - 1);
        monthPicker.setDisplayedValues(months);
    
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(31);
    
        // Parse and set the default or saved date
        String[] dateParts = defaultStartDate.split("-");
        int defaultMonth = Integer.parseInt(dateParts[0]) - 1;
        int defaultDay = Integer.parseInt(dateParts[1]);
        monthPicker.setValue(defaultMonth);
        dayPicker.setValue(defaultDay);
    
        // Define dialog buttons
        builder.setTitle("Select Date");
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedMonth = monthPicker.getValue();
            int selectedDay = dayPicker.getValue();
            String selectedDate = String.format(Locale.getDefault(), "%02d-%02d", selectedMonth + 1, selectedDay);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.ski_season_start_key), selectedDate);
            editor.apply();
        
            // Update the summary text after saving the new date
            updateSkiSeasonStartPreferenceSummary();
        });
    
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
    
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void ensureDefaultSkiSeasonStartDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (!prefs.contains(getString(R.string.ski_season_start_key))) {
            // Set the default date only if it hasn't been set before
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.ski_season_start_key), "09-01");
            editor.apply();
        }
    }

    private void updateSkiSeasonStartPreferenceSummary() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Preference preference = findPreference(getString(R.string.ski_season_start_key));

        // The ensureDefaultSkiSeasonStartDate() method makes sure that a default is always set
        ensureDefaultSkiSeasonStartDate();

        String date = prefs.getString(getString(R.string.ski_season_start_key), "09-01");
        String[] dateParts = date.split("-");
        int monthIndex = Integer.parseInt(dateParts[0]) - 1;
        // Ensure the format is correctly applied to display as "Sep 1"
        String readableDate = new DateFormatSymbols().getMonths()[monthIndex].substring(0, 3) + " " + Integer.parseInt(dateParts[1]);

        if (preference != null) {
            preference.setSummary(readableDate);
        }
    }




    private int getMaxDayOfMonth(int month) {
        // Get the maximum day for the given month
        Calendar calendar = Calendar.getInstance();
        calendar.clear(); 
        calendar.set(Calendar.MONTH, month);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    private void updateUnits() {
        UnitSystem unitSystem = PreferencesUtils.getUnitSystem();

        ListPreference statsRatePreferences = findPreference(getString(R.string.stats_rate_key));

        int entriesId = switch (unitSystem) {
            case METRIC -> R.array.stats_rate_metric_options;
            case IMPERIAL_FEET, IMPERIAL_METER ->
                    R.array.stats_rate_imperial_options;
            case NAUTICAL_IMPERIAL ->
                    R.array.stats_rate_nautical_options;
        };

        String[] entries = getResources().getStringArray(entriesId);
        statsRatePreferences.setEntries(entries);

        HackUtils.invalidatePreference(statsRatePreferences);
    }

    @Override
    public void onChooseActivityTypeDone(ActivityType activityType) {
        if (activityPreferenceDialog != null) {
            activityPreferenceDialog.updateUI(activityType);
        }
    }
}