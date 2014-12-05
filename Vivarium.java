/*
 * Vivarium.java
 * 
 * William Kranich - wkranich@bu.edu
 * 
 * Class containing all objects inside the Vivarium, 
 * including a Tank, Fish, Shark, and an ArrayList of Food.
 * 
 * Used from supplied assignment template
 * 
 */

import javax.media.opengl.*;
import com.jogamp.opengl.util.*;
import java.util.*;

public class Vivarium
{
  private Tank tank;
  public Fish fish;
  public Shark shark;
  public ArrayList<Food> foodlist;
  private boolean foodadded;
  private boolean fishAdded;

  public Vivarium()
  {
    tank = new Tank( 8.0f, 4.0f, 4.0f );
    fish = new Fish(0, 0, 0, 0.8f, 0.5f, this);
    shark = new Shark(-2, 0, -1, 1.3f, 0.5f, this);
    foodlist = new ArrayList<Food>();
    foodadded = false;
    fishAdded = false;
  }

  public void init( GL2 gl )
  {
    tank.init( gl );
    fish.init( gl );
    shark.init( gl );
    for (Food food : foodlist) {
    	food.init( gl );
    }
    
    fish.addPredator(shark);
    shark.addPrey(fish);
  }

  public void update( GL2 gl )
  {
    tank.update( gl );
    
    // Need to reinitialize fish if has been reset after program starts
    if (fishAdded) {
    	fish.init(gl);
    	fishAdded = false;
    } else {
    	fish.update( gl );
    }
    
    shark.update( gl );
    
    // Need to initalize food that has been added by pressing F/f
    if (foodadded) {
    	for (Food food : foodlist) {
        	food.init( gl );
        }
    	foodadded = false;
    }
    
    for (Food food : foodlist) {
    	food.update( gl );
    }
    
    // For concurrency issues, removes food that has been eaten
    for (ListIterator<Food> iter = this.foodlist.listIterator(); iter.hasNext();) {  // food is your ArrayList
        Food f = iter.next();
        if (f.isEaten()) {
          iter.remove();
        }
      }
  }

  public void draw( GL2 gl )
  {
    tank.draw( gl );
    fish.draw( gl );
    shark.draw( gl );
    for (Food food : foodlist) {
    	food.draw( gl );
    }
  }
  
  // Add new food to tank
  public void addFood() {
	  Food f = new Food();
	  foodlist.add(f);
	  foodadded=true;
  }
  
  // Reset fish
  public void newFish() {
	  fish = new Fish(0, 0, 0, 0.8f, 0.5f, this);
	  fishAdded = true;
	  shark.addPrey(fish);
	  fish.addPredator(shark);
  }
}
