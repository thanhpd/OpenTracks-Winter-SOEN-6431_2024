package de.dennisguse.opentracks.settings;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.text.DateFormatSymbols;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.custom_date_picker_dialog, null);
        builder.setView(dialogView);
    
        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        NumberPicker dayPicker = dialogView.findViewById(R.id.dayPicker);
        
        String defaultStartDate = PreferencesUtils.getSkiSeasonStartDate();
    
        // Initialize month picker
        String[] months = new DateFormatSymbols().getMonths();  // Full months array
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(months.length - 1);
        monthPicker.setDisplayedValues(months);
    
        // Initialize day picker with maximum value based on the month
        int month = Integer.parseInt(defaultStartDate.split("-")[0]) - 1;
        int day = Integer.parseInt(defaultStartDate.split("-")[1]);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month);
        dayPicker.setMaxValue(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        dayPicker.setMinValue(1);
    
        // Set current values
        monthPicker.setValue(month);
        dayPicker.setValue(Math.min(day, dayPicker.getMaxValue()));  // Ensure day is within the valid range
    
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            // Adjust the maximum number of days according to the selected month
            calendar.set(Calendar.MONTH, newVal);
            int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            dayPicker.setMaxValue(maxDay);
    
            // Adjust day value if it exceeds the max day for the new month
            if (dayPicker.getValue() > maxDay) {
                dayPicker.setValue(maxDay);
            }
        });
    
        builder.setTitle("Select Date");
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Save selected date and update summary
            int selectedMonth = monthPicker.getValue();
            int selectedDay = dayPicker.getValue();
            String selectedDate = String.format(Locale.getDefault(), "%02d-%02d", selectedMonth + 1, selectedDay);

            PreferencesUtils.setSkiSeasonStartDate(selectedDate);
    
            // Update the preference summary
            updateSkiSeasonStartPreferenceSummary();
        });
    
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    

    private void ensureDefaultSkiSeasonStartDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (!prefs.contains(getString(R.string.ski_season_start_key))) {
            // Set the default date only if it hasn't been set before
            PreferencesUtils.setSkiSeasonStartDate("09-01");
        }
    }

    private void updateSkiSeasonStartPreferenceSummary() {
        Preference preference = findPreference(getString(R.string.ski_season_start_key));

        // The ensureDefaultSkiSeasonStartDate() method makes sure that a default is always set
        ensureDefaultSkiSeasonStartDate();

        String date = PreferencesUtils.getSkiSeasonStartDate();
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