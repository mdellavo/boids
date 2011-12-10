package org.quuux.boids;

import android.content.Context;
import android.util.Log;
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

class StatsCollector
{    
    private static final String TAG = "StatsCollector";
    private static final String ENDPOINT = "http://collector.quuux.org";

    private static Context context;
    private static JSONObject data;
    private static boolean dirty;

    public static void init(Context context) {
        StatsCollector.context = context;
        data = new JSONObject();
        fetchProfile();
    }

    public static void fetchProfile() {
        put("application", StatsCollector.class.getPackage().getName());
        put("device_id",
            Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
        put("device", Build.DEVICE);
        put("display", Build.DISPLAY);
        put("manufacturer", Build.MANUFACTURER);
        put("model", Build.MODEL);
        put("product", Build.PRODUCT);
        put("hardware", Build.HARDWARE);
    }

    public static boolean post() {
        if(!dirty)
            return true;

        boolean rv = false;

        Log.d(TAG, "Posting " + data.toString() + " stats to " + ENDPOINT); 

        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(ENDPOINT);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(data.toString()));

            String json = client.execute(request, new BasicResponseHandler());
            JSONObject response = (JSONObject)new JSONTokener(json).nextValue();

            if(response.has("status") && 
               response.getString("status").equals("ok")) {  
                rv = true;
                dirty = false;
                Log.d(TAG, "POST complete");
            } else {
                Log.d(TAG, "POST did not return ok");
            }

        } catch(Exception e) {
            Log.d(TAG, "Could not post stats: " + e);
        }

        return rv;
    }
 
    public static boolean put(String key, int value) {
        return put(key, new Integer(value));
    }
    
    public static boolean put(String key, long value) {
        return put(key, new Long(value));
    }
   
    public static boolean put(String key, boolean value) {
        return put(key, new Boolean(value));
    }

    public static boolean put(String key, double value) {
        return put(key, new Double(value));
    }

    public static boolean put(String key, Object value) {
        boolean rv = false;

        try {
            data.put(key, value);
            dirty = true;
            rv = true;
        } catch(JSONException e) {    
            Log.d(TAG, "could not put stat: " + e);   
        }

        return rv;
    }

}
