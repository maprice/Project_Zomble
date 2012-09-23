package com.maprice.zomble;

import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import com.maprice.zomble.util.contants.ZombleConstants;


/**
 * @author mprice
 */

public class ZombleCharacterEntity extends Entity implements ZombleConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	static final String LOG_TAG = PREFIX + ZombleCharacterEntity.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	final private AnimatedSprite mCharacterSprite;
	final private AnimatedSprite mItemSprite;
	final private PhysicsHandler mPhysicsHandler;

	final private long[] mDuration = new long[CHARACTER_SPRITE_SHEET_COL-1];

	private int mCharacterAngle = 0;
	private int mItemAngle = 0; 
	private boolean backwards = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	public ZombleCharacterEntity(float pX, float pY,
			TiledTextureRegion characterTextureRegion,
			TiledTextureRegion itemTextureRegion,
			VertexBufferObjectManager vertexBufferObjectManager) 
	{

		super(pX,pY);

		// Setup the animation duration array
		for(int i = 0; i < mDuration.length; i++)  mDuration[i] = FRAME_SPEED;

		// Setup sprites
		mCharacterSprite = new AnimatedSprite(0, 0, characterTextureRegion, vertexBufferObjectManager);
		mItemSprite = new AnimatedSprite(0, 0, itemTextureRegion, vertexBufferObjectManager);
		mPhysicsHandler = new PhysicsHandler(this);
		mCharacterSprite.setX(4.5f);

		// Set scale
		this.setScale(SCALE);

		// Add sprites to entity
		this.attachChild(mCharacterSprite);
		this.attachChild(mItemSprite);
		this.registerUpdateHandler(mPhysicsHandler);
	}

	// ===========================================================
	// Methods
	// ===========================================================


	/**
	 * Moves the character in the appropriate direction and animates the sprite accordingly
	 * 
	 * @param pValueX
	 *            x value of the analog control
	 *            
	 * @param pValueY
	 *            y value of the analog control
	 */
	public void walk(float pValueX, float pValueY){

		double angle = this.getAngle(pValueX, pValueY);
		// Convert angle from degrees to 8-way directional coordinates
		int walkAngle = (int) ((angle*CHARACTER_SPRITE_SHEET_ROW)/(Math.PI*2));

		// True if the current angle the sprite is moving is within 90 degrees of the current angle the sprite is facing
		if(CHARACTER_SPRITE_SHEET_ROW - Math.abs(walkAngle-mCharacterAngle)%CHARACTER_SPRITE_SHEET_ROW <= 2 
				|| Math.abs(walkAngle-mCharacterAngle)%CHARACTER_SPRITE_SHEET_ROW <= 2){

			// Walking forwards
			if(backwards || !mCharacterSprite.isAnimationRunning()){
				backwards = false;
				final int frame = mCharacterAngle*(CHARACTER_SPRITE_SHEET_ROW-1);
				final int[] mframe = {frame+1,frame+2,frame+3,frame+4,frame+5,frame+6};
				mCharacterSprite.animate(mDuration, mframe);
			}
		}
		else{

			// Walking backwards
			if(!backwards || !mCharacterSprite.isAnimationRunning()){
				backwards = true;
				final int frame = mCharacterAngle*(CHARACTER_SPRITE_SHEET_ROW-1);
				final int[] mframe = {frame+6,frame+5,frame+4,frame+3,frame+2,frame+1};
				mCharacterSprite.animate(mDuration, mframe);
			}
		}

		// Update the current velocity
		mPhysicsHandler.setVelocity(pValueX * WALK_SPEED_MULTIPLIER, pValueY * WALK_SPEED_MULTIPLIER);
	}

	/**
	 * Angles the character in the appropriate direction
	 * 
	 * @param pValueX
	 *            x value of the analog control
	 *            
	 * @param pValueY
	 *            y value of the analog control
	 */
	public void look(float pValueX, float pValueY){

		// Find the x and y offset of the center of rotation for the gun
		double offsetX =  (ITEM_X_OFFSET_MULTIPLIER * Math.cos(Math.atan2(pValueY,pValueX)));
		double offsetY =  (ITEM_Y_OFFSET_MULTIPLIER * Math.sin(Math.atan2(pValueY,pValueX)));

		// Set the x and y offset
		mItemSprite.setX((float)offsetX + ITEM_X_OFFSET);
		mItemSprite.setY((float)offsetY + ITEM_Y_OFFSET);

		// Get the angle in radians from [0, 2PI]
		double angle = this.getAngle(pValueX, pValueY);

		// Check to see if gun angle needs updating
		if(mItemAngle != (int)((angle*16)/(Math.PI*2))){

			// Update the gun angle
			mItemAngle = (int) ((angle*16)/(Math.PI*2));

			// Set the frame of the gun to the new angle
			mItemSprite.stopAnimation((int) mItemAngle);

			// Gun should be draw BEHIND of the character
			if(mItemAngle >= 2 && mItemAngle <= 9){
				mCharacterSprite.setZIndex(1);
				mItemSprite.setZIndex(0);
				this.sortChildren();

			}
			// Gun should be draw INFRONT of the character
			else{
				mCharacterSprite.setZIndex(0);
				mItemSprite.setZIndex(1);
				this.sortChildren();
			}
		}

		// Check to see if character angle needs updating
		if(mCharacterAngle != (int)((angle*CHARACTER_SPRITE_SHEET_ROW)/(Math.PI*2))){

			// Update the gun angle
			mCharacterAngle =  (int) ((angle*CHARACTER_SPRITE_SHEET_ROW)/(Math.PI*2));

			// Get the row of the new frame
			int frame = mCharacterAngle*(CHARACTER_SPRITE_SHEET_ROW-1);

			// Set the new frame on the (n+1) column of the row
			mCharacterSprite.stopAnimation(frame+((mCharacterSprite.getCurrentTileIndex()+1)%CHARACTER_SPRITE_SHEET_COL));

		}
	}


	/**
	 * Stops all animation and movement of the character sprite 
	 */
	public void stop() {
		// Stop motion of the character
		mPhysicsHandler.setVelocity(0, 0);

		// Stop animation of the character
		mCharacterSprite.stopAnimation(mCharacterAngle*(CHARACTER_SPRITE_SHEET_ROW-1));
	}

	/**
	 * Angles the character in the appropriate direction
	 * 
	 * @param pValueX
	 *            x value of the analog control
	 *            
	 * @param pValueY
	 *            y value of the analog control
	 *            
	 * @return
	 *           angle in radians [0,2PI]
	 */
	private double getAngle(float pValueX, float pValueY) {

		// Gets the angle in radians
		double inRads = Math.atan2(pValueY,pValueX);

		// Maps the coordinate system to [0, 2PI]
		inRads = (inRads < 0) ? Math.abs(inRads) : 2*Math.PI - inRads;

		return inRads;
	}
}
