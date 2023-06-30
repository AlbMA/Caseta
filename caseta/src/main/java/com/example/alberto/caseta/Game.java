package com.example.alberto.caseta;

import android.util.Log;

public class Game {
    private String filename;
    private String imagename;
    private String name;

    public Game(String filename, String imagename, String name) {
        this.filename = filename;
        this.imagename = imagename;
        this.name = name;

        //Log.i("Mio", "Creando: " + this.filename  + " ifch: " +  this.imagename + " nam: " + this.name);
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }

    public String getName() {
        return name;
    }

    public String getImagename() {
        return imagename;
    }

    public void setImagename(String imagename) {
        this.imagename = imagename;
    }
}
