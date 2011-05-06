package org.quuux.boids;

import android.content.res.AssetManager;
import android.content.Context;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.IOException;

import java.lang.reflect.Field;

import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONException;

class ProfileLoader {
    private static final String TAG = "ProfileLoader";
    
    private static Context context;
    private static Map<String, Profile> profiles = new HashMap<String, Profile>();
    
    public static void init(Context context) {
        ProfileLoader.context = context;
        loadProfiles("default");
    }

    public static Profile getProfile(String key) {
        return profiles.get(key);
    }

    public static String[] getProfileNames() {
        return (String[])profiles.keySet().toArray(new String[0]);
    }

    protected static void loadProfiles(String key) {
        AssetManager asset_manager = context.getAssets();
        try {
            String profile_path = "profiles/" + key + ".json";
            
            Log.d(TAG, "Opening profile: " + profile_path);

            InputStream in = asset_manager.open(profile_path);

            byte[] bytes = new byte[in.available()];
            in.read(bytes);

            String data = new String(bytes);
            JSONTokener tokener = new JSONTokener(data);
            
            while(tokener.more()) {
                try { 
                    JSONObject obj = (JSONObject)tokener.nextValue();                
                    Profile profile = loadProfile(obj);
                    profiles.put(profile.name, profile);
                } catch(JSONException e) {
                    Log.d(TAG, "Could load profile object: " + data);
                }
            }

        } catch(IOException e) {
            Log.d(TAG, "Could not open profile: " + key);
        }
    }

    public static Profile loadProfile(JSONObject obj) {
        Profile profile = null;

        try {
            Class profile_class = Class.forName(obj.optString("class", "Profile"));
            Field fields[] = profile_class.getFields();
            profile = (Profile) profile_class.newInstance();

            for(int i=0; i<fields.length; i++) {
                String field_name = fields[i].getName();
                String type_name = fields[i].getType().getName();

                if(type_name.equals("java.lang.String")) {
                    fields[i].set(profile,obj.optString(field_name, ""));
                } else if(type_name.equals("float")) {
                    fields[i].setFloat(profile,
                                       (float)obj.optDouble(field_name, 0));
                } else if(type_name.equals("int")) {
                    fields[i].setInt(profile, obj.optInt(field_name, 0));
                } else if(type_name.equals("long")) {
                    fields[i].setLong(profile, obj.optLong(field_name, 0));
                } else if(type_name.equals("boolean")) {
                    fields[i].setBoolean(profile, obj.optBoolean(field_name, false));
                } else {
                    Log.d(TAG, "Unknown Type " + type_name + " in field " + field_name);
                }
            }
        } catch(Throwable e) {
            Log.d(TAG, "Could not load profile: " + e);
        }

        return profile;
    }

}

