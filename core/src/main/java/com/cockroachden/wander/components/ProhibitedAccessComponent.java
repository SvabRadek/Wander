package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import java.util.HashSet;
import java.util.Set;

public class ProhibitedAccessComponent implements Component {
    public Set<Entity> blockedByEmpires = new HashSet<>();
}
