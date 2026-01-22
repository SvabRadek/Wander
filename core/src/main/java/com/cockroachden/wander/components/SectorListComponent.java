package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import java.util.ArrayList;
import java.util.List;

public class SectorListComponent implements Component {
    public List<Entity> sectors = new ArrayList<>();
}
