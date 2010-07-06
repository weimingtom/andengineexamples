package org.anddev.andengine.examples;

import java.util.ArrayList;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.Scene;
import org.anddev.andengine.entity.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.handler.runnable.RunnableHandler;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.entity.PhysicsConnector;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;

import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * @author Nicolas Gramlich
 * @since 18:47:08 - 19.03.2010
 */
public class PhysicsExample extends BaseExample implements IAccelerometerListener, IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	// ===========================================================
	// Fields
	// ===========================================================

	private Texture mTexture;

	private TiledTextureRegion mBoxFaceTextureRegion;
	private TiledTextureRegion mCircleFaceTextureRegion;

	private World mPhysicsWorld;
	private int mFaceCount = 0;

	/** our ground box **/
	private Body groundBody;

	/** our boxes **/
	private final ArrayList<Body> boxes = new ArrayList<Body>();

	private final RunnableHandler mRunnableHandler = new RunnableHandler();

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public Engine onLoadEngine() {
		Toast.makeText(this, "Touch the screen to add objects.", Toast.LENGTH_LONG).show();
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera, false));
	}

	@Override
	public void onLoadResources() {
		this.mTexture = new Texture(64, 64, TextureOptions.BILINEAR);
		TextureRegionFactory.setAssetBasePath("gfx/");
		this.mBoxFaceTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "boxface_tiled.png", 0, 0, 2, 1); // 64x32
		this.mCircleFaceTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "circleface_tiled.png", 0, 32, 2, 1); // 64x32
		this.mEngine.getTextureManager().loadTexture(this.mTexture);

		this.enableAccelerometerSensor(this);
	}

	@Override
	public Scene onLoadScene() {
		this.mEngine.registerPostFrameHandler(new FPSLogger());

		// we instantiate a new World with a proper gravity vector
		// and tell it to sleep when possible.
		this.mPhysicsWorld = new World(new Vector2(0, 2 * SensorManager.GRAVITY_EARTH), true);

		// next we create the body for the ground platform. It's
		// simply a static body.
		final BodyDef groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyType.StaticBody;
		this.groundBody = this.mPhysicsWorld.createBody(groundBodyDef);

		// next we create a static ground platform. This platform
		// is not moveable and will not react to any influences from
		// outside. It will however influence other bodies. First we
		// create a PolygonShape that holds the form of the platform.
		// it will be 100 meters wide and 2 meters high, centered
		// around the origin
		final PolygonShape groundPoly = new PolygonShape();
		groundPoly.setAsBox(50, 1);

		// finally we add a fixture to the body using the polygon
		// defined above. Note that we have to dispose PolygonShapes
		// and CircleShapes once they are no longer used. This is the
		// only time you have to care explicitely for memomry managment.
		this.groundBody.createFixture(groundPoly, 10);
		groundPoly.dispose();

		final Scene scene = new Scene(2);
		scene.setBackgroundColor(0, 0, 0);
		scene.setOnSceneTouchListener(this);

		scene.registerPreFrameHandler(this.mPhysicsWorld);
		scene.registerPreFrameHandler(this.mRunnableHandler);

		return scene;
	}

	private void addFace(final float pX, final float pY) {
		this.mFaceCount++;

		final AnimatedSprite face;

		face = new AnimatedSprite(pX, pY, this.mBoxFaceTextureRegion);

		final BodyDef boxBodyDef = new BodyDef();
		boxBodyDef.type = BodyType.DynamicBody;
		boxBodyDef.position.x = pX;
		boxBodyDef.position.y = pY;

		final Body boxBody = this.mPhysicsWorld.createBody(boxBodyDef);

		final PolygonShape boxPoly = new PolygonShape();
		boxPoly.setAsBox(16, 16);
		boxBody.createFixture(boxPoly, 10);

		// add the box to our list of boxes
		this.boxes.add(boxBody);

		boxPoly.dispose();

		final Scene scene = this.mEngine.getScene();
		face.animate(new long[] { 200, 200 }, 0, 1, true);
		scene.getTopLayer().addEntity(face);
		scene.registerPreFrameHandler(new PhysicsConnector(face, boxBody));
	}

	public void onLoadComplete() {

	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final MotionEvent pSceneMotionEvent) {
		if(this.mPhysicsWorld != null) {
			if(pSceneMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				this.mRunnableHandler.postRunnable(new Runnable() {
					@Override
					public void run() {
						PhysicsExample.this.addFace(pSceneMotionEvent.getX(), pSceneMotionEvent.getY());
					}
				});
				return true;
			}
		}
		return false;
	}

	@Override
	public void onAccelerometerChanged(final AccelerometerData pAccelerometerData) {
		this.mPhysicsWorld.setGravity(new Vector2(4 * pAccelerometerData.getY(), 4 * pAccelerometerData.getX()));
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
