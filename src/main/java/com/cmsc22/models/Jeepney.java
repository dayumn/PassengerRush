package com.cmsc22.models;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Jeepney extends Sprite {
    private String name;
    private int points = 0;
    private double speed = 3;
    private int speedBoost = 0; 
    private int capacity = 14;
    private int passengers = 0;
    private static Jeepney winner;
    
    private Image imgUp;
    private Image imgDown;
    private Image imgLeft;
    private Image imgRight;

    public Jeepney(double xPos, double yPos, String name, Image imageUp, Image imageDown, Image imageLeft, Image imageRight) {
        super(xPos, yPos, imageDown); 
        this.name = name;
        this.imgUp = imageUp;
        this.imgDown = imageDown;
        this.imgLeft = imageLeft;
        this.imgRight = imageRight;
    }

    public void setDirectionImage(String direction) {
        switch (direction) {
            case "UP"    -> setImage(imgUp);
            case "DOWN"  -> setImage(imgDown);
            case "LEFT"  -> setImage(imgLeft);
            case "RIGHT" -> setImage(imgRight);
        }
    }

    // Getters and setters
    public static Jeepney getWinner() {
        return winner;
    }
    
    public String getName() {
        return name;
    }

    public static void setWinner(Jeepney winner) {
        Jeepney.winner = winner;
    }

    public void increaseSpeed(int amount) {
        this.speed += amount;
    }

    public void decreaseSpeed(int amount) {
        this.speed -= amount;
    }

    public int getSpeedBoost() {
        return speedBoost;
    }

    public void setSpeedBoost(int speedBoost) {
        this.speedBoost = speedBoost;
    }
    
    public void setSpeed(double d) {
        this.speed = d;
    }
    
    public double getSpeed() {
        return speed + speedBoost;
    }

    public int getPoints() {
        return points;
    }

    public void incrementPoints(int pointsToAdd) {
        this.points += pointsToAdd;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getPassengers() {
        return passengers;
    }

    public void incrementPassengers() {
        this.passengers++;
    }

    public void setPassengers(int numPassengers) { 
        this.passengers = numPassengers;
    }
    
    public void render(GraphicsContext gc, boolean isInvincible) {
        if (img != null) {
            gc.drawImage(this.img, this.xPos - this.width / 2, this.yPos - this.height / 2);
            // Invicibility
            if (isInvincible) {
                gc.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.5)); 
                gc.fillRect(this.xPos - this.width / 2, this.yPos - this.height / 2, this.width, this.height);
            }
        }
    }

    public void setImage(Image img) {
        this.img = img;
        this.setSize();
    }
}