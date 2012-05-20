package ca.kess.sim;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class Simulation implements ApplicationListener {
	private Body[] mBodies;
	private OrthographicCamera mCamera;
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		new LwjglApplication(new Simulation(), "N-Body Simulation", 1200, 800, false);
	}

	private static final int NUM_THREADS = 8;
	private static final int BLOCK_SIZE = 64;
	private static final int NUM_BODIES = 32;
	
	private int mHeaviestBodyIndex = 0;
	@Override
	public void create() {
		mBodies = new Body[NUM_BODIES];
		Random r = new Random();
		
		//Fixed body generation code
		
		mBodies[0] = new Body(this, "Sun", 1000000, 1.0, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, 0, 0, true, Color.YELLOW);
		
		for(int i=1; i < NUM_BODIES; ++i) {
			mBodies[i] = new Body(this, "Planet" + i, 1 + 50000.0* r.nextDouble(), 1.0, mBodies[0].x() + 100 + r.nextInt(600), mBodies[0].y(), 0.0, 0.0, false, Color.RED);
			mBodies[i].vy(mBodies[i].getOrbitalSpeed(mBodies[0])*1.5);
		}
		
		//Random bodies
		/*
		double heaviest = 0.0;
		for(int i=0; i < NUM_BODIES; ++i) {
			if(r.nextDouble() < .1) {
				mBodies[i] = new Repulsor(this, "Planet" + i, 10000 + r.nextInt(300000), 1.0, Gdx.graphics.getWidth()/2 + 200 - r.nextInt(400), Gdx.graphics.getHeight()/2 + 200 - r.nextInt(400), -50 + r.nextInt(100), -50 + r.nextInt(100), false, Color.RED);
			} else {
				mBodies[i] = new Body(this, "Planet" + i, 10000 + r.nextInt(300000), 1.0, Gdx.graphics.getWidth()/2 + 200 - r.nextInt(400), Gdx.graphics.getHeight()/2 + 200 - r.nextInt(400), -50 + r.nextInt(100), -50 + r.nextInt(100), false, Color.BLUE);
			}
			if(mBodies[i].mass() > heaviest) {
				heaviest = mBodies[i].mass();
				mHeaviestBodyIndex = i;
			
			}
		}
		*/
		mBodies[mHeaviestBodyIndex].mass(3*mBodies[mHeaviestBodyIndex].mass());
		Gdx.app.log("Simulation", "Created " + NUM_BODIES + " bodies");
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	private void resetBodies() {
		for(Body body : mBodies) {
			body.update(Math.min(Gdx.graphics.getDeltaTime(), 1.0f/60.0f));
			body.resetForces();
		}
	}
	
	private void updateSimulation() throws InterruptedException, ExecutionException {
		//resetBodies();
		//calculateTriangle2();
		updateSimulationRecursive();
		
	}
	
	@SuppressWarnings("unused")
	private void calculateTriangle2() {
		//Gdx.app.log("Simulation", "calculateTriangle(" + lower + ", " + upper + ")");
		for(int j = 1; j < NUM_BODIES; ++j) {
			mBodies[0].addGravitationalForcesPairwise(mBodies[j]);
		}
	}
	
	private void calculateTriangle(int lower, int upper) {
		//Gdx.app.log("Simulation", "calculateTriangle(" + lower + ", " + upper + ")");
		for(int i=lower; i < upper -1; ++i) {
			for(int j = i + 1; j < upper; ++j) {
				mBodies[i].addGravitationalForcesPairwise(mBodies[j]);
			}
		}
	}
	
	private void calculateBlock(int leftLower, int leftUpper, int rightLower, int rightUpper) {
		//Gdx.app.log("Simulation", "calculateBlock(" + leftLower + ", " + leftUpper + ", " + rightLower + ", " + rightUpper + ")");
		for(int i=leftLower; i < leftUpper; ++i) {
			for(int j=rightLower; j < rightUpper; ++j) {
				mBodies[i].addGravitationalForcesPairwise(mBodies[j]);
			}
		}
	}
	
	
	
	private void updateSimulationRecursive() throws InterruptedException, ExecutionException {
		resetBodies();	
		calculateGravity(0, mBodies.length, NUM_THREADS);
	}
	
	private void calculateGravity(final int lower, final int upper, final int threadCount) throws InterruptedException, ExecutionException {
		//Gdx.app.log("Simulation", "CalculateGravity(" + lower + ", " + upper + ", " + availableThreads + ")");
		final int count = upper - lower;
		if(count <= BLOCK_SIZE) {
			calculateTriangle(lower, upper);
		} else {
			if(threadCount <= 1) {
				calculateGravity(lower, lower + count/2, threadCount);
				calculateGravity(lower + count/2, upper, threadCount);
			} else {
				
				final Thread th = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							calculateGravity(lower, lower + count/2, threadCount-1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				});
				
				th.start();
				calculateGravity(lower + count/2, upper, threadCount -1);
				th.join();
			}
			calculateBlock(lower, lower + count/2, lower + count/2, upper);
			
		}
	}
	
	double timeSinceLastLog = 0.0f;
	double numFrames = 0;
	@Override
	public void render() {
		timeSinceLastLog += Gdx.graphics.getDeltaTime();
		++numFrames;
		if(timeSinceLastLog > 2) {
			timeSinceLastLog -= 2;
			Gdx.app.log("Sim", "FPS: " + numFrames / 2.0f);
			Gdx.graphics.setTitle("Simulation [" + (numFrames/2.0) + " fps]");
			numFrames = 0;
		}
		
		try {
			updateSimulation();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		//float cameraX = (int) (mBodies[mHeaviestBodyIndex].x() - Gdx.graphics.getWidth() / 2);
		//float cameraY = (int) (mBodies[mHeaviestBodyIndex].y() - Gdx.graphics.getHeight() / 2);
		
		//mCamera.translate(cameraX, cameraY, 0);
		mCamera.update();
		mCamera.apply(Gdx.gl10);
		//mCamera.translate(-cameraX, -cameraY, 0);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		Gdx.graphics.getGL10().glEnable(GL10.GL_TEXTURE_2D);
		Gdx.graphics.getGL10().glEnable(GL10.GL_BLEND);
		
		for(Body body : mBodies) {
			body.render();
		}
	}

	private int mHeight = 1;
	private int mWidth = 1;
	private final double BOUNCE_DAMPING = 0.5;
	
	public double getBounceDamping() { return BOUNCE_DAMPING; }
	
	@Override
	public void resize(int w, int h) {
		mCamera = new OrthographicCamera(w, h);
		mCamera.position.set(w/2, h/2, 0);
		mHeight = h;
		mWidth = w;
	}
	
	public int getWidth() { return mWidth; }
	public int getHeight() { return mHeight; }

	
	@Override
	public void resume() {
		
	}
}
