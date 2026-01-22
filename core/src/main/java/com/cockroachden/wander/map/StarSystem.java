package com.cockroachden.wander.map;

import com.badlogic.gdx.graphics.Color;

public class StarSystem {
    public final String name;
    public final Color color;

    // Properties
    public int planetCount;
    public int habitableCount;

    // Resource Richness (0.0 - 1.0)
    public float metalRichness;
    public float rareMineralRichness;
    public float nobleGasRichness;

    public java.util.List<Planet> planets = new java.util.ArrayList<>();

    public boolean hasLuxuryResources;

    public StarSystem(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
