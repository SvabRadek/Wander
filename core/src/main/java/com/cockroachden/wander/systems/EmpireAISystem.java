package com.cockroachden.wander.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.cockroachden.wander.components.*;
import com.cockroachden.wander.map.Galaxy;

import java.util.Random;

public class EmpireAISystem extends EntitySystem {
    private final Galaxy galaxy;
    private final Random random = new Random();

    private float timer = 0f;
    private final float INTERVAL = 10.0f;

    public EmpireAISystem(Galaxy galaxy) {
        this.galaxy = galaxy;
    }

    @Override
    public void update(float deltaTime) {
        ImmutableArray<Entity> empires = getEngine().getEntitiesFor(Family.all(EmpireComponent.class).get());

        // 1. Remove Delta Calculation as it is now done in EconomySystem
        // However, EconomySystem runs every 5 seconds. GameScreen UI might want
        // immediate feedback or just last known delta.
        // Since deltas are only updated every 5s in EconomySystem, let's leave them be.
        // We can remove this block entirely.

        // 2. Check Timer for actual transaction
        timer += deltaTime;
        if (timer >= INTERVAL) {
            timer -= INTERVAL;

            for (Entity empire : empires) {
                EmpireComponent empireComp = empire.getComponent(EmpireComponent.class);

                // Expansion
                attemptExpansion(empire, empireComp);
            }
        }
    }

    private void attemptExpansion(Entity empireEnt, EmpireComponent empire) {
        if (empire.credits < 100)
            return;

        // Try to expand from a random owned system
        if (empire.ownedSystems.isEmpty())
            return;

        // Try up to 5 times to find a valid expansion target to avoid getting stuck on
        // internal systems
        for (int i = 0; i < 5; i++) {
            Entity sourceSystem = empire.ownedSystems.get(random.nextInt(empire.ownedSystems.size()));
            GridPositionComponent pos = sourceSystem.getComponent(GridPositionComponent.class);

            if (pos != null) {
                // Check neighbors
                int[][] offsets = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
                // Shuffle offsets
                shuffleArray(offsets);

                for (int[] offset : offsets) {
                    Entity neighbor = galaxy.getSectorEntity(pos.x + offset[0], pos.y + offset[1]);

                    if (neighbor != null) {
                        // Check if it's a star system
                        StarSystemComponent star = neighbor.getComponent(StarSystemComponent.class);
                        if (star != null) {
                            // Check if unowned
                            if (neighbor.getComponent(OwnerComponent.class) == null) {
                                // Claim it!
                                neighbor.add(new OwnerComponent(empireEnt));
                                empire.ownedSystems.add(neighbor);
                                empire.credits -= 100;

                                // Color it
                                star.color.set(empire.color);

                                return; // Expanded once, stop for this tick
                            }
                        }
                    }
                }
            }
        }
    }

    private void shuffleArray(int[][] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int[] temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}
