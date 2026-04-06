package com.cmsc22.models;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class PowerUp extends Sprite {
    private String type; // "speed", "moonLife", etc.
    private int duration; // Duration in milliseconds
    private int effectValue; // Value of the effect (e.g., speed increase)
    

    public PowerUp(double xPos, double yPos, String type, Image image, int duration, int effectValue) {
        super(xPos, yPos, image);
        this.type = type;
        this.duration = duration;
        this.effectValue = effectValue;
    }

    public String getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }

    public int getEffectValue() {
        return effectValue;
    }

    public void applyTo(Jeepney jeepney) {
        if (type.equals("speed")) {
            jeepney.increaseSpeed(effectValue);
        } else if (type.equals("crack")) {
            jeepney.decreaseSpeed(effectValue); // Reduce speed to 1/4, minimum speed 1.
        }
    }

    public void removeEffect(Jeepney jeepney) {
        if (type.equals("speed")) {
            jeepney.decreaseSpeed(effectValue);
        } else if (type.equals("crack")) {
            jeepney.increaseSpeed(effectValue); //restore the speed.
        }
    }
    
    
}