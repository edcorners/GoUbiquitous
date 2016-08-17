package com.example.android.sunshine.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by Edison on 8/15/2016.
 */
public class LoadBitmapTask extends AsyncTask<Asset,Void,Bitmap> {

    private final String LOG_TAG = LoadBitmapTask.class.getSimpleName();
    private static final long TIMEOUT_MS = 200;
    GoogleApiClient mGoogleApiClient;
    Callback mCallback;

    public interface Callback{
        public void onLoadBitmapFinished(Bitmap bitmap);
    }

    public LoadBitmapTask(GoogleApiClient googleApiClient, Callback callback){
        this.mGoogleApiClient = googleApiClient;
        this.mCallback = callback;
    }

    /**
     * https://developer.android.com/training/wearables/data-layer/assets.html
     * @return
     */
    @Override
    protected Bitmap doInBackground(Asset... assets) {
        Asset asset = assets[0];
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(LOG_TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        mCallback.onLoadBitmapFinished(bitmap);
    }
}
