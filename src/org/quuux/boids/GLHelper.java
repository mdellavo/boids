package org.quuux.boids;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import java.util.ArrayList;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// derived from https://github.com/lithium/android-game
public class GLHelper
{
    private static final String TAG = "GLHelper";

    protected static String vendor;
    protected static String renderer;
    protected static String version;
    protected static String extensions;

    public static void init(GL10 gl) {
        inspect(gl);
    }

    public static void inspect(GL10 gl) {
        vendor = gl.glGetString(GL10.GL_VENDOR);
        renderer = gl.glGetString(GL10.GL_RENDERER);
        version = gl.glGetString(GL10.GL_VERSION);
        extensions = gl.glGetString(GL10.GL_EXTENSIONS);
    }

    public static String getVendor() {
        return vendor;
    }    

    public static String getRenderer() {
        return renderer;
    }    

    public static String getVersion() {
        return version;
    }    

    public static String getExtensions() {
        return extensions;
    }    

    public static boolean hasExtension(String extension) {
        return extensions.indexOf(extension) >= 0;        
    }

    public static boolean hasVertexBufferObject() {
        return hasExtension("vertex_buffer_object");
    }

    public static boolean hasDrawTexture() {
        return hasExtension("draw_texture");
    }

    public static boolean hasPointSprite() {
        return hasExtension("point_sprite");
    }

    public static FloatBuffer floatBuffer(int size) {
        ByteBuffer byte_buf = ByteBuffer.allocateDirect(size * 4);
        byte_buf.order(ByteOrder.nativeOrder());
        return byte_buf.asFloatBuffer();
    }

    public static FloatBuffer toFloatBuffer(float[] data) {
        FloatBuffer rv = floatBuffer(data.length);

        for(int i=0; i<data.length; i++)
            rv.put(data[i]);
        
        rv.position(0);

        return rv;
    }

    public static FloatBuffer toFloatBuffer(Vector3[] data) {
        FloatBuffer rv = floatBuffer(data.length*3);

        for(int i=0; i<data.length; i++) {
            rv.put(data[i].x);
            rv.put(data[i].y);
            rv.put(data[i].z);
        }

        rv.position(0);

        return rv;
    }

    public static FloatBuffer toFloatBuffer(Vector2[] data) {
        FloatBuffer rv = floatBuffer(data.length*2);

        for(int i=0; i<data.length; i++) {
            rv.put(data[i].x);
            rv.put(data[i].y);
        }

        rv.position(0);

        return rv;
    }
    
    public static int createTexture(GL10 gl) {
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);

        Log.d(TAG, "Created Texture: " + textures[0]);

        return textures[0];
    }

    public static int loadTexture(GL10 gl, Bitmap bitmap) {
        int texture = createTexture(gl);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);        
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

        return texture;
    }
    
    public static int createBufferObject(GL10 gl) {
        int[] rv = new int[1];
        ((GL11)gl).glGenBuffers(1, rv, 0);
        return rv[0];
    }
    
    public static int loadBufferObject(GL10 gl, FloatBuffer data) {
        int buffer = createBufferObject(gl);
        
        ((GL11)gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, buffer);
        ((GL11)gl).glBufferData(GL11.GL_ARRAY_BUFFER, data.limit() * 4, data, 
                                GL11.GL_STATIC_DRAW);

        return buffer;
    }

    // TODO implement fallback to a single rect if needed
    public static void drawTexture(GL10 gl, int x, int y, int z, int w, int h) {
        int[] crop = {0, h, w, -h}; 

        ((GL11)gl).glTexParameteriv(GL10.GL_TEXTURE_2D, 
                                    GL11Ext.GL_TEXTURE_CROP_RECT_OES, crop, 0);

        ((GL11Ext) gl).glDrawTexfOES(x, y, z, w, h); 

    }

    // TODO implement with fallback to a single rect if needed
    public static void drawPoints(GL10 gl, FloatBuffer points, 
                                  FloatBuffer sizes, FloatBuffer colors) {
    }
}
