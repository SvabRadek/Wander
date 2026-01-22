package com.cockroachden.wander.components;

import com.badlogic.ashley.core.Component;

public class ResourceComponent implements Component {
    // 0.0 - 1.0
    public float metalRichness;
    public float rareMineralRichness;
    public float nobleGasRichness;

    // Aggregate stats for regions
    public int systemCount;
    public int planetCount;
    public int habitableCount;
    public int luxuryCount;
}
