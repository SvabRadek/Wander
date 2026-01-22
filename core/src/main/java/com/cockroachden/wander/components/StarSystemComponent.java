package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;

public class StarSystemComponent implements Component {
    public String name;
    public Color color;
    public int planetCount;
    public int habitableCount;
    public boolean hasLuxuryResources;

    // Resource richness specific to this system
    public float metalRichness;
    public float rareMineralRichness;
    public float nobleGasRichness;

    public java.util.List<com.cockroachden.wander.map.Planet> planets = new java.util.ArrayList<>();
}
