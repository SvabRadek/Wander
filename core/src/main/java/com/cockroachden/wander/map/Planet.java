package com.cockroachden.wander.map;

public class Planet {
    public enum PlanetType {
        TERRESTRIAL,
        GAS_GIANT,
        ICE_GIANT,
        BARREN_ROCK,
        MOLTEN,
        OCEANIC,
        DESERT,
        ICE_WORLD
    }

    public String name;
    public PlanetType type;
    public long population;
    public float quality; // 0.0 to 1.0 suitability
    public String description;

    // Resource Richness specific to this planet (0.0 - 1.0)
    public float metalRichness;
    public float rareMineralRichness;
    public float nobleGasRichness;

    public Planet(String name, PlanetType type) {
        this.name = name;
        this.type = type;
    }
}
