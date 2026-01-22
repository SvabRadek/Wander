package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;

public class RegionTypeComponent implements Component {
    public enum RegionType {
        STAR_SYSTEM,
        NEBULA,
        VOID,
        UNKNOWN
    }

    public RegionType type;

    public RegionTypeComponent() {
        this.type = RegionType.UNKNOWN;
    }

    public RegionTypeComponent(RegionType type) {
        this.type = type;
    }
}
