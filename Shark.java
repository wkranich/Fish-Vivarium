/*
 * Shark.java
 * 
 * William Kranich - wkranich@bu.edu
 * 
 * Object for an OpenGL shark that moves along 3 axes of a tank,
 * attacks fish and "eats" food, but doesn't go after food.
 */

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.gl2.GLUT;

import java.util.*;

public class Shark {
	private GLUT glut;
	public float x, y, z, last_x, last_y, last_z;
	private int shark_obj;
	private int tail_obj;
	private int body_obj;
	private int fin_obj;
	private int box_obj;

	public float scale;

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
	private float fishPotentialScalar;
	private float wallPotentialScalar;
	
	public float boundingSphereRadius;
	private boolean showBoundingSphere;

	private Random rand;
	
	private Vivarium v;
	
	private Fish prey = null;

	public Shark(float _x, float _y, float _z, float _scale, float _tail_speed, Vivarium _v) {
		glut = new GLUT();
		rand = new Random();
		x = last_x = _x;
		y = last_y = _y;
		z = last_z = _z;
		shark_obj = tail_obj = body_obj = 0;
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

		boundingSphereRadius = 0.35f * scale;
		showBoundingSphere = false;
		fishPotentialScalar = -0.17f;
		wallPotentialScalar = 0.05f;
		
		v = _v;
	}

	public void init(GL2 gl) {
		createBody(gl);
		createTail(gl);
		createFin(gl);
		if (showBoundingSphere) boundingSphere(gl);
		shark_obj = gl.glGenLists(1);
		gl.glNewList(shark_obj, GL2.GL_COMPILE);
		const_disp_list(gl);
		gl.glEndList();
	}

	public void draw(GL2 gl) {
		gl.glPushMatrix();
		gl.glPushAttrib(gl.GL_CURRENT_BIT);
		gl.glColor3f(0.453125f, 0.546875f, 0.625f); // Shark blue
		gl.glCallList(shark_obj);
		gl.glPopAttrib();
		gl.glPopMatrix();
	}

	public void update(GL2 gl) {
		calcPotential();
		calcDistances(gl);
		translate();
		moveTailAndBody();
		gl.glNewList(shark_obj, GL2.GL_COMPILE);
		const_disp_list(gl);
		gl.glEndList();
	}

	private void const_disp_list(GL2 gl) {
		gl.glPushMatrix();

		// gl.glTranslatef(x, y, z);
		gl.glScalef(scale, scale, scale);

		float dx = last_x - x;
		// TODO this dy is not working... its messing up the fish for some
		// reason
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
		float[] perpUp = { v[1] * left[2] - left[1] * v[2],
				v[0] * left[2] - left[0] * v[2],
				v[0] * left[1] - left[0] * v[1] };
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

		// Draw top dorsal fin
		gl.glPushMatrix();
		gl.glCallList(fin_obj);
		gl.glPopMatrix();
		
		// Show bounding sphere for debugging
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

	private void createFin(GL2 gl) {
		// fin
		fin_obj = gl.glGenLists(1);
		gl.glNewList(fin_obj, GL2.GL_COMPILE);
		gl.glPushMatrix();
		gl.glScalef(0.5f, 1, 1);
		gl.glTranslatef(0, 0, -0.1f);
		gl.glRotatef(-75, 1, 0, 0);
		glut.glutSolidCone(0.1, 0.27, 20, 20);
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
	
	// Computes distances between food and itself
	private void calcDistances(GL2 gl) {
		Coord a = new Coord(x*scale,y*scale,z*scale);
		// food
		for (Food f : v.foodlist) {
			Coord b = new Coord(f.x,f.y,f.z);
			if (distance(a, b) < 0.5) {
				gl.glDeleteLists(f.food_obj, 1);
				f.eaten();
			}
		}
	}
	
	// Uses an exponential potential function to avoid walls and go after fish
	private void calcPotential() {
		Coord p = new Coord(x,y,z);
		Coord q1;
		if (prey != null) q1 = new Coord(prey.x, prey.y, prey.z);
		else q1 = new Coord(10000,100000,100000);
		Coord q2 = new Coord(3.6, y, z);
		Coord q3 = new Coord(-3.6, y, z);
		Coord q4 = new Coord(x, 1.9, z);
		Coord q5 = new Coord(x, -1.9, z);
		Coord q6 = new Coord(x, y, 1.9);
		Coord q7 = new Coord(x, y, -1.9);
		Coord[] coords = {potentialFunction(p,q1,fishPotentialScalar), potentialFunction(p,q2, wallPotentialScalar), 
				potentialFunction(p,q3,wallPotentialScalar), potentialFunction(p,q4,wallPotentialScalar),
				potentialFunction(p,q5,wallPotentialScalar), potentialFunction(p,q6,wallPotentialScalar), 
				potentialFunction(p,q7,wallPotentialScalar)};
		Coord sum = add(coords);
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
	
	// Move the fish around the tank using a combination of potential functions
	// and flipping directions when about to leave tank.
	private void translate() {
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
		// since I copied this from fish I'm dividing
		// the boundaries by the scale factor for the shark
		// to keep it inside the tank
		if (x > 3.6 / scale || x < -3.6 / scale) {
			// if n is not large enough to pull it below the
			// constraints of the tank, it will get stuck, so
			// I am setting x at the constraint so it won't get
			// stuck.
			x = 3.6f / scale;
			if (trans_dir_x > 0) {
				trans_dir_x = -1 * n;
			} else {
				x *= -1;
				trans_dir_x = n;
			}
		}
		if (y > 1.8 / scale || y < -1.8 / scale) {
			y = 1.8f / scale;
			if (trans_dir_y > 0) {
				trans_dir_y = -1 * n;
			} else {
				y *= -1;
				trans_dir_y = n;
			}
		}
		if (z > 1.8 / scale || z < -1.8 / scale) {
			z = 1.8f / scale;
			if (trans_dir_z > 0) {
				trans_dir_z = -1 * n;
			} else {
				z *= -1;
				trans_dir_z = n;
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
		
		// To keep track of fish coordinates
		public void addPrey(Fish f) {
			prey = f;
		}
		
		// If fish has been eaten, stop attacking
		public void removePrey() {
			prey = null;
		}

}
