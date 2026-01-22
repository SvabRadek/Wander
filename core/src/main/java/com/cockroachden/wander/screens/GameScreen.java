package com.cockroachden.wander.screens;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cockroachden.wander.components.*;
import com.cockroachden.wander.map.Galaxy;
import com.badlogic.gdx.graphics.GL20;
import com.cockroachden.wander.components.StarSystemComponent;

public class GameScreen extends ScreenAdapter {
    private final Galaxy galaxy;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;

    private final Stage stage;
    private final Skin skin;
    private final InputMultiplexer inputMultiplexer;

    // ECS Mappers
    private final ComponentMapper<RegionTypeComponent> regionTypeMapper = ComponentMapper
            .getFor(RegionTypeComponent.class);
    private final ComponentMapper<NameComponent> nameMapper = ComponentMapper.getFor(NameComponent.class);
    private final ComponentMapper<ResourceComponent> resourceMapper = ComponentMapper.getFor(ResourceComponent.class);
    private final ComponentMapper<SectorListComponent> sectorListMapper = ComponentMapper
            .getFor(SectorListComponent.class);
    private final ComponentMapper<GridPositionComponent> gridPosMapper = ComponentMapper
            .getFor(GridPositionComponent.class);
    private final ComponentMapper<GeometryComponent> geometryMapper = ComponentMapper.getFor(GeometryComponent.class);
    private final ComponentMapper<StarSystemComponent> starSystemMapper = ComponentMapper
            .getFor(StarSystemComponent.class);
    private final ComponentMapper<ParentRegionComponent> parentRegionMapper = ComponentMapper
            .getFor(ParentRegionComponent.class);
    private final ComponentMapper<OwnerComponent> ownerMapper = ComponentMapper.getFor(OwnerComponent.class);
    private final ComponentMapper<EmpireComponent> empireMapper = ComponentMapper.getFor(EmpireComponent.class);
    private final ComponentMapper<TradeRouteComponent> tradeRouteMapper = ComponentMapper
            .getFor(TradeRouteComponent.class);
    private final ComponentMapper<ProhibitedAccessComponent> prohibitedMapper = ComponentMapper
            .getFor(ProhibitedAccessComponent.class);
    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);

    private Entity hoveredRegionEntity;
    private Entity hoveredSectorEntity;
    private Entity selectedRegionEntity; // Can be Region or StarSystem
    private RegionInfoPanel regionInfoPanel;
    private TopPanel topPanel;

    private static final float SECTOR_SIZE = 32f;

    public GameScreen(Galaxy galaxy) {
        this.galaxy = galaxy;
        this.shapeRenderer = new ShapeRenderer();
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Systems
        galaxy.getEngine().addSystem(new com.cockroachden.wander.systems.EmpireAISystem(galaxy));
        galaxy.getEngine().addSystem(new com.cockroachden.wander.systems.EconomySystem(galaxy));

        // UI Setup
        this.stage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        this.regionInfoPanel = new RegionInfoPanel(skin);
        stage.addActor(regionInfoPanel);

        this.topPanel = new TopPanel(skin);
        stage.addActor(topPanel);

        // Input Setup
        this.inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(new GameInputProcessor());
        Gdx.input.setInputProcessor(inputMultiplexer);

        // Center camera on galaxy
        float galaxyWidth = galaxy.getWidth() * SECTOR_SIZE;
        float galaxyHeight = galaxy.getHeight() * SECTOR_SIZE;
        camera.position.set(galaxyWidth / 2, galaxyHeight / 2, 0);
    }

    @Override
    public void render(float delta) {
        handleInput(delta);

        ScreenUtils.clear(0f, 0f, 0.1f, 1f);

        // Update ECS
        galaxy.getEngine().update(delta);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        renderGrid();
        shapeRenderer.end();

        renderTradeRoutes();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderStars();
        shapeRenderer.end();

        // Draw UI
        stage.act(delta);
        stage.draw();
    }

    private void handleInput(float delta) {
        float speed = 500f * delta;
        float zoomSpeed = 2f * delta;

        // Camera Controls
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-speed * camera.zoom, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(speed * camera.zoom, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, speed * camera.zoom);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -speed * camera.zoom);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom += zoomSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            camera.zoom -= zoomSpeed;
        }

        camera.zoom = Math.max(0.1f, Math.min(camera.zoom, 5f));

        // Mouse Hover Logic
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        int gx = (int) (mousePos.x / SECTOR_SIZE);
        int gy = (int) (mousePos.y / SECTOR_SIZE);

        Entity hoveredSector = galaxy.getSectorEntity(gx, gy);
        this.hoveredSectorEntity = hoveredSector;

        if (hoveredSector != null && parentRegionMapper.has(hoveredSector)) {
            hoveredRegionEntity = parentRegionMapper.get(hoveredSector).regionEntity;
        } else {
            hoveredRegionEntity = null;
        }
    }

    private void renderGrid() {
        shapeRenderer.end();
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        ImmutableArray<Entity> regions = galaxy.getEngine()
                .getEntitiesFor(Family.all(SectorListComponent.class, RegionTypeComponent.class).get());

        // Pass 0: Backgrounds
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Entity region : regions) {
            RegionTypeComponent typeComp = regionTypeMapper.get(region);
            SectorListComponent sl = sectorListMapper.get(region);

            Color regionColor = null;

            // Check for Empire Ownership
            if (sl != null) {
                for (Entity sector : sl.sectors) {
                    if (ownerMapper.has(sector)) {
                        Entity empire = ownerMapper.get(sector).owner;
                        if (empireMapper.has(empire)) {
                            regionColor = empireMapper.get(empire).color;
                            break;
                        }
                    }
                }
            }

            if (regionColor != null) {
                shapeRenderer.setColor(regionColor.r, regionColor.g, regionColor.b, 0.2f); // 20% opacity for empire
                renderRegionBackground(region);
            } else if (typeComp.type == RegionTypeComponent.RegionType.NEBULA) {
                shapeRenderer.setColor(1f, 0f, 1f, 0.05f);
                renderRegionBackground(region);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(1f);
        shapeRenderer.setColor(Color.DARK_GRAY);

        // Pass 1: Borders
        for (Entity region : regions) {
            if (region == hoveredRegionEntity || region == selectedRegionEntity)
                continue;
            renderRegionBorders(region);
        }
        shapeRenderer.end();

        // Pass 2: Hover
        if (hoveredRegionEntity != null && hoveredRegionEntity != selectedRegionEntity) {
            Gdx.gl.glLineWidth(2f);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.CYAN);
            renderRegionBorders(hoveredRegionEntity);
            shapeRenderer.end();
        }

        // Pass 3: Selected
        if (selectedRegionEntity != null) {
            Gdx.gl.glLineWidth(3f);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            if (starSystemMapper.has(selectedRegionEntity)) {
                shapeRenderer.setColor(Color.LIME); // Different color for star selection
                // We don't render border for single star sector usually, but let's highlight
                // its parent region or just the sector?
                // For now, let's just highlight the parent region too but in a distinct way to
                // show context?
                // Actually, if we selected a star, let's keep the Region Highlighted as GOLD
                // but maybe flash the star?
                // Or, if selectedRegionEntity IS the star sector, we need to handle that.

                // Render Parent Region as Gold
                if (parentRegionMapper.has(selectedRegionEntity)) {
                    Entity parent = parentRegionMapper.get(selectedRegionEntity).regionEntity;
                    renderRegionBorders(parent);
                }
            } else {
                shapeRenderer.setColor(Color.GOLD);
                renderRegionBorders(selectedRegionEntity);
            }
            shapeRenderer.end();
        }

        Gdx.gl.glLineWidth(1f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
    }

    private void renderRegionBackground(Entity region) {
        SectorListComponent sl = sectorListMapper.get(region);
        if (sl == null)
            return;

        for (Entity sector : sl.sectors) {
            GeometryComponent geo = geometryMapper.get(sector);
            if (geo != null) {
                float[] v = geo.vertices;
                float x1 = v[0] * SECTOR_SIZE;
                float y1 = v[1] * SECTOR_SIZE;
                float x2 = v[2] * SECTOR_SIZE;
                float y2 = v[3] * SECTOR_SIZE;
                float x3 = v[4] * SECTOR_SIZE;
                float y3 = v[5] * SECTOR_SIZE;
                float x4 = v[6] * SECTOR_SIZE;
                float y4 = v[7] * SECTOR_SIZE;

                shapeRenderer.triangle(x1, y1, x2, y2, x3, y3);
                shapeRenderer.triangle(x1, y1, x3, y3, x4, y4);
            }
        }
    }

    private void renderRegionBorders(Entity region) {
        SectorListComponent sl = sectorListMapper.get(region);
        if (sl == null)
            return;

        for (Entity sector : sl.sectors) {
            GeometryComponent geo = geometryMapper.get(sector);
            GridPositionComponent pos = gridPosMapper.get(sector);

            if (geo != null && pos != null) {
                float[] v = geo.vertices;
                float[] sv = new float[8];
                for (int i = 0; i < 8; i++)
                    sv[i] = v[i] * SECTOR_SIZE;

                checkAndDrawEdge(region, pos.x, pos.y, sv[0], sv[1], sv[2], sv[3], 0, -1);
                checkAndDrawEdge(region, pos.x, pos.y, sv[2], sv[3], sv[4], sv[5], 1, 0);
                checkAndDrawEdge(region, pos.x, pos.y, sv[4], sv[5], sv[6], sv[7], 0, 1);
                checkAndDrawEdge(region, pos.x, pos.y, sv[6], sv[7], sv[0], sv[1], -1, 0);
            }
        }
    }

    private void checkAndDrawEdge(Entity currentRegion, int x, int y, float x1, float y1, float x2, float y2, int dx,
            int dy) {
        Entity neighborSector = galaxy.getSectorEntity(x + dx, y + dy);
        boolean draw = true;

        if (neighborSector != null && parentRegionMapper.has(neighborSector)) {
            Entity neighborRegion = parentRegionMapper.get(neighborSector).regionEntity;
            if (neighborRegion == currentRegion) {
                draw = false;
            }
        }

        if (draw) {
            shapeRenderer.line(x1, y1, x2, y2);
        }
    }

    private void renderTradeRoutes() {
        ImmutableArray<Entity> routes = galaxy.getEngine().getEntitiesFor(Family.all(TradeRouteComponent.class).get());
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.3f); // White lines, semi-transparent

        for (Entity routeEntity : routes) {
            TradeRouteComponent route = tradeRouteMapper.get(routeEntity);
            if (route.path != null && route.path.size() > 1) {
                for (int i = 0; i < route.path.size() - 1; i++) {
                    com.badlogic.gdx.math.Vector2 p1 = route.path.get(i);
                    com.badlogic.gdx.math.Vector2 p2 = route.path.get(i + 1);

                    shapeRenderer.line(p1.x * SECTOR_SIZE, p1.y * SECTOR_SIZE, p2.x * SECTOR_SIZE, p2.y * SECTOR_SIZE);
                }
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    private void renderStars() {
        ImmutableArray<Entity> sectors = galaxy.getEngine().getEntitiesFor(
                Family.all(GridPositionComponent.class, GeometryComponent.class, StarSystemComponent.class).get());

        for (Entity sector : sectors) {
            StarSystemComponent star = starSystemMapper.get(sector);
            GeometryComponent geo = geometryMapper.get(sector);

            shapeRenderer.setColor(star.color);
            float cx = geo.center.x * SECTOR_SIZE;
            float cy = geo.center.y * SECTOR_SIZE;

            // Highlight selected star
            if (sector == selectedRegionEntity) {
                shapeRenderer.setColor(Color.WHITE);
                shapeRenderer.circle(cx, cy, SECTOR_SIZE * 0.45f); // Larger white halo
                shapeRenderer.setColor(star.color);
            }

            shapeRenderer.circle(cx, cy, SECTOR_SIZE * 0.3f);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        stage.getViewport().update(width, height, true);
        camera.update();
        stage.getViewport().update(width, height, true);
        regionInfoPanel.setHeight(height);

        topPanel.setWidth(width);
        topPanel.setPosition(0, height - topPanel.getHeight());

        regionInfoPanel.setHeight(height - topPanel.getHeight());
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        stage.dispose();
    }

    // Inner Classes

    private class GameInputProcessor extends com.badlogic.gdx.InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.LEFT) {
                // Priority: Star System -> Region
                if (hoveredSectorEntity != null && starSystemMapper.has(hoveredSectorEntity)) {
                    selectEntity(hoveredSectorEntity);
                    return true;
                } else if (hoveredRegionEntity != null) {
                    selectEntity(hoveredRegionEntity);
                    return true;
                } else {
                    deselectEntity();
                }
            }
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                deselectEntity();
                return true;
            }
            return false;
        }
    }

    private void selectEntity(Entity entity) {
        this.selectedRegionEntity = entity; // Reuse this field for selected entity (Region or Sector)
        regionInfoPanel.show(entity);
    }

    private void deselectEntity() {
        this.selectedRegionEntity = null;
        regionInfoPanel.hide();
    }

    private class RegionInfoPanel extends Table {
        private final Label nameLabel;
        private final Label typeLabel;
        private final Table contentTable; // Dynamic content
        private final TextButton targetButton;

        public RegionInfoPanel(Skin skin) {
            super(skin);

            // Background
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                    com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 0.95f));
            pixmap.fill();
            com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
            com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable drawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                    new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
            this.setBackground(drawable);
            pixmap.dispose();

            this.top().left();
            this.pad(20);

            // Header Table
            Table headerTable = new Table(skin);
            nameLabel = new Label("Name", skin);
            nameLabel.setFontScale(1.2f);

            targetButton = new TextButton("Target", skin);
            targetButton.addListener(new ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    if (selectedRegionEntity != null) {
                        centerCameraOnEntity(selectedRegionEntity);
                    }
                }
            });

            headerTable.add(nameLabel).expandX().left();
            headerTable.add(targetButton).right();

            // Type Label
            typeLabel = new Label("Type", skin);
            typeLabel.setColor(Color.LIGHT_GRAY);

            // Content Table (Replaces detailsLabel)
            contentTable = new Table(skin);
            contentTable.top().left();

            // Add to Main Table
            add(headerTable).expandX().fillX().padBottom(5).row();
            add(typeLabel).left().padBottom(15).row();
            add(contentTable).left().expandX().fillX().top().row(); // Add content table

            add().expand().fill();

            this.setWidth(350);
            this.setHeight(Gdx.graphics.getHeight());
            this.setPosition(-getWidth(), 0);
        }

        public void show(Entity entity) {
            // Determine if Region or Star System
            if (starSystemMapper.has(entity)) {
                showStarSystem(entity);
            } else {
                showRegion(entity);
            }

            this.clearActions();
            this.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));
        }

        private void showStarSystem(Entity entity) {
            StarSystemComponent star = starSystemMapper.get(entity);
            nameLabel.setText(star.name);
            typeLabel.setText("Star System");

            contentTable.clearChildren();

            // Summary
            contentTable.add(new Label(
                    String.format("Planets: %d\nHabitable: %d\n\n", star.planetCount, star.habitableCount), skin))
                    .left().row();
            contentTable.add(new Label("--- PLANETS ---", skin)).center().padBottom(10).row();

            // Player Check
            Entity playerEmpire = getPlayerEmpire();
            EmpireComponent playerComp = (playerEmpire != null) ? empireMapper.get(playerEmpire) : null;
            boolean systemOwnedByPlayer = ownerMapper.has(entity) && ownerMapper.get(entity).owner == playerEmpire;
            boolean systemUnowned = !ownerMapper.has(entity);

            for (com.cockroachden.wander.map.Planet p : star.planets) {
                Table planetTable = new Table(skin);
                planetTable.top().left();

                String info = p.name + "\n" + "Type: " + p.type + "\n";
                if (p.population > 0) {
                    info += String.format("Pop: %.2f B\n", p.population / 1_000_000_000f);
                    if (systemOwnedByPlayer && playerComp != null) {
                        float taxIncome = (p.population / 1_000_000f) * playerComp.taxRate;
                        info += String.format("Tax: %.1f%% (+%.0f)\n", playerComp.taxRate * 100f, taxIncome);
                    }
                } else {
                    info += "Uninhabited\n";
                }
                info += String.format("Q:%.0f%% M:%.1f R:%.1f G:%.1f", p.quality * 100f, p.metalRichness,
                        p.rareMineralRichness, p.nobleGasRichness);

                Label pLabel = new Label(info, skin);
                planetTable.add(pLabel).left().expandX();

                // Colonize Button
                if (p.population == 0 && p.quality > 0.3f) { // Colonizable
                    // Check ownership requirements (Must be unowned OR owned by player)
                    if (systemUnowned || systemOwnedByPlayer) {
                        TextButton colBtn = new TextButton("Colonize (500C, 100M)", skin); // Show cost
                        colBtn.addListener(new ClickListener() {
                            @Override
                            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                                if (playerComp != null) {
                                    // COST: 500 Credits, 100 Metals
                                    if (playerComp.credits >= 500 && playerComp.metals >= 100) {
                                        playerComp.credits -= 500;
                                        playerComp.metals -= 100;

                                        // Settle
                                        p.population = 100_000_000; // Start with 100M

                                        // Claim System if unowned
                                        if (!ownerMapper.has(entity)) {
                                            entity.add(new OwnerComponent(playerEmpire));
                                            playerComp.ownedSystems.add(entity);
                                            star.color.set(playerComp.color);
                                        }

                                        // Refresh UI
                                        show(entity);
                                    }
                                }
                            }
                        });
                        planetTable.add(colBtn).right().top().padLeft(10);
                    }
                }

                contentTable.add(planetTable).left().expandX().fillX().padBottom(15).row();
            }
        }

        private Entity getPlayerEmpire() {
            ImmutableArray<Entity> players = galaxy.getEngine()
                    .getEntitiesFor(Family.all(PlayerComponent.class, EmpireComponent.class).get());
            return (players.size() > 0) ? players.first() : null;
        }

        private void showRegion(Entity region) {
            // Check Components
            NameComponent nameComp = nameMapper.get(region);
            RegionTypeComponent typeComp = regionTypeMapper.get(region);
            ResourceComponent resComp = resourceMapper.get(region);

            nameLabel.setText(nameComp != null ? nameComp.name : "Unknown Region");

            contentTable.clearChildren();

            String typeText = "Unknown Sector";
            if (typeComp != null) {
                switch (typeComp.type) {
                    case NEBULA:
                        typeText = "Deep Space Nebula";
                        break;
                    case STAR_SYSTEM:
                        // ... existing logic ...
                        typeText = "Star System";
                        break;
                    case VOID:
                        typeText = "Void Sector";
                    default:
                        typeText = "Void Sector";
                        break;
                }
            }
            typeLabel.setText(typeText);

            String stats = "";
            if (resComp != null) {
                if (typeComp != null && typeComp.type == RegionTypeComponent.RegionType.NEBULA) {
                    SectorListComponent sl = sectorListMapper.get(region);
                    int sCount = (sl != null) ? sl.sectors.size() : 0;
                    stats = String.format("Gas Richness: %.2f\nSectors: %d",
                            resComp.nobleGasRichness, sCount);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("Metal:   %.2f\n", resComp.metalRichness));
                    sb.append(String.format("Rare:    %.2f\n", resComp.rareMineralRichness));
                    sb.append(String.format("Gas:     %.2f\n", resComp.nobleGasRichness));
                    sb.append("\n");
                    sb.append(String.format("Stars:     %d\n", resComp.systemCount));
                    sb.append(String.format("Planets:   %d\n", resComp.planetCount));
                    sb.append(String.format("Habitable: %d", resComp.habitableCount));
                    stats = sb.toString();
                }
            }

            contentTable.add(new Label(stats, skin)).left().row();

            // Prohibited Access Toggle (Only for Player)
            Entity playerEmpire = getPlayerEmpire();
            if (playerEmpire != null && typeComp != null && (typeComp.type == RegionTypeComponent.RegionType.NEBULA
                    || typeComp.type == RegionTypeComponent.RegionType.VOID)) {

                if (!prohibitedMapper.has(region)) {
                    region.add(new ProhibitedAccessComponent());
                }
                ProhibitedAccessComponent pac = prohibitedMapper.get(region);
                boolean isProhibited = pac.blockedByEmpires.contains(playerEmpire);

                TextButton toggleBtn = new TextButton(isProhibited ? "ALLOW Civilians" : "BLOCK Civilians", skin);
                toggleBtn.setColor(isProhibited ? Color.GREEN : Color.RED);

                toggleBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                        if (isProhibited) {
                            pac.blockedByEmpires.remove(playerEmpire);
                        } else {
                            pac.blockedByEmpires.add(playerEmpire);
                        }
                        showRegion(region); // Refresh UI
                    }
                });

                contentTable.add(toggleBtn).left().padTop(20).row();
                if (isProhibited) {
                    Label warning = new Label("Trade routes will avoid this region.", skin);
                    warning.setColor(Color.RED);
                    warning.setFontScale(0.8f);
                    contentTable.add(warning).left();
                }
            }
        }

        public void hide() {
            this.clearActions();
            this.addAction(Actions.moveTo(-getWidth(), 0, 0.3f, Interpolation.pow2In));
        }

        private void centerCameraOnEntity(Entity entity) {
            // If sector
            if (gridPosMapper.has(entity)) {
                GeometryComponent geo = geometryMapper.get(entity);
                if (geo != null) {
                    camera.position.set(geo.center.x * SECTOR_SIZE, geo.center.y * SECTOR_SIZE, 0);
                }
                return;
            }

            // If region
            SectorListComponent sl = sectorListMapper.get(entity);
            if (sl == null || sl.sectors.isEmpty())
                return;

            float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

            for (Entity s : sl.sectors) {
                GeometryComponent geo = geometryMapper.get(s);
                if (geo != null) {
                    for (int i = 0; i < 8; i += 2) {
                        float vx = geo.vertices[i];
                        float vy = geo.vertices[i + 1];
                        if (vx < minX)
                            minX = vx;
                        if (vx > maxX)
                            maxX = vx;
                        if (vy < minY)
                            minY = vy;
                        if (vy > maxY)
                            maxY = vy;
                    }
                }
            }

            float centerX = (minX + maxX) / 2f * SECTOR_SIZE;
            float centerY = (minY + maxY) / 2f * SECTOR_SIZE;

            camera.position.set(centerX, centerY, 0);
        }
    }

    private class TopPanel extends Table {
        private final Label empireNameLabel;
        private final Label creditsLabel;
        private final Label resourcesLabel;

        public TopPanel(Skin skin) {
            super(skin);

            // Background
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                    com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 0.9f));
            pixmap.fill();
            com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
            com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable drawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                    new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
            this.setBackground(drawable);
            pixmap.dispose();

            this.pad(10);
            this.defaults().space(20);

            empireNameLabel = new Label("Empire", skin);
            empireNameLabel.setFontScale(1.2f);

            creditsLabel = new Label("Credits: 0", skin);
            creditsLabel.setColor(Color.GOLD);

            resourcesLabel = new Label("M: 0 | R: 0 | G: 0 | L: 0", skin);

            add(empireNameLabel).expandX().left();
            add(creditsLabel);
            add(resourcesLabel).expandX().right();

            this.pack();
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            updateInfo();
        }

        private void updateInfo() {
            // Find Player Empire
            ImmutableArray<Entity> players = galaxy.getEngine()
                    .getEntitiesFor(Family.all(PlayerComponent.class, EmpireComponent.class).get());

            if (players.size() > 0) {
                Entity playerEmpire = players.first();
                EmpireComponent empire = empireMapper.get(playerEmpire);

                if (empire != null) {
                    empireNameLabel.setText(empire.name);
                    empireNameLabel.setColor(empire.color);

                    creditsLabel.setText(String.format("Credits: %.0f (+%.0f)", empire.credits, empire.deltaCredits));

                    resourcesLabel.setText(
                            String.format("M: %.0f (+%.0f) | R: %.0f (+%.0f) | G: %.0f (+%.0f) | L: %.0f (+%.0f)",
                                    empire.metals, empire.deltaMetals,
                                    empire.rareMinerals, empire.deltaRareMinerals,
                                    empire.nobleGases, empire.deltaNobleGases,
                                    empire.luxuryResources, empire.deltaLuxuryResources));
                }
            } else {
                empireNameLabel.setText("Spectator Mode");
                empireNameLabel.setColor(Color.GRAY);
                creditsLabel.setText("");
                resourcesLabel.setText("");
            }
        }
    }
}
