package com.cockroachden.wander.map;

import java.util.ArrayList;
import java.util.List;

public class Region {
    private final List<Sector> sectors;
    private int mergeCount = 0;

    // Nebula Properties
    public boolean isNebula = false;
    public float nobleGasRichness = 0f;

    public Region() {
        this.sectors = new ArrayList<>();
    }

    public void addSector(Sector sector) {
        if (!sectors.contains(sector)) {
            sectors.add(sector);
        }
    }

    public List<Sector> getSectors() {
        return sectors;
    }

    public boolean contains(Sector sector) {
        return sectors.contains(sector);
    }

    public void incrementMergeCount() {
        this.mergeCount++;
    }

    public int getMergeCount() {
        return mergeCount;
    }

    public int getMinDistanceToStar() {
        int min = Integer.MAX_VALUE;
        for (Sector s : sectors) {
            if (s.getDistanceToStar() < min) {
                min = s.getDistanceToStar();
            }
        }
        return min;
    }

    public boolean canMerge() {
        int minDist = getMinDistanceToStar();
        // Rule: Region can be merged (Distance - 1) times.
        // e.g. Dist 2 -> Max 1 merge. Dist 3 -> Max 2 merges.
        // Merges allowed if mergeCount < (minDist - 1)
        return mergeCount < (minDist - 1);
    }
}
