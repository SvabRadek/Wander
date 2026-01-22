package com.cockroachden.wander.map;

import com.badlogic.gdx.math.Vector2;

public class Sector {
    public final int x;
    public final int y;
    private StarSystem starSystem;
    private float[] vertices; // x1, y1, x2, y2, ... (8 floats)
    private final Vector2 center;
    private Region region;
    private int distanceToStar = Integer.MAX_VALUE;

    public Sector(int x, int y, float[] vertices) {
        this.x = x;
        this.y = y;
        this.vertices = vertices;

        // Calculate center
        float cx = (vertices[0] + vertices[2] + vertices[4] + vertices[6]) / 4f;
        float cy = (vertices[1] + vertices[3] + vertices[5] + vertices[7]) / 4f;
        this.center = new Vector2(cx, cy);
    }

    public float[] getVertices() {
        return vertices;
    }

    public Vector2 getCenter() {
        return center;
    }

    public void setStarSystem(StarSystem starSystem) {
        this.starSystem = starSystem;
    }

    public StarSystem getStarSystem() {
        return starSystem;
    }

    public boolean hasStarSystem() {
        return starSystem != null;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    public void setDistanceToStar(int distance) {
        this.distanceToStar = distance;
    }

    public int getDistanceToStar() {
        return distanceToStar;
    }
}
