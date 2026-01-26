package com.cockroachden.wander.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.cockroachden.wander.components.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GalaxyGenerator {
    private final Random random = new Random();

    public Galaxy generate(GalaxyProperties properties) {
        // Set Seed
        random.setSeed(properties.seed);

        int targetStarCount = properties.targetStarCount;
        float density = properties.density;

        float area = targetStarCount / density;
        float radius = (float) Math.sqrt(area / Math.PI);
        int gridSize = (int) Math.ceil(radius * 2.2f); // Add padding

        Galaxy galaxy = new Galaxy(gridSize, gridSize);
        galaxy.setProperties(properties);

        // --- Generation using DTOs ---

        // Generate Jittered Vertices
        float[][] xVerts = new float[gridSize + 1][gridSize + 1];
        float[][] yVerts = new float[gridSize + 1][gridSize + 1];

        float jitterAmount = 0.25f;

        for (int x = 0; x <= gridSize; x++) {
            for (int y = 0; y <= gridSize; y++) {
                float baseX = x;
                float baseY = y;
                float offsetX = (random.nextFloat() * 2f - 1f) * jitterAmount;
                float offsetY = (random.nextFloat() * 2f - 1f) * jitterAmount;

                xVerts[x][y] = baseX + offsetX;
                yVerts[x][y] = baseY + offsetY;
            }
        }

        // Create Sectors
        float centerX = gridSize / 2f;
        float centerY = gridSize / 2f;
        List<Sector> validSectors = new ArrayList<>();
        Sector[][] sectorGrid = new Sector[gridSize][gridSize];

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
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
                    validSectors.add(sector);
                    sectorGrid[x][y] = sector;
                }
            }
        }

        // Place Stars
        java.util.Collections.shuffle(validSectors, random);

        int placedCount = 0;
        for (Sector sector : validSectors) {
            if (placedCount >= targetStarCount)
                break;

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

                // Properties
                system.planetCount = random.nextInt(10) + 1;
                int habRoll = random.nextInt(100);
                if (habRoll < 50)
                    system.habitableCount = 0;
                else if (habRoll < 85)
                    system.habitableCount = 1;
                else if (habRoll < 95)
                    system.habitableCount = 2;
                else
                    system.habitableCount = 3 + random.nextInt(Math.max(1, system.planetCount / 3));

                system.habitableCount = Math.min(system.habitableCount, system.planetCount);

                float mult = properties.resourceRichnessMultiplier;
                system.metalRichness = Math.min(1.0f, (0.2f + random.nextFloat() * 0.8f) * mult);
                system.rareMineralRichness = Math.min(1.0f, (random.nextFloat() * 0.6f) * mult);
                system.nobleGasRichness = Math.min(1.0f, random.nextFloat() * mult);

                if (random.nextFloat() < (0.10f * mult)) {
                    system.hasLuxuryResources = true;
                }

                // GENERATE PLANETS
                for (int i = 0; i < system.planetCount; i++) {
                    boolean isHabitable = i < system.habitableCount;

                    Planet.PlanetType type;
                    float quality = 0f;

                    if (isHabitable) {
                        float r = random.nextFloat();
                        if (r < 0.5f)
                            type = Planet.PlanetType.TERRESTRIAL;
                        else
                            type = Planet.PlanetType.OCEANIC;

                        // Best Quality: 70% - 100%
                        quality = 0.7f + random.nextFloat() * 0.3f;

                    } else {
                        float r = random.nextFloat();

                        // Roughly 30% chance for "Mid Tier" types among non-habitable candidates
                        if (r < 0.3f) {
                            if (random.nextBoolean())
                                type = Planet.PlanetType.DESERT;
                            else
                                type = Planet.PlanetType.ICE_WORLD;

                            // Mid Quality: 30% - 70%
                            quality = 0.3f + random.nextFloat() * 0.4f;

                        } else {
                            // Low Tier / Uninhabitable
                            float r2 = random.nextFloat();
                            if (r2 < 0.4f)
                                type = Planet.PlanetType.BARREN_ROCK;
                            else if (r2 < 0.7f)
                                type = Planet.PlanetType.GAS_GIANT;
                            else if (r2 < 0.9f)
                                type = Planet.PlanetType.ICE_GIANT;
                            else
                                type = Planet.PlanetType.MOLTEN;

                            // Low Quality: 0%
                            quality = 0f;
                        }
                    }

                    Planet planet = new Planet(system.name + " " + (i + 1), type);
                    planet.quality = quality;

                    if (quality >= 0.7f) {
                        planet.description = "A thriving high-quality world.";
                    } else if (quality >= 0.3f) {
                        planet.description = "A harsh but colonizable world.";
                    } else {
                        planet.description = "A hostile environment.";
                    }

                    planet.population = 0; // Default to 0, empires will populate their home systems

                    // Planet Resources
                    planet.metalRichness = Math.max(0,
                            Math.min(1, system.metalRichness + (random.nextFloat() - 0.5f) * 0.2f));
                    planet.rareMineralRichness = Math.max(0,
                            Math.min(1, system.rareMineralRichness + (random.nextFloat() - 0.5f) * 0.2f));
                    planet.nobleGasRichness = Math.max(0,
                            Math.min(1, system.nobleGasRichness + (random.nextFloat() - 0.5f) * 0.2f));

                    if (type == Planet.PlanetType.GAS_GIANT || type == Planet.PlanetType.ICE_GIANT) {
                        planet.nobleGasRichness = Math.max(0.8f, planet.nobleGasRichness);
                        planet.metalRichness *= 0.1f;
                    }

                    system.planets.add(planet);

                    // Initial Trade Capacity Estimate (Refined by EconomySystem later)
                    planet.tradeCapacity = (planet.population * 0.000001f) + (planet.industryLevel * 5.0f)
                            + ((planet.metalRichness + planet.rareMineralRichness + planet.nobleGasRichness) * 10.0f);
                }

                sector.setStarSystem(system);
                placedCount++;
            }
        }

        // Regions
        List<Region> regions = new ArrayList<>();
        mergeVoidSectors(regions, validSectors, gridSize, sectorGrid);

        // Nebula Generation
        for (Region region : regions) {
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
                    float baseRichness = 0.1f + ((minDist - 3) * 0.05f);
                    region.nobleGasRichness = Math.min(0.3f * properties.resourceRichnessMultiplier,
                            baseRichness * properties.resourceRichnessMultiplier);
                }
            }
        }

        // --- EMPIRE GENERATION ---

        // 1. Create Empire Definitions
        List<EmpireComponent> empireDefinitions = new ArrayList<>();
        // Player is Terrans
        empireDefinitions.add(new EmpireComponent("United Terran Federation", Color.BLUE));
        empireDefinitions.add(new EmpireComponent("Crimson Pact", Color.RED));
        empireDefinitions.add(new EmpireComponent("Verdant Union", Color.GREEN));
        empireDefinitions.add(new EmpireComponent("Golden Syndicate", Color.YELLOW));

        // 2. Select Home Systems
        List<Sector> homeSectors = new ArrayList<>();
        List<Sector> potentialHomeSectors = new ArrayList<>();

        for (Sector s : validSectors) {
            if (s.hasStarSystem() && s.getStarSystem().habitableCount >= 1) {
                potentialHomeSectors.add(s);
            }
        }

        java.util.Collections.shuffle(potentialHomeSectors, random);

        Map<EmpireComponent, Sector> empireHomeMap = new HashMap<>();

        for (EmpireComponent empireDef : empireDefinitions) {
            // Find a valid home sector that is far enough from others
            Sector chosen = null;
            for (Sector candidate : potentialHomeSectors) {
                if (homeSectors.contains(candidate))
                    continue;

                boolean farEnough = true;
                for (Sector existing : homeSectors) {
                    float dist = Vector2.dst(candidate.x, candidate.y, existing.x, existing.y);
                    if (dist < gridSize / 4f) { // Maintain some distance
                        farEnough = false;
                        break;
                    }
                }

                if (farEnough) {
                    chosen = candidate;
                    break;
                }
            }

            // If strict distancing failed, just pick the next available valid one
            // (fallback for dense/small maps)
            if (chosen == null && !potentialHomeSectors.isEmpty()) {
                for (Sector candidate : potentialHomeSectors) {
                    if (!homeSectors.contains(candidate)) {
                        chosen = candidate;
                        break;
                    }
                }
            }

            if (chosen != null) {
                homeSectors.add(chosen);
                empireHomeMap.put(empireDef, chosen);

                // Boost home system
                StarSystem sys = chosen.getStarSystem();
                sys.habitableCount = Math.max(sys.habitableCount, 2);

                // Find best planet to populate
                Planet bestPlanet = null;
                for (Planet p : sys.planets) {
                    if (bestPlanet == null || p.quality > bestPlanet.quality) {
                        bestPlanet = p;
                    }
                }

                if (bestPlanet != null) {
                    bestPlanet.population = 10_000_000_000L;
                    bestPlanet.description = "Capital World of " + empireDef.name;
                }
            }
        }

        // --- Conversion to ECS Entities ---
        PooledEngine engine = galaxy.getEngine();
        Map<Region, Entity> regionEntityMap = new HashMap<>();

        for (Region region : regions) {
            Entity regionEntity = engine.createEntity();

            // Region Name
            String rName = region.isNebula ? "Nebula " + region.hashCode() : "Region " + region.hashCode();
            regionEntity.add(new NameComponent(rName));

            // Region Type
            RegionTypeComponent.RegionType type = RegionTypeComponent.RegionType.VOID;
            if (region.isNebula)
                type = RegionTypeComponent.RegionType.NEBULA;
            else if (hasStarSystem(region))
                type = RegionTypeComponent.RegionType.STAR_SYSTEM;
            regionEntity.add(new RegionTypeComponent(type));

            // Resources & Stats
            ResourceComponent resComp = new ResourceComponent();
            if (region.isNebula) {
                resComp.nobleGasRichness = region.nobleGasRichness; // Nebula gas
            }
            // Calculate aggregate stats
            int sysCount = 0;
            int pCount = 0;
            int hCount = 0;
            int lCount = 0;
            float totalMetal = 0;
            float totalRare = 0;
            float totalGas = region.isNebula ? 0 : 0; // Don't double count if mixed? Assuming distinct types.

            for (Sector s : region.getSectors()) {
                if (s.hasStarSystem()) {
                    StarSystem sys = s.getStarSystem();
                    sysCount++;
                    pCount += sys.planetCount;
                    hCount += sys.habitableCount;
                    if (sys.hasLuxuryResources)
                        lCount++;
                    totalMetal += sys.metalRichness;
                    totalRare += sys.rareMineralRichness;
                    totalGas += sys.nobleGasRichness;
                }
            }
            resComp.systemCount = sysCount;
            resComp.planetCount = pCount;
            resComp.habitableCount = hCount;
            resComp.luxuryCount = lCount;
            if (sysCount > 0) {
                resComp.metalRichness = totalMetal / sysCount;
                resComp.rareMineralRichness = totalRare / sysCount;
                resComp.nobleGasRichness += (totalGas / sysCount);
            }
            regionEntity.add(resComp);

            // Prepare Sector List Component
            SectorListComponent sectorListComp = new SectorListComponent();
            regionEntity.add(sectorListComp);

            engine.addEntity(regionEntity);
            regionEntityMap.put(region, regionEntity);
        }

        // Create Empire Entities
        Map<EmpireComponent, Entity> empireEntityMap = new HashMap<>();
        for (Map.Entry<EmpireComponent, Sector> entry : empireHomeMap.entrySet()) {
            EmpireComponent empComp = entry.getKey();
            Sector homeSector = entry.getValue();

            Entity empireEnt = engine.createEntity();

            // Initialize resources
            empComp.credits = 1000;
            empComp.metals = 500;
            empComp.rareMinerals = 100;
            empComp.nobleGases = 50;
            empComp.luxuryResources = 10;

            empireEnt.add(empComp);

            // Assign Player to Terran Federation
            if (empComp.name.equals("United Terran Federation")) {
                empireEnt.add(new PlayerComponent());
            }

            engine.addEntity(empireEnt);
            empireEntityMap.put(empComp, empireEnt);
        }

        // Create Sector Entities
        for (Sector sector : validSectors) {
            Entity sectorEntity = engine.createEntity();

            sectorEntity.add(new GridPositionComponent(sector.x, sector.y));
            sectorEntity.add(new GeometryComponent(sector.getVertices()));

            if (sector.hasStarSystem()) {
                StarSystem sys = sector.getStarSystem();
                StarSystemComponent starComp = new StarSystemComponent();
                starComp.name = sys.name;
                starComp.color = sys.color;
                starComp.planetCount = sys.planetCount;
                starComp.habitableCount = sys.habitableCount;
                starComp.hasLuxuryResources = sys.hasLuxuryResources;
                starComp.metalRichness = sys.metalRichness;
                starComp.rareMineralRichness = sys.rareMineralRichness;
                starComp.nobleGasRichness = sys.nobleGasRichness;
                starComp.planets.addAll(sys.planets); // Copy planets
                sectorEntity.add(starComp);

                // Check if this is a home system
                for (Map.Entry<EmpireComponent, Sector> entry : empireHomeMap.entrySet()) {
                    if (entry.getValue() == sector) {
                        EmpireComponent emp = entry.getKey();
                        Entity ownerEnt = empireEntityMap.get(emp);

                        // Add Owner Component
                        sectorEntity.add(new OwnerComponent(ownerEnt));

                        // Add to Empire's owned list
                        emp.ownedSystems.add(sectorEntity);

                        // Color the system directly for now (optional override)
                        starComp.color.set(emp.color);

                        break;
                    }
                }
            }

            // Link to Region
            Region parentRegion = sector.getRegion();
            if (parentRegion != null && regionEntityMap.containsKey(parentRegion)) {
                Entity regionEntity = regionEntityMap.get(parentRegion);
                sectorEntity.add(new ParentRegionComponent(regionEntity));

                // Add to region's list
                regionEntity.getComponent(SectorListComponent.class).sectors.add(sectorEntity);
            }

            engine.addEntity(sectorEntity);
            galaxy.registerSector(sectorEntity, sector.x, sector.y);
        }

        return galaxy;
    }

    // Updated Helper Methods to use DTO Lists instead of Galaxy

    private void mergeVoidSectors(List<Region> regions, List<Sector> sectors, int gridSize, Sector[][] sectorGrid) {
        // 1. Calculate Distances
        calculateDistances(sectors, gridSize, sectorGrid);

        // 2. Initialize Regions
        for (Sector sector : sectors) {
            Region region = new Region();
            region.addSector(sector);
            sector.setRegion(region);
            regions.add(region);
        }

        // 3. Merging Loop
        int pass = 0;
        while (true) {
            pass++;
            List<Region> voidRegions = new ArrayList<>();
            for (Region r : regions) {
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

                if (!region.canMerge())
                    continue;

                Region neighbor = findMergeableNeighbor(region, gridSize, sectorGrid, voidRegions, consumed);

                if (neighbor != null) {
                    performRegionMerge(regions, region, neighbor);
                    region.incrementMergeCount();
                    consumed.add(region);
                    consumed.add(neighbor);
                    mergesThisPass++;
                }
            }

            if (mergesThisPass == 0)
                break;
        }
    }

    private void calculateDistances(List<Sector> sectors, int gridSize, Sector[][] sectorGrid) {
        List<Sector> queue = new ArrayList<>();

        for (Sector s : sectors) {
            if (s.hasStarSystem()) {
                s.setDistanceToStar(0);
                queue.add(s);
            } else {
                s.setDistanceToStar(Integer.MAX_VALUE);
            }
        }

        int head = 0;
        while (head < queue.size()) {
            Sector current = queue.get(head++);
            int dist = current.getDistanceToStar();

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
            int[][] offsets = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
            for (int[] off : offsets) {
                int nx = s.x + off[0];
                int ny = s.y + off[1];

                if (nx >= 0 && nx < gridSize && ny >= 0 && ny < gridSize) {
                    Sector neighborSector = sectorGrid[nx][ny];
                    if (neighborSector != null) {
                        Region nr = neighborSector.getRegion();
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

    private void performRegionMerge(List<Region> regions, Region target, Region source) {
        for (Sector s : source.getSectors()) {
            target.addSector(s);
            s.setRegion(target);
        }
        regions.remove(source);
    }

    private boolean isVoid(Sector sector, int gridSize, Sector[][] sectorGrid) {
        if (sector.hasStarSystem())
            return false;
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

    private boolean hasStarSystem(Region region) {
        for (Sector s : region.getSectors()) {
            if (s.hasStarSystem())
                return true;
        }
        return false;
    }

    private String generateName() {
        return "System " + random.nextInt(1000);
    }

    private Color generateColor() {
        return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f);
    }
}
