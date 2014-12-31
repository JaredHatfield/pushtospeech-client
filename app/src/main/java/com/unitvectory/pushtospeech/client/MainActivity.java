package com.unitvectory.pushtospeech.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * The Main Activity
 * 
 * @author Jared Hatfield
 * 
 */
public class MainActivity extends Activity {

    private final static String TAG = "pushtospeech";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String PROPERTY_REG_ID = "registration_id";

    private static final String PROPERTY_DEVICE_ID = "device_id";

    private static final String PROPERTY_DEVICE_SECRET = "device_secret";

    private static final String PROPERTY_APP_VERSION = "app_version";

    private static final String SENDER_ID = "1022031844776";

    private static final String USER_AGENT = "pushtospeech/client";

    private static final String SERVER_URL_WEB =
            "https://pushtospeech.appspot.com/?id=";

    private static final String SERVER_URL_TOKEN =
            "https://pushtospeech.appspot.com/api/v1/token";

    private GoogleCloudMessaging gcm;

    private Context context;

    private String regid;

    private String deviceId;

    private String deviceSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = this.getApplicationContext();

        this.deviceId = this.getDeviceId(context);
        this.deviceSecret = this.getDeviceSecret(context);

        EditText textEdit = (EditText) this.findViewById(R.id.deviceId);
        textEdit.setText(this.deviceId);

        // Check device for Play Services APK.
        if (checkPlayServices()) {
            this.gcm = GoogleCloudMessaging.getInstance(this);
            this.regid = getRegistrationId(context);

            if (this.regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If it
     * doesn't, display a dialog that allows users to download the APK from the
     * Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_website:
                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(SERVER_URL_WEB
                                + this.deviceId));
                startActivity(browserIntent);
                return true;
            case R.id.action_about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.about_title)
                        .setMessage(R.string.about_message)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        dialog.dismiss();
                                    }
                                });
                builder.create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void copyText(View view) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("id", this.deviceId);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, R.string.device_id_copied, Toast.LENGTH_LONG)
                .show();
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     * 
     * @param context
     *            the context
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion =
                prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Gets the device identifier.
     * 
     * @param context
     *            the context
     * @return device id
     */
    private String getDeviceId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String deviceId = prefs.getString(PROPERTY_DEVICE_ID, "");
        if (deviceId.isEmpty()) {
            deviceId = PasswordGenerator.newId();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_DEVICE_ID, deviceId);
            editor.commit();
        }

        return deviceId;
    }

    /**
     * Gets the device secret.
     * 
     * @param context
     *            the context
     * @return device secret
     */
    private String getDeviceSecret(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String deviceSecret = prefs.getString(PROPERTY_DEVICE_SECRET, "");
        if (deviceSecret.isEmpty()) {
            deviceSecret = PasswordGenerator.newSecret();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_DEVICE_SECRET, deviceSecret);
            editor.commit();
        }

        return deviceSecret;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(
                            context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * 
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    sendRegistrationIdToBackend();
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to the server over HTTP.
     */
    private void sendRegistrationIdToBackend() {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(SERVER_URL_TOKEN);
            post.setHeader("User-Agent", USER_AGENT);
            String json =
                    new JSONObject().put("deviceid", this.deviceId)
                            .put("devicesecret", this.deviceSecret)
                            .put("registrationid", this.regid).toString();
            Log.i(TAG, json);
            HttpEntity entity = new StringEntity(json, "UTF-8");
            post.setEntity(entity);
            client.execute(post);
        } catch (Exception e) {
            Log.e(TAG, "Unable to post token", e);
        }
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     * 
     * @param context
     *            application's context.
     * @param regId
     *            registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

}
