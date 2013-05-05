package org.quuux.boids;

import android.util.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.os.Build;

import android.provider.Settings.Secure;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.entity.StringEntity;

class CheckInThread extends Thread {      
    private static final String TAG = "CheckInThread";

    private static final String ENDPOINT = "http://collector.quuux.org";

    private final String package_name;
    private final String device_id;
    private final String key;
    protected String endpoint;
    private final JSONObject data;

    public CheckInThread(String package_name, String device_id, String key, 
                         JSONObject data) {
        this.package_name = package_name;
        this.device_id = device_id;
        this.key = key;
        this.data = data;
    }
        
    public void run() {
        try {
            data.put("application", package_name);
            data.put("device_id", device_id);
            data.put("type", key);

            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(ENDPOINT);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(data.toString()));

            String json = client.execute(request, 
                                         new BasicResponseHandler());
            JSONObject response = (JSONObject)new JSONTokener(json).nextValue();

            if(response.has("status") && 
               response.getString("status").equals("ok")) {  
                if (BuildConfig.DEBUG) if (BuildConfig.DEBUG) Log.d(TAG, "Collector accepted " + key + " checkin");
                CheckInManager.setCheckedIn(key);
            } else {
                if (BuildConfig.DEBUG) if (BuildConfig.DEBUG) Log.d(TAG, "Collector did not accept " + key + " checkin");
            }

        } catch(Exception e) {
            if (BuildConfig.DEBUG) if (BuildConfig.DEBUG) Log.d(TAG, "Could not send check in: " + e);
        }            
    }
}

class CheckInManager {
    private static final String TAG = "CheckInManager";

    private static final String SHARED_PREFS_NAME = "CheckIns";

    private static final String DEVICE_CHECKIN = "DEVICE";
    private static final String GL_CHECKIN = "GL";

    private static Context context;
    
    public static void init(Context c) {
        context = c;
        sendDeviceProfile();
    }

    private static boolean hasCheckedIn(String key) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, 0);
        return prefs.getBoolean(key, false);
    }
      
    static void setCheckedIn(String key) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, 0);
        Editor editor = prefs.edit();
        editor.putBoolean(key, true);
        editor.commit();
    }

    private static void sendDeviceProfile() {
        if(hasCheckedIn(DEVICE_CHECKIN))
            return;

        JSONObject data = new JSONObject();

        try { 
            data.put("device", Build.DEVICE);
            data.put("display", Build.DISPLAY);
            data.put("manufacturer", Build.MANUFACTURER);
            data.put("model", Build.MODEL);
            data.put("product", Build.PRODUCT);
            data.put("hardware", Build.HARDWARE);

            checkIn(DEVICE_CHECKIN, data);
        } catch(Exception e) {
            if (BuildConfig.DEBUG) if (BuildConfig.DEBUG) Log.d(TAG, "Could not send device check in: " + e);
        }
    }

    public static void sendGLProfile() {
        if(hasCheckedIn(GL_CHECKIN))
            return;

        JSONObject data = new JSONObject();

        try {
            data.put("gl_vendor", GLHelper.getVendor());
            data.put("gl_renderer", GLHelper.getRenderer());
            data.put("gl_version", GLHelper.getVersion());
            data.put("gl_extensions", GLHelper.getExtensions());
        
            checkIn(GL_CHECKIN, data);
        } catch(Exception e) {
            if (BuildConfig.DEBUG) if (BuildConfig.DEBUG) Log.d(TAG, "Could not send GL check in: " + e);
        }

    }

    private static String getDeviceID() {
        return Secure.getString(context.getContentResolver(), 
                                Secure.ANDROID_ID);
    }

    private static String getPackageName() {
        return context.getPackageName();
    }

    private static void checkIn(String key, JSONObject data) {
        if (BuildConfig.DEBUG) if (BuildConfig.DEBUG) Log.d(TAG, "Sending " + key + " check in");
        new CheckInThread(getPackageName(), getDeviceID(), key, data).start();
    }
}
