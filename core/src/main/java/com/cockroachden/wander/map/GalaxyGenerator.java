package com.cockroachden.wander.map;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GalaxyGenerator {
    private final Random random = new Random();

    public Galaxy generate(GalaxyProperties properties) {
        // Set Seed
        random.setSeed(properties.seed);

        int targetStarCount = properties.targetStarCount;
        float density = properties.density;

        // Calculate grid size based on target star count and density (Circle Area)
        // Area = count / density
        // Radius = sqrt(Area / PI)
        // We assume 1 sector unit size

        float area = targetStarCount / density;
        float radius = (float) Math.sqrt(area / Math.PI);
        int gridSize = (int) Math.ceil(radius * 2.2f); // Add padding

        Galaxy galaxy = new Galaxy(gridSize, gridSize);
        galaxy.setProperties(properties); // Optional: Store props in galaxy if needed later

        // Generate Jittered Vertices
        // vertexGrid[x][y] is a 2D vector
        float[][] xVerts = new float[gridSize + 1][gridSize + 1];
        float[][] yVerts = new float[gridSize + 1][gridSize + 1];

        float jitterAmount = 0.25f; // Max offset from grid center

        for (int x = 0; x <= gridSize; x++) {
            for (int y = 0; y <= gridSize; y++) {
                // Base grid position centered at (0,0) relative to galaxy?
                // Or just use index coordinates. Let's use index coordinates.
                float baseX = x;
                float baseY = y;

                // Jitter
                float offsetX = (random.nextFloat() * 2f - 1f) * jitterAmount;
                float offsetY = (random.nextFloat() * 2f - 1f) * jitterAmount;

                xVerts[x][y] = baseX + offsetX;
                yVerts[x][y] = baseY + offsetY;
            }
        }

        // Create Sectors

        // 1. Create All Valid Sectors
        float centerX = gridSize / 2f;
        float centerY = gridSize / 2f;
        List<Sector> validSectors = new ArrayList<>();
        Sector[][] sectorGrid = new Sector[gridSize][gridSize];

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                // Check if sector center is within radius
                float sectorCenterX = (xVerts[x][y] + xVerts[x + 1][y + 1]) / 2f;
                float sectorCenterY = (yVerts[x][y] + yVerts[x + 1][y + 1]) / 2f;

                float dist = (float) Math.hypot(sectorCenterX - centerX, sectorCenterY - centerY);

                if (dist <= radius) {
                    float[] vertices = new float[] {
                            xVerts[x][y], yVerts[x][y], // Bottom Left
                            xVerts[x + 1][y], yVerts[x + 1][y], // Bottom Right
                            xVerts[x + 1][y + 1], yVerts[x + 1][y + 1], // Top Right
                            xVerts[x][y + 1], yVerts[x][y + 1] // Top Left
                    };

                    Sector sector = new Sector(x, y, vertices);
                    galaxy.addSector(sector);
                    validSectors.add(sector);
                    sectorGrid[x][y] = sector;
                }
            }
        }

        // 2. Place Stars (Neighbor Exclusion)
        java.util.Collections.shuffle(validSectors, random);

        int placedCount = 0;
        for (Sector sector : validSectors) {
            if (placedCount >= targetStarCount)
                break;

            // Check neighbors
            boolean neighborHasStar = false;
            for (int nx = sector.x - 1; nx <= sector.x + 1; nx++) {
                for (int ny = sector.y - 1; ny <= sector.y + 1; ny++) {
                    if (nx == sector.x && ny == sector.y)
                        continue;

                    if (nx >= 0 && nx < gridSize && ny >= 0 && ny < gridSize) {
                        Sector neighbor = sectorGrid[nx][ny];
                        if (neighbor != null && neighbor.hasStarSystem()) {
                            neighborHasStar = true;
                            break;
                        }
                    }
                }
                if (neighborHasStar)
                    break;
            }

            if (!neighborHasStar) {
                StarSystem system = new StarSystem(generateName(), generateColor());

                // Populate Properties
                system.planetCount = random.nextInt(10) + 1; // 1 to 10 planets
                // Habitable count mostly 0-2
                int habRoll = random.nextInt(100);
                if (habRoll < 50)
                    system.habitableCount = 0;
                else if (habRoll < 85)
                    system.habitableCount = 1;
                else if (habRoll < 95)
                    system.habitableCount = 2;
                else
                    system.habitableCount = 3 + random.nextInt(Math.max(1, system.planetCount / 3));

                // Cap habitable at planet count
                system.habitableCount = Math.min(system.habitableCount, system.planetCount);

                // Resources
                float mult = properties.resourceRichnessMultiplier;

                system.metalRichness = Math.min(1.0f, (0.2f + random.nextFloat() * 0.8f) * mult);
                system.rareMineralRichness = Math.min(1.0f, (random.nextFloat() * 0.6f) * mult);
                system.nobleGasRichness = Math.min(1.0f, random.nextFloat() * mult);

                if (random.nextFloat() < (0.10f * mult)) {
                    system.hasLuxuryResources = true;
                }

                sector.setStarSystem(system);
                placedCount++;
            }
        }

        System.out
                .println("Requested " + targetStarCount + " stars. Placed " + placedCount + " after neighbor checks.");

        mergeVoidSectors(galaxy, gridSize, sectorGrid);
        System.out.println("Sectors after merge: " + galaxy.getSectors().size());

        // Step 5: Nebula Generation (Post-Processing)
        for (Region region : galaxy.getRegions()) {
            boolean hasStar = false;
            for (Sector s : region.getSectors()) {
                if (s.hasStarSystem()) {
                    hasStar = true;
                    break;
                }
            }

            if (!hasStar) {
                int minDist = region.getMinDistanceToStar();
                if (minDist >= 3) {
                    region.isNebula = true;
                    // Richness based on depth
                    float baseRichness = 0.1f + ((minDist - 3) * 0.05f);
                    region.nobleGasRichness = Math.min(0.3f * properties.resourceRichnessMultiplier,
                            baseRichness * properties.resourceRichnessMultiplier);
                }
            }
        }

        return galaxy;
    }

    private void mergeVoidSectors(Galaxy galaxy, int gridSize, Sector[][] sectorGrid) {
        // 1. Calculate Distances
        calculateDistances(galaxy, gridSize, sectorGrid);

        // 2. Initialize Regions
        if (galaxy.getRegions().isEmpty()) {
            for (Sector sector : galaxy.getSectors()) {
                Region region = new Region();
                region.addSector(sector);
                sector.setRegion(region);
                galaxy.addRegion(region);
            }
        }

        // 3. Merging Loop (Run until stability)
        int pass = 0;
        int totalMerges = 0;

        while (true) {
            pass++;
            List<Region> voidRegions = new ArrayList<>();
            // Identify Void Regions
            for (Region r : galaxy.getRegions()) {
                if (isVoidRegion(r, gridSize, sectorGrid)) {
                    voidRegions.add(r);
                }
            }
            java.util.Collections.shuffle(voidRegions, random);

            List<Region> consumed = new ArrayList<>();
            int mergesThisPass = 0;

            for (Region region : voidRegions) {
                if (consumed.contains(region))
                    continue;

                // Check merge limit rule
                if (!region.canMerge()) {
                    continue;
                }

                // Find a valid neighbor region to merge with
                Region neighbor = findMergeableNeighbor(region, gridSize, sectorGrid, voidRegions, consumed);

                if (neighbor != null) {
                    performRegionMerge(galaxy, region, neighbor);
                    region.incrementMergeCount(); // Increment count for the surviving region

                    consumed.add(region);
                    consumed.add(neighbor);
                    mergesThisPass++;
                }
            }

            System.out.println("Pass " + pass + " (Region Merges): " + mergesThisPass);
            totalMerges += mergesThisPass;

            if (mergesThisPass == 0) {
                break;
            }
        }
    }

    private void calculateDistances(Galaxy galaxy, int gridSize, Sector[][] sectorGrid) {
        List<Sector> queue = new ArrayList<>();

        // Init distances
        for (Sector s : galaxy.getSectors()) {
            if (s.hasStarSystem()) {
                s.setDistanceToStar(0);
                queue.add(s);
            } else {
                s.setDistanceToStar(Integer.MAX_VALUE);
            }
        }

        // BFS
        int head = 0;
        while (head < queue.size()) {
            Sector current = queue.get(head++);
            int dist = current.getDistanceToStar();

            // Check 8 neighbors
            for (int x = current.x - 1; x <= current.x + 1; x++) {
                for (int y = current.y - 1; y <= current.y + 1; y++) {
                    if (x == current.x && y == current.y)
                        continue;

                    if (x >= 0 && x < gridSize && y >= 0 && y < gridSize) {
                        Sector neighbor = sectorGrid[x][y];
                        if (neighbor != null) {
                            if (neighbor.getDistanceToStar() == Integer.MAX_VALUE) {
                                neighbor.setDistanceToStar(dist + 1);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isVoidRegion(Region region, int gridSize, Sector[][] sectorGrid) {
        for (Sector s : region.getSectors()) {
            if (!isVoid(s, gridSize, sectorGrid)) {
                return false;
            }
        }
        return true;
    }

    private Region findMergeableNeighbor(Region region, int gridSize, Sector[][] sectorGrid, List<Region> voidRegions,
            List<Region> consumed) {
        List<Region> candidates = new ArrayList<>();

        for (Sector s : region.getSectors()) {
            // Check 4 orthogonal neighbors
            int[][] offsets = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
            for (int[] off : offsets) {
                int nx = s.x + off[0];
                int ny = s.y + off[1];

                if (nx >= 0 && nx < gridSize && ny >= 0 && ny < gridSize) {
                    Sector neighborSector = sectorGrid[nx][ny];
                    if (neighborSector != null) {
                        Region nr = neighborSector.getRegion();
                        // Neighbor must be valid, different region, also void, and not consumed
                        if (nr != null && nr != region && voidRegions.contains(nr) && !consumed.contains(nr)) {
                            if (!candidates.contains(nr)) {
                                candidates.add(nr);
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty())
            return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private void performRegionMerge(Galaxy galaxy, Region target, Region source) {
        // Add all sectors from source to target
        for (Sector s : source.getSectors()) {
            target.addSector(s);
            s.setRegion(target);
        }
        // Remove source region from galaxy
        galaxy.removeRegion(source);
    }

    private boolean isVoid(Sector sector, int gridSize, Sector[][] sectorGrid) {
        if (sector.hasStarSystem())
            return false;

        // Check 8 neighbors for stars
        for (int nx = sector.x - 1; nx <= sector.x + 1; nx++) {
            for (int ny = sector.y - 1; ny <= sector.y + 1; ny++) {
                if (nx == sector.x && ny == sector.y)
                    continue;
                if (nx >= 0 && nx < gridSize && ny >= 0 && ny < gridSize) {
                    Sector neighbor = sectorGrid[nx][ny];
                    if (neighbor != null && neighbor.hasStarSystem()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String generateName() {
        return "System " + random.nextInt(1000);
    }

    private Color generateColor() {
        return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f);
    }
}
