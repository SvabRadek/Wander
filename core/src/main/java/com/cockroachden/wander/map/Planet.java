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
    public long deltaPopulation; // Change per tick
    public float quality; // 0.0 to 1.0 suitability
    public float industryLevel; // 0.0 to 1.0 representing industrialization
    public String description;

    // Resource Richness specific to this planet (0.0 - 1.0)
    public float metalRichness;
    public float rareMineralRichness;
    public float nobleGasRichness;

    // Computed Properties
    public float tradeCapacity;

    public Planet(String name, PlanetType type) {
        this.name = name;
        this.type = type;
    }
}
