/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.TrackWriter;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;

/**
 * AsyncTask to export all the tracks to the SD card.
 *
 * @author Jimmy Shih
 */
public class ExportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private ExportActivity exportActivity;
  private final TrackFileFormat trackFileFormat;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private WakeLock wakeLock;
  private TrackWriter trackWriter;

  // true if the AsyncTask result is success
  private boolean success;

  // true if the AsyncTask has completed
  private boolean completed;

  // message id to return to the activity
  private int messageId;
  
  /**
   * Creates an AsyncTask.
   *
   * @param exportActivity the activity currently associated with this AsyncTask
   * @param trackFileFormat the track file format
   */
  public ExportAsyncTask(ExportActivity exportActivity, TrackFileFormat trackFileFormat) {
    this.exportActivity = exportActivity;
    this.trackFileFormat = trackFileFormat;
    context = exportActivity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(exportActivity);

    // Get the wake lock if not recording
    if (PreferencesUtils.getRecordingTrackId(exportActivity) == -1L) {
      wakeLock = SystemUtils.acquireWakeLock(exportActivity, wakeLock);
    }
    success = false;
    completed = false;
    messageId = R.string.export_error;
  }

  /**
   * Sets the current {@link ExportActivity} associated with this AyncTask.
   *
   * @param exportActivity the current {@link ExportActivity}, can be null
   */
  public void setActivity(ExportActivity exportActivity) {
    this.exportActivity = exportActivity;
    if (completed && exportActivity != null) {
      exportActivity.onAsyncTaskCompleted(success, messageId);
    }
  }

  @Override
  protected void onPreExecute() {
    if (exportActivity != null) {
      exportActivity.showProgressDialog();
    }
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getTracksCursor(null, null, TracksColumns._ID);
      if (cursor == null) {
        messageId = R.string.export_success;
        return true;
      }
      int count = cursor.getCount();
      int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
      for (int i = 0; i < count; i++) {
        if (isCancelled()) {
          return false;
        }
        cursor.moveToPosition(i);
        long id = cursor.getLong(idIndex);
        trackWriter = TrackWriterFactory.newWriter(
            context, myTracksProviderUtils, id, trackFileFormat);
        if (trackWriter == null) {
          return false;
        }
        trackWriter.writeTrack();

        if (!trackWriter.wasSuccess()) {
          messageId = trackWriter.getErrorMessage();
          return false;
        }
        publishProgress(i + 1, count);
      }
      messageId = R.string.export_success;
      return true;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      // Release the wake lock if obtained
      if (wakeLock != null && wakeLock.isHeld()) {
        wakeLock.release();
      }
    }
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (exportActivity != null) {
      exportActivity.setProgressDialogValue(values[0], values[1]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (exportActivity != null) {
      exportActivity.onAsyncTaskCompleted(success, messageId);
    }
  }

  @Override
  protected void onCancelled() {
    if (trackWriter != null) {
      trackWriter.stopWriteTrack();
    }
  }
}
