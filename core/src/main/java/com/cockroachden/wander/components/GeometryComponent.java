package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class GeometryComponent implements Component {
    public float[] vertices; // 8 floats
    public final Vector2 center = new Vector2();

    public GeometryComponent() {
    }

    public GeometryComponent(float[] vertices) {
        this.vertices = vertices;
        if (vertices != null && vertices.length >= 8) {
            float cx = (vertices[0] + vertices[2] + vertices[4] + vertices[6]) / 4f;
            float cy = (vertices[1] + vertices[3] + vertices[5] + vertices[7]) / 4f;
            this.center.set(cx, cy);
        }
    }
}
