package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class EmpireComponent implements Component {
    public String name;
    public Color color;

    // Resources
    public float credits;
    public float metals;
    public float rareMinerals;
    public float nobleGases;
    public float luxuryResources;
    public float taxRate = 0.05f; // 5% default

    // Resource Deltas (Per Tick)
    public float deltaCredits;
    public float deltaMetals;
    public float deltaRareMinerals;
    public float deltaNobleGases;
    public float deltaLuxuryResources;

    // Owned Systems
    public List<Entity> ownedSystems = new ArrayList<>();

    public EmpireComponent() {
    }

    public EmpireComponent(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
