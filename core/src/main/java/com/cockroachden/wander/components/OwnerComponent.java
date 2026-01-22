package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class OwnerComponent implements Component {
    public Entity owner;

    public OwnerComponent(Entity owner) {
        this.owner = owner;
    }
}
