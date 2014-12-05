/*
 * Fish.java
 * 
 * William Kranich - wkranich@bu.edu
 * 
 * Object for an OpenGL fish that moves along 3 axes of a tank,
 * avoids Sharks, and "eats" food.
 */

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.gl2.GLUT;

import java.util.*;

public class Fish {
	private GLUT glut;
	public float x, y, z, last_x, last_y, last_z;
	private int fish_obj;
	private int tail_obj;
	private int body_obj;
	private int box_obj;

	private float scale;
	private boolean dead;

	private float tail_angle;
	private float tail_speed;
	private float tail_direction;
	private float body_speed;
	private float body_angle;

	private float trans_dir_x;
	private float trans_dir_y;
	private float trans_dir_z;
	private float trans_speed_x;
	private float trans_speed_y;
	private float trans_speed_z;
	
	private float sharkPotentialScalar;
	private float wallPotentialScalar;
	private float foodPotentialScalar;
	
	public float boundingSphereRadius;
	private boolean showBoundingSphere;
	
	private Shark predator = null;
	
	private Coord color;

	private Random rand;
	
	private Vivarium v;

	public Fish(float _x, float _y, float _z, float _scale, float _tail_speed, Vivarium _v) {
		glut = new GLUT();
		rand = new Random();
		x = last_x = _x;
		y = last_y = _y;
		z = last_z = _z;
		fish_obj = tail_obj = body_obj = 0;
		scale = _scale;
		tail_speed = _tail_speed;
		tail_angle = body_angle = 0;
		tail_direction = 1;
		body_speed = tail_speed / 4;
		trans_dir_x = rand.nextFloat();
		trans_dir_y = rand.nextFloat();
		trans_dir_z = rand.nextFloat();
		trans_speed_x = 0.005f;
		trans_speed_y = 0.005f;
		trans_speed_z = 0.005f;
		
		sharkPotentialScalar = 0.25f;
		wallPotentialScalar = 0.1f;
		foodPotentialScalar = -0.2f;
		
		boundingSphereRadius = 0.35f;
		showBoundingSphere = false;
		
		v = _v;
		
		color = new Coord(0.85f, 0.55f, 0.20f);
		dead = false;
	}

	public void init(GL2 gl) {
		createBody(gl);
		createTail(gl);
		if (showBoundingSphere) boundingSphere(gl);
		fish_obj = gl.glGenLists(1);
		gl.glNewList(fish_obj, GL2.GL_COMPILE);
		const_disp_list(gl);
		gl.glEndList();
	}

	public void draw(GL2 gl) {
		gl.glPushMatrix();
		gl.glPushAttrib(gl.GL_CURRENT_BIT);
		gl.glColor3f( (float)color.x, (float)color.y, (float)color.z); // Orange
		gl.glCallList(fish_obj);
		gl.glPopAttrib();
		gl.glPopMatrix();
	}

	public void update(GL2 gl) {
		calcDistances(gl);
		calcPotential();
		translate();
		if (!dead) moveTailAndBody();
		gl.glNewList(fish_obj, GL2.GL_COMPILE);
		const_disp_list(gl);
		gl.glEndList();
	}

	private void const_disp_list(GL2 gl) {
		gl.glPushMatrix();

		// gl.glTranslatef(x, y, z);
		
		// Find a rotation matrix that points object in direction of movement
		float dx = last_x - x;
		// TODO this dy is not working... its messing up the fish for some
		// reason
		//float dy = last_y - y;
		float dy = 0.0f;
		float dz = last_z - z;

		float mag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float[] v = new float[3];
		v[0] = dx / mag;
		v[1] = dy / mag;
		v[2] = dz / mag;

		// up vector
		float[] up = { 0.0f, 1.0f, 0.0f };

		float[] left = { v[1] * up[2] - up[1] * v[2],
				v[0] * up[2] - up[0] * v[2], v[0] * up[1] - up[0] * v[1] };
		// normalize
		mag = (float) Math.sqrt(left[0] * left[0] + left[1] * left[1] + left[2]
				* left[2]);
		left[0] = left[0] / mag;
		left[1] = left[1] / mag;
		left[2] = left[2] / mag;

		// perpendicular up
		float[] perpUp = { left[1] * v[2] - v[1] * left[2],
				left[0] * v[2] - v[0] * left[2],
				left[0] * v[1] - v[0] * left[1] };
		// normalize
		mag = (float) Math.sqrt(perpUp[0] * perpUp[0] + perpUp[1] * perpUp[1]
				+ perpUp[2] * perpUp[2]);
		perpUp[0] = perpUp[0] / mag;
		perpUp[1] = perpUp[1] / mag;
		perpUp[2] = perpUp[2] / mag;

		float[] rotationMatrix = { left[0], left[1], left[2], 0.0f, perpUp[0],
				perpUp[1], perpUp[2], 0.0f, v[0], v[1], v[2], 0.0f, x, y, z,
				1.0f };
		gl.glMultMatrixf(rotationMatrix, 0);
		
		if (dead) {
			gl.glRotatef(-90, 0, 0, 1);
		}
		
		// Rotate tail
		gl.glPushMatrix();
		gl.glRotatef(tail_angle, 0, 1, 0);
		gl.glCallList(tail_obj);
		gl.glPopMatrix();
		
		// Rotate body
		gl.glPushMatrix();
		gl.glRotatef(body_angle, 0, 1, 0);
		gl.glCallList(body_obj);
		gl.glPopMatrix();
		
		// If showing bounding sphere, add to fish_obj display list
		gl.glPushMatrix();
		gl.glCallList(box_obj);
		gl.glPopMatrix();

		gl.glPopMatrix();
	}
	
	// Function that draws a bounding sphere for debugging purposes
	private void boundingSphere(GL2 gl) {
		box_obj=gl.glGenLists(1);
		gl.glNewList(box_obj, GL2.GL_COMPILE);
		gl.glPushMatrix();
		glut.glutWireSphere(0.35, 36, 24);
		gl.glPopMatrix();
		gl.glEndList();
	}
	
	private void createBody(GL2 gl) {
		// body
		body_obj = gl.glGenLists(1);
		gl.glNewList(body_obj, GL2.GL_COMPILE);
		gl.glPushMatrix();
		gl.glScalef(0.4f, 0.6f, 1);
		gl.glTranslatef(0, 0, -0.09f);
		glut.glutSolidSphere(0.2, 36, 24);
		gl.glPopMatrix();
		gl.glEndList();
	}

	private void createTail(GL2 gl) {
		// tail
		tail_obj = gl.glGenLists(1);
		gl.glNewList(tail_obj, GL2.GL_COMPILE);
		gl.glPushMatrix();
		gl.glScalef(0.5f, 1, 1);
		gl.glTranslatef(0, 0, 0.35f);
		gl.glRotatef(-180, 0, 1, 0);
		glut.glutSolidCone(0.1, 0.35, 20, 20);
		int circle_points = 100;
		// create end cap, adapted from OpenGL red book example 2-4 on drawing
		// circles
		gl.glBegin(gl.GL_POLYGON);
		double angle;
		for (int i = 0; i < circle_points; i++) {
			angle = 2 * Math.PI * i / circle_points;
			gl.glVertex2f((float) Math.cos(angle) * 0.1f,
					(float) Math.sin(angle) * 0.1f);
		}
		gl.glEnd();
		gl.glPopMatrix();
		gl.glEndList();
	}

	private void moveTailAndBody() {
		tail_angle += tail_speed * tail_direction;
		body_angle += body_speed * tail_direction * -1;

		if (tail_angle > 10 || tail_angle < -10) {
			tail_direction *= -1;
		}
	}
	
	// Uses an exponential potential function to avoid walls, sharks, and go after food
	private void calcPotential() {
		Coord p = new Coord(x,y,z);
		Coord q1 = new Coord(predator.x, predator.y, predator.z);
		Coord q2 = new Coord(3.6, y, z);
		Coord q3 = new Coord(-3.6, y, z);
		Coord q4 = new Coord(x, 1.9, z);
		Coord q5 = new Coord(x, -1.9, z);
		Coord q6 = new Coord(x, y, 1.9);
		Coord q7 = new Coord(x, y, -1.9);
		Coord[] coords = {potentialFunction(p,q1,sharkPotentialScalar), potentialFunction(p,q2, wallPotentialScalar), 
				potentialFunction(p,q3,wallPotentialScalar), potentialFunction(p,q4,wallPotentialScalar),
				potentialFunction(p,q5,wallPotentialScalar), potentialFunction(p,q6,wallPotentialScalar), 
				potentialFunction(p,q7,wallPotentialScalar)};
		Coord sum = add(coords);
		for (Food f : v.foodlist) {
			Coord qi = new Coord(f.x, f.y, f.z);
			qi = potentialFunction(p, qi, foodPotentialScalar);
			Coord[] m = {sum, qi};
			sum = add(m);
		}
		trans_dir_x += sum.x;
		trans_dir_y += sum.y;
		trans_dir_z += sum.z;
		
	}	
	
	// Exponential potential function
	private Coord potentialFunction(Coord p, Coord q1, float scale) {
		float x = (float) (scale*(p.x - q1.x)*Math.pow(Math.E,-1*(Math.pow((p.x-q1.x), 2) + Math.pow((p.y-q1.y), 2) + Math.pow((p.z-q1.z), 2)) ));
		float y = (float) (scale*(p.y - q1.y)*Math.pow(Math.E,-1*(Math.pow((p.x-q1.x), 2) + Math.pow((p.y-q1.y), 2) + Math.pow((p.z-q1.z), 2)) ));
		float z = (float) (scale*(p.z - q1.z)*Math.pow(Math.E,-1*(Math.pow((p.x-q1.x), 2) + Math.pow((p.y-q1.y), 2) + Math.pow((p.z-q1.z), 2)) ));
		Coord potential = new Coord(x, y, z);
		return potential;
	}
	
	// Computes distances between food and sharks
	// If collision with shark, "dies" and floats to top of tank
	private void calcDistances(GL2 gl){
		Coord a = new Coord(x,y,z);
		// food
		for (Food f : v.foodlist) {
			Coord b = new Coord(f.x,f.y,f.z);
			if (distance(a, b) < 0.5) {
				gl.glDeleteLists(f.food_obj, 1);
				f.eaten();
			}
		}
		
		Coord shark = new Coord(predator.x*predator.scale, predator.y*predator.scale, predator.z*predator.scale);
		if (distance(a, shark) < predator.boundingSphereRadius) {
			color = new Coord(1,1,1);
			predator.removePrey();
			dead = true;
		}
	}
	
	// Move the fish around the tank using a combination of potential functions
	// and flipping directions when about to leave tank.
	private void translate() {
		if (dead) {
			if (y < 1.9) {
				y += trans_speed_y;
			}
		} else {
		last_x = x;
		last_y = y;
		last_z = z;
		x += trans_speed_x * trans_dir_x;
		y += trans_speed_y * trans_dir_y;
		z += trans_speed_z * trans_dir_z;
		
		float n = rand.nextFloat();
		while (n < 0.2f) {
			n = rand.nextFloat();
		}
		if (x > 3.6 || x < -3.6) {
			// if n is not large enough to pull it below the 
			// constraints of the tank, it will get stuck, so
			// I am setting x at the constraint so it won't get
			// stuck.
			x = 3.6f;
			if (trans_dir_x > 0){
				trans_dir_x = -1 * n;
			} else {
				x *= -1;
				trans_dir_x = n;
			}
		}
		if (y > 1.9 || y < -1.9) {
			y = 1.9f;
			if (trans_dir_y > 0){
				trans_dir_y = -1 * n;
			} else {
				y *= -1;
				trans_dir_y = n;
			}
		}
		if (z > 1.9 || z < -1.9) {
			z = 1.9f;
			if (trans_dir_z > 0){
				trans_dir_z = -1 * n;
			} else {
				z*= -1;
				trans_dir_z = n;
			}
		}
		}

	}
	
	// Coord helper functions
	private Coord add(Coord a, Coord b) {
		a.x += b.x;
		a.y += b.y;
		a.z += b.z;
		return a;
	}
	
	private Coord add(Coord[] b) {
		Coord ret = new Coord();
		for (Coord a : b) {
			ret.x += a.x;
			ret.y += a.y;
			ret.z += a.z;
		}
		return ret;
	}
	
	private float distance(Coord a, Coord b) {
		return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2) + Math.pow(a.z - b.z, 2));
	}
	
	// To access shark location
	public void addPredator(Shark s) {
		predator = s;
	}

}


