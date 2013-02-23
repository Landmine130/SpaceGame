package world;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import misc.Timer;
import vecmath.Matrix4f;
import vecmath.Quat4f;
import vecmath.Vector3f;

public class Animation {
	
	public static final String ANIMATIONDATA_PATH = "Resources/Models/Animations/";
	
	public static final HashMap<String, Animation> loadedAnimations = new HashMap<String, Animation>();
	
	private static final int HEADER_SIZE = 3;
	private static final int FLOAT_BYTE_COUNT = Float.SIZE / Byte.SIZE;
	public static Animation getAnimationForName(String name) {
		
		String path = ANIMATIONDATA_PATH + name;
		Animation a = loadedAnimations.get(path);
		
		if (a == null) {
			
			File file = new File(name);
			long dataSize = file.length() / FLOAT_BYTE_COUNT - HEADER_SIZE;
			
			try {
				DataInputStream reader = new DataInputStream(new FileInputStream(file));
				
				int fps = reader.readInt();
				int frameCount = reader.readInt();
				int jointCount = reader.readInt();

				Matrix4f[][] frames = new Matrix4f[jointCount][frameCount];
				int[] parentJointIndexes = new int[jointCount];
				
				Matrix4f rotationMatrix = new Matrix4f();

				for (int i = 0; i < dataSize; i++) {
					
					Vector3f position = new Vector3f(reader.readFloat(), reader.readFloat(), reader.readFloat());
					float ox = reader.readFloat();
					float oy = reader.readFloat();
					float oz = reader.readFloat();
					// possible optimization: Calculate ow instead of reading from file
					float ow = reader.readFloat();
					Quat4f orientation = new Quat4f(ox, oy, oz, ow);
					Matrix4f m = new Matrix4f(position);
					rotationMatrix.set(orientation);
					m.mul(rotationMatrix);
				}
				
				a = new Animation(frames, parentJointIndexes, 1.0f / fps);
			}
			catch (Exception e) {
				System.err.println("Error: could not read file " + path);
				e.printStackTrace();
			}
		}
		return a;
	}
	
	
	private float startTime;
	private float frameLength;
	private Matrix4f[][] frames;
	private int[] parentJointIndexes;

	/**
	 * Creates a new Animation object with the specified parameters
	 * @param frames an array of frames with each frame represented as an array of Matrix4f objects
	 * @param parentJointIndexes an array which specifies the index of the parent for each Matrix4f in a frame
	 * @param frameLength the duration of a single frame in seconds
	 */
	public Animation(Matrix4f[][] frames, int[] parentJointIndexes, float frameLength) {
		this.frames = frames;
		this.parentJointIndexes = parentJointIndexes;
		this.frameLength = frameLength;
	}
	
	public float getTime() {
		return (float)Timer.getTime() - startTime;
	}
	
	public void setTime(float time) {
		startTime = (float)Timer.getTime() - time;
	}
	
	public int getFrameCount() {
		return frames.length;
	}
	
	/**
	 * Calculates object-local positions of all bones in this animation for a given time
	 * @param animationTime the time since animation start that the return value is calculated for
	 * @return the object-local positions of the bones for this animation
	 */
	public Matrix4f[] getJointPositions(float animationTime) {
		
		Matrix4f scaledNext = new Matrix4f();
		Matrix4f scale = new Matrix4f();
		
		Matrix4f[] currentPositions = new Matrix4f[getFrameCount()];
		
		float currentFrame = animationTime * frameLength;
		int currentIndex = (int) currentFrame % getFrameCount();
		int nextIndex = (currentIndex + 1) % getFrameCount();
		
		Matrix4f[] lastFrame = frames[currentIndex];
		Matrix4f[] nextFrame = frames[nextIndex];
		
		float lastFrameWeight = currentFrame / currentIndex;
		
		for (int i = 0; i < lastFrame.length; i++) {
			
			Matrix4f interpolatedMatrix = new Matrix4f(lastFrame[i]);
			scale.setScale(lastFrameWeight);
			interpolatedMatrix.mul(scale);
			
			scaledNext.set(nextFrame[i]);
			scale.setScale(1 - lastFrameWeight);
			scaledNext.mul(scale);
			interpolatedMatrix.mul(scaledNext);
			
			interpolatedMatrix.mul(currentPositions[parentJointIndexes[i]], interpolatedMatrix);
			currentPositions[i] = interpolatedMatrix;
		}
		
		return currentPositions;
	}
	
	/**
	 * Calculates object-local positions of all bones in this animation for the time since animation start
	 * @return the current object-local positions of the bones for this animation
	 */
	public Matrix4f[] getJointPositions() {
		return getJointPositions(getTime());
	}
}