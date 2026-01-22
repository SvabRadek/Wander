package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class ParentRegionComponent implements Component {
    public Entity regionEntity;

    public ParentRegionComponent() {
    }

    public ParentRegionComponent(Entity regionEntity) {
        this.regionEntity = regionEntity;
    }
}
