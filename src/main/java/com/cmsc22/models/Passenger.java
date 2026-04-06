package com.cmsc22.models;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.scene.image.Image;

public class Passenger extends Sprite {
    private long loadingStartTime; //Variable to track loading start time

    public Passenger(double xPos, double yPos, Image image) {
        super(xPos, yPos, image);
        loadingStartTime = System.currentTimeMillis();
    }

    public long getLoadingStartTime() {
        return loadingStartTime;
    }
}