package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;

public class ImageViewPreference extends Preference {

    private ImageView imageView;
    private TextView textView;
    View.OnClickListener imageClickListener;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.settings_profile_nickname_key, key)) {
            textView.setText(PreferencesUtils.getProfileNickname());
        }
    };

    public ImageViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    //onBindViewHolder() will be called after we call setImageClickListener() from SettingsFragment
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        imageView = (ImageView) holder.findViewById(R.id.image);
        textView = (TextView) holder.findViewById(R.id.nickname);
        imageView.setOnClickListener(imageClickListener);
        textView.setText(PreferencesUtils.getProfileNickname());

        Bitmap bm = loadProfilePicture();
        if (bm != null) {
            imageView.setImageBitmap(bm);
        }

        getSharedPreferences().registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private Bitmap loadProfilePicture() {
        Bitmap b = null;

        try {
            File f = new File(getContext().getFilesDir(), getContext().getString(R.string.settings_profile_profile_picture_key));
            b = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException ex) {
            Toast.makeText(getContext(), "Cannot find image", Toast.LENGTH_SHORT).show();
        }

        return b;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageClickListener(View.OnClickListener onClickListener)
    {
        imageClickListener = onClickListener;
    }
}
