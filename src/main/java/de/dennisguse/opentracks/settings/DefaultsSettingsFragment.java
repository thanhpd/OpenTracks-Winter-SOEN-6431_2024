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
    
        String[] months = new DateFormatSymbols().getShortMonths();
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(months.length - 1);
        monthPicker.setDisplayedValues(months);
    
        // Assuming the date format in the summary is "dd MMM"
        String currentDate = preference.getSummary().toString();
        if (!currentDate.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            try {
                cal.setTime(new SimpleDateFormat("dd MMM", Locale.getDefault()).parse(currentDate));
                int currentMonth = cal.get(Calendar.MONTH);
                int currentDay = cal.get(Calendar.DAY_OF_MONTH);
    
                monthPicker.setValue(currentMonth);
                updateDayPicker(dayPicker, currentMonth);
    
                dayPicker.setValue(currentDay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateDayPicker(dayPicker, newVal));
    
        builder.setTitle("Select Date");
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedMonth = monthPicker.getValue();
            int selectedDay = dayPicker.getValue();
    
            String selectedDate = String.format(Locale.getDefault(), "%02d %s", selectedDay, months[selectedMonth]);
            if (preference != null) {
                preference.setSummary(selectedDate);
            }
        });
    
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void updateDayPicker(NumberPicker dayPicker, int month) {
        int maxDay = getMaxDayOfMonth(month);
        dayPicker.setMaxValue(maxDay);
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