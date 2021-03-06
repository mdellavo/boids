package org.quuux.boids;

import android.content.res.AssetManager;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.IllegalAccessException;

import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONException;

class ProfileLoader {
    private static final String TAG = "ProfileLoader";
    
    public static final String SHARED_PREFS_NAME = "BoidsSettings";        

    private static Context context;
    private static final Map<String, Profile> profiles = new HashMap<String, Profile>();
    
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

    private static void loadProfiles(String key) {
        AssetManager asset_manager = context.getAssets();

        try {
            String profile_path = "profiles/" + key + ".json";
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Opening profile: " + profile_path);

            InputStream in = asset_manager.open(profile_path);

            byte[] bytes = new byte[in.available()];
            in.read(bytes);

            String data = new String(bytes);
            JSONTokener tokener = new JSONTokener(data);
            
            do {
                try { 
                    JSONObject obj = (JSONObject)tokener.nextValue();
                    Profile profile = loadProfile(obj);
                    profiles.put(profile.name, profile);
                } catch(JSONException e) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Could not load profile object: " + e, e);
                }
            } while(tokener.more());

        } catch(IOException e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Could not open profile: " + key, e);
        }
    }

    private static Profile loadProfile(JSONObject obj) {
        Profile profile = null;

        try {
            Class profile_class = Class.forName(obj.optString("class", "org.quuux.boids.Profile"));
            Field fields[] = profile_class.getFields();
            profile = (Profile) profile_class.newInstance();

            for(int i=0; i<fields.length; i++) {
                String field_name = fields[i].getName();
                String type_name = fields[i].getType().getName();

                if(type_name.equals("java.lang.String")) {
                    fields[i].set(profile,
                                  obj.optString(field_name,
                                                (String)fields[i].get(profile)));
                } else if(type_name.equals("float")) {
                    fields[i].setFloat(profile,
                                       (float)obj.optDouble(field_name,
                                                            fields[i].getFloat(profile)));
                } else if(type_name.equals("int")) {
                    fields[i].setInt(profile, obj.optInt(field_name,
                                                         fields[i].getInt(profile)));
                } else if(type_name.equals("long")) {
                    fields[i].setLong(profile, obj.optLong(field_name,
                                                           fields[i].getLong(profile)));
                } else if(type_name.equals("boolean")) {
                    fields[i].setBoolean(profile, obj.optBoolean(field_name,
                                                                 fields[i].getBoolean(profile)));
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Unknown Type " + type_name + " in field " + field_name);
                }
            }

        } catch(Throwable e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Could not load profile: " + e, e);
        }

        return profile;
    }

    public static JSONObject storeProfile(Profile profile) {

        JSONObject obj = new JSONObject();
        Field fields[] = profile.getClass().getFields();

        for(Field f : fields) {
            String name = f.getName();

            try {
                obj.put(name, f.get(profile));
            } catch(JSONException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Could not store field " + name + ": " + e, e);
            } catch(IllegalAccessException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Could not store field " + name + ": " + e, e);
            }
        }

        return obj;
    }

    public static void saveProfile(Profile profile,
                                          SharedPreferences preferences) {
        String json = storeProfile(profile).toString();
        
        if (BuildConfig.DEBUG) Log.d(TAG, "saving profile to preferences: " + json);

        Editor editor = preferences.edit();
        editor.putString("profile", json);       
        editor.commit();
    }

    public static void  updateProfile(Profile profile, 
                                      SharedPreferences preferences) {

        try {
            Field fields[] = profile.getClass().getFields();

            for(Field field : fields) {
                String field_name = field.getName();

                if(preferences.contains(field_name))
                    updateProfile(profile, preferences, field_name);
            }
        } catch(Throwable e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Could not update profile: " + e, e);
        }

    }

    public static void updateProfile(Profile profile,
                                     SharedPreferences preferences,
                                     String key) {

        try {
            Field field = profile.getClass().getField(key.toUpperCase());
            String type_name = field.getType().getName();

            try {
                if (BuildConfig.DEBUG) Log.d(TAG, "Setting field: " + key);

                if(type_name.equals("java.lang.String")) {
                    field.set(profile,
                              preferences.getString(key, (String)field.get(profile)));
                } else if(type_name.equals("float")) {
                    field.setFloat(profile,
                                   (float)preferences.getInt(key,
                                                             (int)field.getFloat(profile)));
                } else if(type_name.equals("int")) {
                    field.setInt(profile, preferences.getInt(key, field.getInt(profile)));
                } else if(type_name.equals("long")) {
                    field.setLong(profile, (long)preferences.getInt(key, (int)field.getLong(profile)));
                } else if(type_name.equals("boolean")) {
                    field.setBoolean(profile, preferences.getBoolean(key, field.getBoolean(profile)));
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Unknown Type " + type_name + " in field " + key);
                }

            } catch(IllegalAccessException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Error setting field " + key + ": " + e, e);
            }

        } catch(NoSuchFieldException e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No Such Field " + key + ": " + e, e);
        }
    }
}

