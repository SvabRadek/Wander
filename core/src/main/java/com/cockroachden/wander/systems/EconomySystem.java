package com.cockroachden.wander.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IntervalSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.cockroachden.wander.components.*;
import com.cockroachden.wander.map.Galaxy;

import java.util.*;

public class EconomySystem extends IntervalSystem {
    private final Galaxy galaxy;

    public EconomySystem(Galaxy galaxy) {
        super(2.0f); // Run every 2 seconds
        this.galaxy = galaxy;
    }

    @Override
    protected void updateInterval() {
        ImmutableArray<Entity> empires = getEngine().getEntitiesFor(Family.all(EmpireComponent.class).get());

        for (Entity empire : empires) {
            validateRoutes(empire);
            updateTradeRoutes(empire);
        }
    }

    private void updateTradeRoutes(Entity empire) {
        EmpireComponent empireComp = empire.getComponent(EmpireComponent.class);
        List<Entity> owned = empireComp.ownedSystems;

        if (owned.size() < 2)
            return;

        // Iterate all pairs
        for (int i = 0; i < owned.size(); i++) {
            for (int j = i + 1; j < owned.size(); j++) {
                Entity s1 = owned.get(i);
                Entity s2 = owned.get(j);

                // Check if route exists
                if (routeExists(s1, s2))
                    continue;

                // Create route
                List<Vector2> path = findPath(s1, s2, empire);
                if (path != null && !path.isEmpty()) {
                    createTradeRoute(s1, s2, path);
                }
            }
        }
    }

    private void validateRoutes(Entity empire) {
        ImmutableArray<Entity> routes = getEngine().getEntitiesFor(Family.all(TradeRouteComponent.class).get());
        List<Entity> invalidRoutes = new ArrayList<>();

        for (Entity route : routes) {
            TradeRouteComponent tr = route.getComponent(TradeRouteComponent.class);
            // Check if this route belongs to this empire (implicitly by checking source
            // ownership)
            OwnerComponent own = tr.source.getComponent(OwnerComponent.class);
            if (own != null && own.owner == empire) {
                // Validate path
                if (tr.path != null) {
                    for (Vector2 point : tr.path) {
                        int x = (int) (point.x / 32f); // TODO: Remove hardcoded sector size or pass it
                        int y = (int) (point.y / 32f);
                        // Actually path is center points.
                        // Better: Check every sector/node the path was built from?
                        // Pathfinding returns Vector2 centers.
                        // So getting sector at that point is fine.

                        Entity sector = galaxy.getSectorEntity((int) point.x, (int) point.y);
                        if (isProhibited(sector, empire)) {
                            invalidRoutes.add(route);
                            break;
                        }
                    }
                }
            }
        }

        for (Entity r : invalidRoutes) {
            getEngine().removeEntity(r);
        }
    }

    private boolean isProhibited(Entity sector, Entity empire) {
        if (sector == null)
            return false;
        ParentRegionComponent parentReg = sector.getComponent(ParentRegionComponent.class);
        if (parentReg != null) {
            Entity region = parentReg.regionEntity;
            ProhibitedAccessComponent pac = region.getComponent(ProhibitedAccessComponent.class);
            if (pac != null && pac.blockedByEmpires.contains(empire)) {
                return true;
            }
        }
        return false;
    }

    private boolean routeExists(Entity s1, Entity s2) {
        ImmutableArray<Entity> routes = getEngine().getEntitiesFor(Family.all(TradeRouteComponent.class).get());
        for (Entity route : routes) {
            TradeRouteComponent tr = route.getComponent(TradeRouteComponent.class);
            if ((tr.source == s1 && tr.target == s2) || (tr.source == s2 && tr.target == s1)) {
                return true;
            }
        }
        return false;
    }

    private void createTradeRoute(Entity s1, Entity s2, List<Vector2> path) {
        Entity route = getEngine().createEntity();
        TradeRouteComponent tr = new TradeRouteComponent();
        tr.source = s1;
        tr.target = s2;
        tr.path = path;
        tr.value = 10f; // TODO: Calc value based on system richness
        route.add(tr);
        getEngine().addEntity(route);
    }

    // A* Pathfinding
    private List<Vector2> findPath(Entity start, Entity end, Entity empire) {
        GridPositionComponent startPos = start.getComponent(GridPositionComponent.class);
        GridPositionComponent endPos = end.getComponent(GridPositionComponent.class);

        if (startPos == null || endPos == null)
            return null;

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startPos.x, startPos.y, 0, dist(startPos.x, startPos.y, endPos.x, endPos.y), null);
        openSet.add(startNode);
        allNodes.put(key(startNode.x, startNode.y), startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.x == endPos.x && current.y == endPos.y) {
                return reconstructPath(current);
            }

            int[][] offsets = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
            for (int[] off : offsets) {
                int nx = current.x + off[0];
                int ny = current.y + off[1];

                Entity neighbor = galaxy.getSectorEntity(nx, ny);
                if (neighbor == null)
                    continue;

                if (isProhibited(neighbor, empire)) {
                    continue;
                }

                // Cost calculation
                float moveCost = 1.0f;

                float newG = current.g + moveCost;

                Node neighborNode = allNodes.get(key(nx, ny));
                if (neighborNode == null || newG < neighborNode.g) {
                    if (neighborNode == null) {
                        neighborNode = new Node(nx, ny, newG, dist(nx, ny, endPos.x, endPos.y), current);
                        allNodes.put(key(nx, ny), neighborNode);
                        openSet.add(neighborNode);
                    } else if (newG < neighborNode.g) {
                        openSet.remove(neighborNode);
                        neighborNode.g = newG;
                        neighborNode.parent = current;
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        return null;
    }

    private List<Vector2> reconstructPath(Node node) {
        List<Vector2> path = new ArrayList<>();
        Node current = node;
        while (current != null) {
            Entity sector = galaxy.getSectorEntity(current.x, current.y);
            if (sector != null) {
                GeometryComponent geo = sector.getComponent(GeometryComponent.class);
                if (geo != null) {
                    path.add(new Vector2(geo.center));
                }
            }
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private float dist(int x1, int y1, int x2, int y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    private String key(int x, int y) {
        return x + "," + y;
    }

    private static class Node implements Comparable<Node> {
        int x, y;
        float g, h;
        Node parent;

        public Node(int x, int y, float g, float h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        public float f() {
            return g + h;
        }

        @Override
        public int compareTo(Node o) {
            return Float.compare(this.f(), o.f());
        }
    }
}
