package com.cockroachden.wander.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;

public class Galaxy {
    private final PooledEngine engine;
    private final int width; // Logical bounds
    private final int height;

    // Spatial lookup for Sectors (Entities)
    private final Entity[][] sectorGrid;

    private GalaxyProperties properties;

    public Galaxy(int width, int height) {
        this.width = width;
        this.height = height;
        this.engine = new PooledEngine();
        this.sectorGrid = new Entity[width][height];
    }

    public PooledEngine getEngine() {
        return engine;
    }

    public void registerSector(Entity sector, int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            sectorGrid[x][y] = sector;
        }
    }

    public Entity getSectorEntity(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height)
            return null;
        return sectorGrid[x][y];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setProperties(GalaxyProperties properties) {
        this.properties = properties;
    }

    public GalaxyProperties getProperties() {
        return properties;
    }

    // Helper to add Entity
    public void addEntity(Entity entity) {
        engine.addEntity(entity);
    }
}
