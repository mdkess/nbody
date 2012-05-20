package ca.kess.sim;

import com.badlogic.gdx.graphics.Color;

public class Repulsor extends Body {
	
	public Repulsor(Simulation sim, String name, double mass, double density, double x,
			double y, double vx, double vy, boolean fixed, Color color) {
		super(sim, name, mass, density, x, y, vx, vy, fixed, color);
	}
	
	
	@Override
	public void addGravitationalForcesPairwise(Body other) {
		double dx = other.x() - x();
		double dy = other.y() - y();
		double r2 = dx * dx + dy * dy;
		r2 = Math.max(r2, Body.MIN_DISTANCE); //prevent bodies from flying apart.
		double r = Math.sqrt(r2);
		
		
		double force = G * other.mass() * mass() / r2;
		
		double ratio = force / r;
		
		//Force will be in the direction of "other";
		 
		double fx = dx * ratio;
		double fy = dy * ratio;

		applyForce(-fx, -fy);
		
		other.applyForce(fx, fy);
	}
}
