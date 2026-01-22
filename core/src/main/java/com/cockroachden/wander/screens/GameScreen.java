package com.cockroachden.wander.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cockroachden.wander.map.Galaxy;
import com.cockroachden.wander.map.Region; // Import Region
import com.cockroachden.wander.map.Sector;
import com.cockroachden.wander.map.StarSystem;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.math.Interpolation;

public class GameScreen extends ScreenAdapter {
    private final Galaxy galaxy;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;

    private final Stage stage;
    private final Skin skin;
    private final InputMultiplexer inputMultiplexer;

    private Region hoveredRegion;
    private Region selectedRegion;
    private RegionInfoPanel regionInfoPanel;

    private static final float SECTOR_SIZE = 32f;

    public GameScreen(Galaxy galaxy) {
        this.galaxy = galaxy;
        this.shapeRenderer = new ShapeRenderer();
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // UI Setup
        this.stage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        this.regionInfoPanel = new RegionInfoPanel(skin);
        stage.addActor(regionInfoPanel);

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
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        renderGrid();
        shapeRenderer.end();

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

        Sector hoveredSector = galaxy.getSectorAt(mousePos.x, mousePos.y, SECTOR_SIZE);
        if (hoveredSector != null) {
            hoveredRegion = hoveredSector.getRegion();
        } else {
            hoveredRegion = null;
        }
    }

    private void renderGrid() {
        // Pass 0: Nebula Backgrounds (Transparent)
        // We technically need to end the 'Line' batch from render() and start a filled
        // one.
        // render() calls: shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        // renderGrid(); shapeRenderer.end();
        // So we are inside a Line batch here. To draw filled transparent shapes, we
        // must interrupt.
        shapeRenderer.end();

        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Region region : galaxy.getRegions()) {
            if (region.isNebula) {
                // Magenta, 5% alpha -- Visual Refinement
                shapeRenderer.setColor(1f, 0f, 1f, 0.05f);
                renderRegionBackground(region);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        // Resume Line Batch for the rest of the method
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Pass 1: Draw all normal (non-highlighted) regions
        Gdx.gl.glLineWidth(1f);
        // Default Color for borders - revert to Dark Gray without nebula override
        shapeRenderer.setColor(Color.DARK_GRAY);

        for (Region region : galaxy.getRegions()) {
            if (region == hoveredRegion || region == selectedRegion)
                continue;

            renderRegionBorders(region);
        }

        // Flush batch to apply line width change for next pass
        shapeRenderer.end();

        // Pass 2: Draw Hovered Region
        if (hoveredRegion != null && hoveredRegion != selectedRegion) {
            Gdx.gl.glLineWidth(2f);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            if (hoveredRegion.isNebula)
                shapeRenderer.setColor(Color.MAGENTA);
            else
                shapeRenderer.setColor(Color.CYAN);
            renderRegionBorders(hoveredRegion);
            shapeRenderer.end();
        }

        // Pass 3: Draw Selected Region
        if (selectedRegion != null) {
            Gdx.gl.glLineWidth(3f);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.GOLD);
            renderRegionBorders(selectedRegion);
            shapeRenderer.end(); // End selected pass
        }

        // Restore state for next external calls (stars etc)
        Gdx.gl.glLineWidth(1f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
    }

    private void renderRegionBackground(Region region) {
        // Draw filled triangles for each sector to fill the region shape
        for (Sector s : region.getSectors()) {
            float[] v = s.getVertices();
            // Assuming Sector vertices are ordered
            // v has 8 elements (x1, y1, x2, y2, x3, y3, x4, y4)
            // Triangulate quad: (v0, v1, v2) and (v0, v2, v3)

            // indices in vertex array:
            // v0: 0,1
            // v1: 2,3
            // v2: 4,5
            // v3: 6,7

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

    private void renderRegionBorders(Region region) {
        for (Sector sector : region.getSectors()) {
            float[] v = sector.getVertices();
            float[] sv = new float[8];
            for (int i = 0; i < 8; i++)
                sv[i] = v[i] * SECTOR_SIZE;

            checkAndDrawEdge(sector, sv[0], sv[1], sv[2], sv[3], 0, -1);
            checkAndDrawEdge(sector, sv[2], sv[3], sv[4], sv[5], 1, 0);
            checkAndDrawEdge(sector, sv[4], sv[5], sv[6], sv[7], 0, 1);
            checkAndDrawEdge(sector, sv[6], sv[7], sv[0], sv[1], -1, 0);
        }
    }

    private void checkAndDrawEdge(Sector s, float x1, float y1, float x2, float y2, int dx, int dy) {
        Sector neighbor = galaxy.getSector(s.x + dx, s.y + dy);
        boolean draw = true;
        if (neighbor != null && neighbor.getRegion() == s.getRegion()) {
            draw = false;
        }
        if (draw) {
            shapeRenderer.line(x1, y1, x2, y2);
        }
    }

    private void renderStars() {
        for (com.cockroachden.wander.map.Sector sector : galaxy.getSectors()) {
            if (sector.hasStarSystem()) {
                StarSystem system = sector.getStarSystem();
                shapeRenderer.setColor(system.color);

                // Start at calculated center
                Vector2 center = sector.getCenter();
                float cx = center.x * SECTOR_SIZE;
                float cy = center.y * SECTOR_SIZE;

                shapeRenderer.circle(cx, cy, SECTOR_SIZE * 0.3f);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        stage.getViewport().update(width, height, true);
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
                if (hoveredRegion != null) {
                    selectRegion(hoveredRegion);
                    return true;
                } else {
                    deselectRegion();
                }
            }
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                deselectRegion();
                return true;
            }
            return false;
        }
    }

    private void selectRegion(Region region) {
        this.selectedRegion = region;
        regionInfoPanel.show(region);
    }

    private void deselectRegion() {
        this.selectedRegion = null;
        regionInfoPanel.hide();
    }

    private class RegionInfoPanel extends com.badlogic.gdx.scenes.scene2d.ui.Table {
        private final Label nameLabel;
        private final Label detailsLabel;

        public RegionInfoPanel(Skin skin) {
            super(skin);

            // Create a simple background texture programmatically
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                    com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 0.9f));
            pixmap.fill();
            com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
            com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable drawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                    new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
            this.setBackground(drawable);
            // Don't dispose pixmap immediately if we use it? Actually Texture copies data.
            // But to be safe in LibGDX, we usually dispose Pixmap after uploading to
            // Texture.
            pixmap.dispose();

            this.top().left();
            this.pad(20);

            nameLabel = new Label("Region Name", skin);
            detailsLabel = new Label("Details...", skin);

            // Ensure labels wrap if needed
            detailsLabel.setWrap(true);

            add(nameLabel).left().expandX().fillX().row();
            add(detailsLabel).left().expandX().fillX().padTop(10).row();

            // Initial position (off-screen left)
            this.setWidth(300);
            this.setHeight(Gdx.graphics.getHeight());
            this.setPosition(-getWidth(), 0);
        }

        public void show(Region region) {
            nameLabel.setText(
                    region.isNebula ? "Nebula Region " + region.hashCode() : "Star Region " + region.hashCode());

            String stats;
            if (region.isNebula) {
                stats = String.format("Type: Deep Space Nebula\nGas Richness: %.2f\nSectors: %d",
                        region.nobleGasRichness, region.getSectors().size());
            } else {
                // Calculate Stats
                int systemCount = 0;
                int totalPlanets = 0;
                int totalHabitable = 0;
                int luxuryCount = 0;
                float avgMetal = 0;
                float avgRare = 0;
                float avgGas = 0;

                StringBuilder sysList = new StringBuilder();

                for (Sector sector : region.getSectors()) {
                    if (sector.hasStarSystem()) {
                        StarSystem sys = sector.getStarSystem();
                        systemCount++;
                        totalPlanets += sys.planetCount;
                        totalHabitable += sys.habitableCount;
                        if (sys.hasLuxuryResources)
                            luxuryCount++;

                        avgMetal += sys.metalRichness;
                        avgRare += sys.rareMineralRichness;
                        avgGas += sys.nobleGasRichness;

                        sysList.append("- ").append(sys.name).append("\n");
                        sysList.append("  ").append(sys.planetCount).append(" Planets (").append(sys.habitableCount)
                                .append(" Hab)\n");
                    }
                }

                if (systemCount > 0) {
                    avgMetal /= systemCount;
                    avgRare /= systemCount;
                    avgGas /= systemCount;
                }

                stats = String.format(
                        "Systems: %d\nPlanets: %d (%d Habitable)\nLuxuries: %d\n\nAvg Resources:\nMetal: %.2f\nRare: %.2f\nGas: %.2f\n\n%s",
                        systemCount, totalPlanets, totalHabitable, luxuryCount, avgMetal, avgRare, avgGas,
                        sysList.toString());
            }

            detailsLabel.setText(stats);

            this.clearActions();
            this.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));

            System.out.println("Selected Region: " + region);
        }

        public void hide() {
            this.clearActions();
            this.addAction(Actions.moveTo(-getWidth(), 0, 0.3f, Interpolation.pow2In));
        }
    }
}
