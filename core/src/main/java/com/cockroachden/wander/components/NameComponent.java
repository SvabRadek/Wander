package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;

public class NameComponent implements Component {
    public String name;

    public NameComponent() {
    }

    public NameComponent(String name) {
        this.name = name;
    }
}
