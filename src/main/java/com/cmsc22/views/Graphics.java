package com.cmsc22.views;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Graphics {
    private Image collisionSheet;
    private int collisionFrameWidth;
    private int collisionFrameHeight;
    private int currentCollisionFrame = 0;
    private long collisionStartTime = 0;
    private final int COLLISION_ANIMATION_DELAY = 100; // milliseconds per frame
    private final int NUM_COLLISION_FRAMES = 4; // Number of frames in the sprite sheet
    private boolean showingCollision = false;

    public Graphics(Image collisionSheet, int frameWidth, int frameHeight) {
        this.collisionSheet = collisionSheet;
        this.collisionFrameWidth = frameWidth;
        this.collisionFrameHeight = frameHeight;
    }

    public void renderCollision(GraphicsContext gc, double x, double y) {
        if (showingCollision) {
            long elapsedTime = System.currentTimeMillis() - collisionStartTime;
            currentCollisionFrame = (int) (elapsedTime / COLLISION_ANIMATION_DELAY) % NUM_COLLISION_FRAMES;

            // Check if animation is complete
            if (currentCollisionFrame >= NUM_COLLISION_FRAMES) {
                showingCollision = false;
                currentCollisionFrame = 0; // Reset frame index
            } else {
                // Draw the current frame of the animation
                gc.drawImage(collisionSheet, currentCollisionFrame * collisionFrameWidth, 0,
                        collisionFrameWidth, collisionFrameHeight,
                        x - collisionFrameWidth / 2, y - collisionFrameHeight / 2,
                        collisionFrameWidth, collisionFrameHeight);
            }
        }
    }

    public void showCollision(double x, double y) {
        showingCollision = true;
        collisionStartTime = System.currentTimeMillis();
    }
}