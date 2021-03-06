package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutionException;

/**
 * Created by Edison on 8/18/2016.
 */
public class WatchFaceUpdater  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public final String LOG_TAG = WatchFaceUpdater.class.getSimpleName();

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private Bitmap mIcon;
    private double high;
    private double low;

    private static WatchFaceUpdater instance;

    public static WatchFaceUpdater getInstance(Context context){
        instance = instance == null ? new WatchFaceUpdater(context):instance;
        return instance;
    }

    private WatchFaceUpdater(Context context) {
        this.mContext = context;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void updateWatchFace() {
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Last sync was more than 1 day ago, let's send a notification with the weather.
        String locationQuery = Utility.getPreferredLocation(mContext);

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        // we'll query our contentProvider, as always
        Cursor cursor = mContext.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            final int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            high = cursor.getDouble(INDEX_MAX_TEMP);
            low = cursor.getDouble(INDEX_MIN_TEMP);

            int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
            Resources resources = mContext.getResources();
            int artResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
            String artUrl = Utility.getArtUrlForWeatherCondition(mContext, weatherId);

            int iconWidth =  resources.getDimensionPixelSize(R.dimen.watch_face_icon_size);
            int iconHeight = resources.getDimensionPixelSize(R.dimen.watch_face_icon_size);

            try {
                mIcon = Glide.with(mContext)
                        .load(artUrl)
                        .asBitmap()
                        .error(artResourceId)
                        .fitCenter()
                        .into(iconWidth, iconHeight).get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(LOG_TAG, "Error retrieving large mIcon from " + artUrl, e);
                mIcon = BitmapFactory.decodeResource(resources, artResourceId);
            }
        }
        sendWeatherData();
        cursor.close();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected: " + bundle);
        sendWeatherData();
    }

    private void sendWeatherData() {
        if(mIcon != null) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(mContext.getString(R.string.weather_data_uri));
            dataMapRequest.getDataMap().putString(mContext.getString(R.string.data_item_high), Utility.formatTemperature(mContext, high));
            dataMapRequest.getDataMap().putString(mContext.getString(R.string.data_item_low), Utility.formatTemperature(mContext, low));
            dataMapRequest.getDataMap().putAsset(mContext.getString(R.string.data_item_icon), Utility.createAssetFromBitmap(mIcon));
            dataMapRequest.getDataMap().putString(mContext.getString(R.string.data_item_timestamp), "_" + System.currentTimeMillis());
            dataMapRequest.setUrgent();
            Log.d(LOG_TAG, " high:" + high + " low:" + low);
            PutDataRequest request = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    if (dataItemResult.getStatus().isSuccess()) {
                        Log.d(LOG_TAG, "Data stored " + dataItemResult.getDataItem().toString());
                    } else {
                        Log.d(LOG_TAG, "Data not stored");
                    }
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed: " + connectionResult);
    }

}
