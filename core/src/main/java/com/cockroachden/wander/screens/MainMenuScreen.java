package com.cockroachden.wander.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cockroachden.wander.Wander;
import com.cockroachden.wander.map.Galaxy;
import com.cockroachden.wander.map.GalaxyGenerator;
import com.cockroachden.wander.map.GalaxyProperties;

public class MainMenuScreen extends ScreenAdapter {
    private final Wander game;
    private final Stage stage;
    private final Skin skin;

    public MainMenuScreen(Wander game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Load default skin
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        Label titleLabel = new Label("Wander - Galaxy Generator", skin);
        table.add(titleLabel).padBottom(20).colspan(2);
        table.row();

        // Star Count
        table.add(new Label("Target Stars:", skin)).right().pad(5);
        final TextField starCountField = new TextField("300", skin);
        starCountField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        table.add(starCountField).width(100).left().pad(5);
        table.row();

        // Density
        table.add(new Label("Density (0.01 - 0.5):", skin)).right().pad(5);
        final TextField densityField = new TextField("0.05", skin);
        table.add(densityField).width(100).left().pad(5);
        table.row();

        // Seed
        table.add(new Label("Seed (Optional):", skin)).right().pad(5);
        final TextField seedField = new TextField("", skin);
        table.add(seedField).width(200).left().pad(5);
        table.row();

        // Resource Richness
        table.add(new Label("Resource Richness (0.1 - 5.0):", skin)).right().pad(5);
        final TextField richnessField = new TextField("1.0", skin);
        table.add(richnessField).width(100).left().pad(5);
        table.row();

        // Generate Button
        TextButton generateButton = new TextButton("Generate Galaxy", skin);
        table.add(generateButton).colspan(2).padTop(20).width(200).height(50);

        generateButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                generateGalaxy(starCountField.getText(), densityField.getText(), seedField.getText(),
                        richnessField.getText());
            }
        });
    }

    private void generateGalaxy(String starCountStr, String densityStr, String seedStr, String richnessStr) {
        int starCount = 300;
        try {
            starCount = Integer.parseInt(starCountStr);
        } catch (NumberFormatException e) {
            // Ignore, use default
        }

        float density = 0.05f;
        try {
            density = Float.parseFloat(densityStr);
        } catch (NumberFormatException e) {
            // Ignore
        }

        float richness = 1.0f;
        try {
            richness = Float.parseFloat(richnessStr);
        } catch (NumberFormatException e) {
            // Ignore
        }

        long seed;
        if (seedStr == null || seedStr.trim().isEmpty()) {
            seed = System.currentTimeMillis();
        } else {
            try {
                seed = Long.parseLong(seedStr);
            } catch (NumberFormatException e) {
                seed = seedStr.hashCode();
            }
        }

        GalaxyProperties props = new GalaxyProperties(starCount, density, seed, richness);
        GalaxyGenerator generator = new GalaxyGenerator();
        Galaxy galaxy = generator.generate(props);

        game.setScreen(new GameScreen(galaxy));
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
