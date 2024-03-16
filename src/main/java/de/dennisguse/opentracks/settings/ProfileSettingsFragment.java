package de.dennisguse.opentracks.settings;

import static android.graphics.BitmapFactory.decodeFile;
import static android.graphics.BitmapFactory.decodeResource;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.dennisguse.opentracks.R;

public class ProfileSettingsFragment extends PreferenceFragmentCompat {
    ImageViewPreference imageViewPreference;
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    Bitmap profilePicture;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.night_mode_key, key)) {
            getActivity().runOnUiThread(PreferencesUtils::applyNightMode);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_profile);
        imageViewPreference = findPreference(getString(R.string.settings_profile_profile_picture_key));

        if (imageViewPreference != null) {
            pickMedia =
                    registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                        // Callback is invoked after the user selects a media item or closes the
                        // photo picker.
                        if (uri != null) {
                            try {
                                ImageView imageView2 = imageViewPreference.getImageView();
                                profilePicture = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                                imageView2.setImageBitmap(profilePicture);

                                try {
                                    File file = new File(getContext().getFilesDir(), getString(R.string.settings_profile_profile_picture_key));
                                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                                    profilePicture.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                                    fileOutputStream.close();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(getContext(), "No media selected", Toast.LENGTH_SHORT).show();
                        }
                    });
            imageViewPreference.setImageClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //do whatever you want on image click here
                    Toast.makeText(getContext(), "Image Clicked", Toast.LENGTH_SHORT).show();
                    pickMedia.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                }
            });
        }

    }

    private Bitmap loadProfilePicture() {
        Bitmap b = null;

        try {
            File f = new File(getContext().getFilesDir(), getString(R.string.settings_profile_profile_picture_key));
            b = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException ex) {
            Toast.makeText(getContext(), "Cannot find image", Toast.LENGTH_SHORT).show();
        }

        return b;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_profile_title);

        imageViewPreference = findPreference(getString(R.string.settings_profile_profile_picture_key));
        ImageView imageView = imageViewPreference.getImageView();
        profilePicture = loadProfilePicture();
        if (profilePicture != null && imageView != null) {
            imageView.setImageBitmap(profilePicture);
        }
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
