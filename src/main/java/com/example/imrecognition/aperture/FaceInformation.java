package com.example.imrecognition.aperture;

import com.meylemueller.obj.isy.ISYObjectEntity;

public class FaceInformation {

    private String name;
    private double x;
    private double y;
    private double width;
    private double height;
    private ISYObjectEntity isyObject;

    public FaceInformation(String name, double x, double y, double width, double heigth, ISYObjectEntity isyObject){
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = heigth;
        this.isyObject = isyObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public ISYObjectEntity getIsyObject() {
        return isyObject;
    }

    public void setIsyObject(ISYObjectEntity isyObject) {
        this.isyObject = isyObject;
    }
}
