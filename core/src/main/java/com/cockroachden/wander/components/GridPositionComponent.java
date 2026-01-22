package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;

public class GridPositionComponent implements Component {
    public int x;
    public int y;

    public GridPositionComponent() {
    }

    public GridPositionComponent(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
