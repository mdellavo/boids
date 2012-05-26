package org.quuux.boids;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;

public class Camera {

    protected Vector3 eye;
    protected Vector3 center;
    protected Vector3 up;
    protected Vector3 rotation;
    protected float fov;
    protected float znear; 
    protected float zfar;

    public Camera() {
        eye = new Vector3(0, 0, 500);
        center = new Vector3(0, 0, -1600);
        up = new Vector3(0f, 1f, 0f);    
        rotation = new Vector3(0, 0, 0);
        fov = 120f;
        znear = .1f;
        zfar = 3200f;
    }

    public void init(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);        

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU.gluPerspective(gl, fov, (float)width/(float)height, znear, zfar);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        GLU.gluLookAt(gl, eye.x, eye.y, eye.z, 
                          center.x, center.y, center.z,
                          up.x, up.y, up.z );
    }

    // FIXME add translation, zoom, etc
    public void update(GL10 gl) {
        gl.glRotatef(rotation.x, 1f, 1f, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 1f, 1f);
    }

    public void tick(long elapsed) {
        //rotation.y += .0001f;
    }

}
