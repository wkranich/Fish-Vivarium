/*
 * Food.java
 * 
 * William Kranich - wkranich@bu.edu
 * 
 * OpenGL object that draws food as a brown sphere.
 * Dropped from a random x and z point and falls to the
 * bottom of the tank for fish to eat.
 */

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.gl2.GLUT;

import java.util.*;

public class Food {
	private GLUT glut;
	private Random rand;
	
	public float x, y, z;
	public int food_obj;
	private float speed;
	public float radius;
	private boolean eaten;
	
	public Food() {
		glut = new GLUT();
		rand = new Random();
		
		x = rand.nextFloat()*8 - 4;
		y = 2.0f;
		z = rand.nextFloat()*4 - 2;
		speed = 0.01f;
		radius = 0.1f;
		eaten = false;
	}
	
	public void init(GL2 gl) {
		food_obj = gl.glGenLists(1);
		gl.glNewList(food_obj, GL2.GL_COMPILE);
		glut.glutSolidSphere(radius, 36, 24);
		gl.glEndList();
	}
	
	public void update(GL2 gl) {
		if (y > -1.9f) {
			y -= speed;
		}
	}
	
	public void draw(GL2 gl) {
		gl.glPushMatrix();
	    gl.glPushAttrib( GL2.GL_CURRENT_BIT );
	    gl.glTranslatef(x, y, z);
	    gl.glColor3f( 0.625f, 0.32f, 0.176f); // brown
	    gl.glCallList( food_obj );
	    gl.glPopAttrib();
	    gl.glPopMatrix();
	}
	
	public boolean isEaten() {
		return eaten;
	}
	
	public void eaten() {
		eaten = true;
	}

}
