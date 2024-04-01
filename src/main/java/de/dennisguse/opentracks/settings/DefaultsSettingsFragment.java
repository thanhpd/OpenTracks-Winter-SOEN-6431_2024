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

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
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
        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        updateUnits();
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
        Preference preference = findPreference(getString(R.string.ski_season_start_key));

        // Customize month picker
        String[] months = new DateFormatSymbols().getShortMonths();
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(months.length - 1);
        monthPicker.setDisplayedValues(months); // Display month names

        // Customize day picker based on the selected month
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            int maxDay = getMaxDayOfMonth(newVal); // Get the maximum day for the selected month
            dayPicker.setMaxValue(maxDay);
            System.out.println("New Value: " + newVal + ", Old Value: " + oldVal + ", Max Day: " + maxDay);
        });

// Set initial max day for the initial month
        int initialMonth = monthPicker.getValue();
        int maxDay = getMaxDayOfMonth(initialMonth);
        dayPicker.setMaxValue(maxDay);
        System.out.println("Initial Month: " + initialMonth + ", Max Day: " + maxDay);

        // Customize day picker
        dayPicker.setMinValue(1);

        String skiSeasonStartDate = PreferencesUtils.getSkiSeasonStartDate();

        // Parse the date to extract month and day
        String[] dateParts = skiSeasonStartDate.split("-");
        int lastSelectedmonth = Integer.parseInt(dateParts[0]) - 1; // Subtract 1 as Calendar months are 0-indexed
        int lastSelectedday = Integer.parseInt(dateParts[1]);

        // Set the values to the pickers
        monthPicker.setValue(lastSelectedmonth);
        dayPicker.setValue(lastSelectedday);

        builder.setTitle("Select Date");
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedMonth = monthPicker.getValue();
            int selectedDay = dayPicker.getValue();
            String setSelectedDate = String.format(Locale.getDefault(), "%02d-%02d", selectedMonth + 1, selectedDay);

// Set the selected date as the ski season start date
            PreferencesUtils.setSkiSeasonStartDate(setSelectedDate);
            String selectedDate = String.format(Locale.getDefault(), "%02d %s", selectedDay, months[selectedMonth]);

            if (preference != null) {
                // Set summary to display the selected date
                preference.setSummary(selectedDate);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private int getMaxDayOfMonth(int month) {
        // Get the maximum day for the given month
        Calendar calendar = Calendar.getInstance();
        calendar.clear();  // Clear all fields to prevent interference from previous configurations
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