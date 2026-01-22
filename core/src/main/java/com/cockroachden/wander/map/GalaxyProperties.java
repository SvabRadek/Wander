package com.cockroachden.wander.map;

public class GalaxyProperties {
    public final int targetStarCount;
    public final float density;
    public final long seed;
    public final float resourceRichnessMultiplier;

    public GalaxyProperties(int targetStarCount, float density, long seed, float resourceRichnessMultiplier) {
        this.targetStarCount = targetStarCount;
        this.density = density;
        this.seed = seed;
        this.resourceRichnessMultiplier = resourceRichnessMultiplier;
    }

    public GalaxyProperties(int targetStarCount, float density, float resourceRichnessMultiplier) {
        this(targetStarCount, density, System.currentTimeMillis(), resourceRichnessMultiplier);
    }

    // Backwards compatibility/defaults
    public GalaxyProperties(int targetStarCount, float density) {
        this(targetStarCount, density, System.currentTimeMillis(), 1.0f);
    }
}
