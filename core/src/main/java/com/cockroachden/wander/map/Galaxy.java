package com.cockroachden.wander.map;

import java.util.ArrayList;
import java.util.List;

public class Galaxy {
    private final List<Sector> sectors;
    private final List<Region> regions;
    private final int width; // Logical bounds
    private final int height;

    private final Sector[][] grid;

    public Galaxy(int width, int height) {
        this.width = width;
        this.height = height;
        this.sectors = new ArrayList<>();
        this.regions = new ArrayList<>();
        this.grid = new Sector[width][height];
    }

    public void addSector(Sector sector) {
        sectors.add(sector);
        if (sector.x >= 0 && sector.x < width && sector.y >= 0 && sector.y < height) {
            grid[sector.x][sector.y] = sector;
        }
    }

    public void removeSector(Sector sector) {
        sectors.remove(sector);
        if (sector.x >= 0 && sector.x < width && sector.y >= 0 && sector.y < height) {
            if (grid[sector.x][sector.y] == sector) {
                grid[sector.x][sector.y] = null;
            }
        }
    }

    public Sector getSector(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return grid[x][y];
    }

    public void addRegion(Region region) {
        regions.add(region);
    }

    public void removeRegion(Region region) {
        regions.remove(region);
    }

    public List<Region> getRegions() {
        return regions;
    }

    public List<Sector> getSectors() {
        return sectors;
    }

    public int getWidth() {
        return width;
    }

    public Sector getSectorAt(float worldX, float worldY, float sectorSize) {
        int x = (int) (worldX / sectorSize);
        int y = (int) (worldY / sectorSize);
        return getSector(x, y);
    }

    private GalaxyProperties properties;

    public void setProperties(GalaxyProperties properties) {
        this.properties = properties;
    }

    public GalaxyProperties getProperties() {
        return properties;
    }

    public int getHeight() {
        return height;
    }
}
