package com.cmsc22.views;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
public class LoadingArea extends Sprite {
    private int id;
    private int capacity = 4; 

    public LoadingArea(double xPos, double yPos, double size, int id) {
        super(xPos, yPos, null); 
        this.width = size;
        this.height = size;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return xPos;
    }

    public double getY() {
        return yPos;
    }

    public double getSize() {
        return width;
    }

    public int getCapacity() {
        return capacity;
    }
}