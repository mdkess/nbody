package ca.kess.sim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class Body {
	public static final double MIN_DISTANCE = 20 * 20;	
	
	//private static double G = 6.6738480 * Math.pow(10.0, -11);
	public static final double G = 1.0;
	public static double sSizeFactor = 0.1;
	private Mesh mMesh;
	
	private Simulation mSimulation;

	private String mName;
	public String getName() { return mName; }
	private double mDensity;
	public double getDensity() { return mDensity; }
	private double mMass;
	private double mForceX, mForceY;
	private double mVelocityX, mVelocityY;
	private double mPositionX, mPositionY;
	private double mRadius;
	private boolean mFixed;
	
	private Texture mTexture;
	
	public double distanceTo(Body other) {
		double dx = other.x() - x();
		double dy = other.y() - y();
		return Math.sqrt(dx * dx + dy * dy);		
	}
	
	public double getOrbitalSpeed(Body other) {
		double numerator = other.mass() * other.mass() * G;
		double denominator = (mMass + other.mass()) * distanceTo(other);
		return Math.sqrt(numerator/denominator);
	}
	
	public Body(Simulation sim, String name, double mass, double density, double x, double y, double vx, double vy, boolean fixed, Color color) {
		mSimulation = sim;
		mFixed = fixed;
		mName = name;
		mMass = mass;
		mDensity = density;
		mPositionX = x;
		mPositionY = y;
		mVelocityX = vx;
		mVelocityY = vy;
		mForceX = 0.0f;
		mForceY = 0.0f;
		
		mRadius = Math.max(Math.sqrt((mass/density)/Math.PI) * sSizeFactor, 2.0);
		
		mMesh = new Mesh(true, 4, 4, new VertexAttribute(Usage.Position, 3, "a_position"), new VertexAttribute(Usage.ColorPacked, 4, "a_color"),
				new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoords"));
		
		mMesh.setVertices(new float[] { -0.5f, -0.5f, 0.0f, color.toFloatBits(), 0, 0,
		                                 0.5f, -0.5f, 0.0f, color.toFloatBits(), 1, 0,
		                                -0.5f,  0.5f, 0.0f, color.toFloatBits(), 0, 1,
		                                 0.5f,  0.5f, 0.0f, color.toFloatBits(), 1, 1,
		                                });

		mMesh.setIndices(new short[] {0, 1, 2, 3});
		mMesh.scale((float)mRadius, (float)mRadius, 1.0f);
		FileHandle imageFileHandle = Gdx.files.internal("data/circle128x128.png"); 
        mTexture = new Texture(imageFileHandle);
	}
	
	public void render() {
		GL10 gl = Gdx.graphics.getGL10();
		gl.glPushMatrix();
		gl.glTranslatef((int)x(), (int)y(), 0);
		mTexture.bind();
		mMesh.render(GL10.GL_TRIANGLE_STRIP, 0, 4);
		//mLineMesh.setVertices()
		
		
		gl.glPopMatrix();
	}
	
	public void update(float dt) {
		if(mFixed) { return; }
		//Compute acceleration with F = ma -> a = F/m
		
		double aX = mForceX / mMass;
		double aY = mForceY / mMass;
		
		mVelocityX += dt * aX;
		mVelocityY += dt * aY;
		
		mPositionX += dt * mVelocityX;
		if(mPositionX + mRadius > mSimulation.getWidth()) {
			mPositionX = mSimulation.getWidth() - mRadius;
			mVelocityX = - mVelocityX * mSimulation.getBounceDamping();
		} else if(mPositionX -mRadius < 0) {
			mPositionX = mRadius;
			mVelocityX = - mVelocityX * mSimulation.getBounceDamping();
		}
		
		mPositionY += dt * mVelocityY;
		if(mPositionY + mRadius> mSimulation.getHeight()) {
			mPositionY = mSimulation.getHeight() - mRadius;
			mVelocityY = - mVelocityY * mSimulation.getBounceDamping();
		} else if(mPositionY - mRadius < 0) {
			mPositionY = mRadius;
			mVelocityY = - mVelocityY * mSimulation.getBounceDamping();
		}
	}
	
	public double x() {
		return mPositionX;
	}
	public double y() {
		return mPositionY;
	}
	public double vx() {
		return mVelocityX;
	}
	public void vx(double vx) {
		mVelocityX = vx;
	}
	public double vy() {
		return mVelocityY;
	}
	public void vy(double vy) {
		mVelocityY = vy;
	}
	
	public double mass() {
		return mMass;
	}
	public void mass(double mass) {
		mMass = mass;
	}
	
	public double radius() { return mRadius; }
	
	/**
	 * The force of gravity between two bodies is:
	 * F1 = F2 = G * m1 * m2 / r*r
	 */
	public void addGravitationalForcesPairwise(Body other) {
		double dx = other.x() - x();
		double dy = other.y() - y();
		double r2 = dx * dx + dy * dy;
		r2 = Math.max(r2, MIN_DISTANCE); //prevent bodies from flying apart.
		double r = Math.sqrt(r2);
		
		
		double force = G * other.mass() * mass() / r2;
		
		double ratio = force / r;
		
		//Force will be in the direction of "other";
		 
		double fx = dx * ratio;
		double fy = dy * ratio;

		applyForce(fx, fy);
		
		other.applyForce(-fx, -fy);
	}
	
	public void applyForce(double fx, double fy) {
		mForceX += fx;
		mForceY += fy;
	}
	
	public void resetForces() {
		mForceX = mForceY = 0.0f;
	}
}
