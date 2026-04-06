package com.cmsc22.models;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Sprite {
    protected Image img;
    protected double xPos, yPos;
    protected double width, height;

    public Sprite(double xPos, double yPos, Image image) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.loadImage(image);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D(this.xPos, this.yPos, this.width, this.height);
    }

    protected void setSize() {
        if (this.img != null) {
            this.width = this.img.getWidth();
            this.height = this.img.getHeight();
        }
    }

    public boolean collidesWith(Sprite other) {
        return this.getBounds().intersects(other.getBounds());
    }

    protected void loadImage(Image img) {
        try {
            this.img = img;
            this.setSize();
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
        }
    }

    //Corrected render method to center the image.
    public void render(GraphicsContext gc) {
        if (img != null) {
            gc.drawImage(this.img, this.xPos - this.width / 2, this.yPos - this.height / 2);
        }
    }

    public Image getImage() {
        return this.img;
    }

    public double getXPos() {
        return this.xPos;
    }

    public double getYPos() {
        return this.yPos;
    }

    public void setXPos(double xPos) {
        this.xPos = xPos;
    }

    public void setYPos(double yPos) {
        this.yPos = yPos;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}