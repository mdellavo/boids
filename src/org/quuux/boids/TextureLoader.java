package org.quuux.boids;

import android.content.res.AssetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.IOException;

class TextureLoader {
    private static final String TAG = "TextureLoader";
    
    private static Context context;
    private static Map<String, Texture> cache;
    
    public static void init(Context context) {
        TextureLoader.context = context;
        TextureLoader.cache   = new HashMap<String, Texture>();
    }

    public static Texture get(String key) {
        Texture rv = TextureLoader.cache.get(key);

        if(rv == null) {
            rv = openTexture(key);
            cache.put(key, rv);
        }
        
        return rv;
    }

    private static Texture openTexture(String key) {
        AssetManager asset_manager = context.getAssets();

        Texture rv = null;

        try {
            String texture_path = "textures/" + key + ".png";
            
            Log.d(TAG, "Opening texture: " + texture_path);

            InputStream in = asset_manager.open(texture_path);
            rv = new Texture(key, BitmapFactory.decodeStream(in));
        } catch(IOException e) {
            Log.d(TAG, "Could not open texture: " + key);
        }

        return rv;
    }

}
