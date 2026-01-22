package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

public class TradeRouteComponent implements Component {
    public Entity source;
    public Entity target;
    public List<Vector2> path; // World coordinates (sector coords * size?) or just grid coords?
                               // Better use world coordinates for rendering directly.
    public float value;
}
