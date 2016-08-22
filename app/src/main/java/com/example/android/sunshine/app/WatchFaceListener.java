package com.example.android.sunshine.app;

import android.util.Log;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Edison on 8/21/2016.
 */
public class WatchFaceListener extends WearableListenerService {
    public final String LOG_TAG = WatchFaceListener.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(LOG_TAG, "onDataChanged: " + dataEvents);
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            DataItem dataItem = dataEvent.getDataItem();
            String path = dataItem.getUri().getPath();
            if (!path.equals("/weather_data_request")) {
                continue;
            }
            SunshineSyncAdapter.syncImmediately(this);
        }
        super.onDataChanged(dataEvents);
    }
}
