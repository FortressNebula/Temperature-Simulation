package com.nebula.tempsim;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.nebula.tempsim.math.Vector2i;
import com.nebula.tempsim.tile.Tile;
import com.nebula.tempsim.tile.TileGrid;

public class TemperatureSimulation extends ApplicationAdapter {
	// Major simulation constants
	static final Model SIMULATION_MODEL = Model.EQUALISE_WITH_COLDER_UNITS;
	public static final TileGrid GRID = new TileGrid();
	// Constants useful in rendering and traversing the grid
	static final int SQUARE_SIZE = 40;
	static final Vector2i[] NEIGHBOUR_OFFSETS = new Vector2i[] {
			new Vector2i( 0,  0),
			new Vector2i( 1,  0),
			new Vector2i( 0,  1),
			new Vector2i(-1,  0),
			new Vector2i( 0, -1)
	};
	// Constants for rendering
	static final Color BACKGROUND_COLOUR = new Color(0x1c1d1fff);
	static final float OUTLINE_PIXELS = 1;

	// Global vars controlling speed of simulation as well as painting tiles
	static float timeScale = 0.5f;
	static Tile.TileType brushType = Tile.TileType.NORMAL;

	// Assets
	SpriteBatch batch;
	Texture speedIndicator;
	Texture pauseIndicator;
	Texture tilemap;
	TextureRegion brush;

	// Whether the game is paused or unpaused
	boolean isRunning = false;

	// Rendering
	FrameBuffer main;
	ShaderProgram outlineShader;
	Vector2 outlineOffsets;
	
	@Override
	public void create () {
		// Rendering init
		batch = new SpriteBatch();
		main = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		outlineShader = new ShaderProgram(Gdx.files.internal("shaders/outline.vsh"), Gdx.files.internal("shaders/outline.fsh"));

		outlineOffsets = new Vector2(
				OUTLINE_PIXELS / Gdx.graphics.getWidth(),
				OUTLINE_PIXELS / Gdx.graphics.getHeight()
		);

		if (!outlineShader.isCompiled())
			throw new GdxRuntimeException("Outline shader failed to compile!" + outlineShader.getLog());

		// Tile map containing the various tiles
		tilemap = new Texture("tilemap.png");

		// Sprites used to display the state of the simulation and tools used
		speedIndicator = new Texture("speed_indicator.png");
		pauseIndicator = new Texture("pause_indicator.png");
		brush = new TextureRegion(tilemap, 0, 20, 20, 20);

		// Initialise the texture regions for the Tile types
		for (Tile.TileType type : Tile.TileType.values()) {
			type.setRegion(new TextureRegion(tilemap, type.ordinal() * 20 - 20, 0, 20, 20));
		}

		// Create the grid of tiles
		GRID.init(Gdx.graphics.getWidth() / SQUARE_SIZE, Gdx.graphics.getHeight() / SQUARE_SIZE);
	}

	@Override
	public void render () {
		ScreenUtils.clear(BACKGROUND_COLOUR);

		// Update the grid based on the current simulation model + take user input
		update();

		// Render the grid
		main.begin();
		ScreenUtils.clear(0, 0, 0, 0);
		batch.begin();

		GRID.forEverySquare((x, y) -> {
			// Don't render empty tiles
			if (GRID.emptyAt(x, y)) return;
			// Some silly math to get the funky colours
			float temperature = GRID.get(x, y).getTemperature();
			batch.setColor(
					(temperature - 100) / 100 + 1,
					(temperature - 1000) / 1000 + 1,
					0.01f * (500 - temperature)*(1000 - temperature) / 10000,
					1);
			// Draw the tile
			batch.draw(GRID.get(x, y).getRegion(), x * SQUARE_SIZE, y * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
		});

		batch.end();
		main.end();

		// Draw main texture through an outline shader
		batch.begin();
		batch.setShader(outlineShader);
		outlineShader.setUniformf("u_offsets", outlineOffsets);
		batch.setColor(Color.WHITE);
		batch.draw(main.getColorBufferTexture(), 0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), -Gdx.graphics.getHeight());
		batch.setShader(null);

		// Render time indicator
		int numberTriangles = timeScale < 0.4f ? 1 : (timeScale > 0.6f ? 3 : 2);
		Color color = timeScale < 0.7f ? Color.WHITE : (timeScale > 1.2f ? Color.RED : Color.YELLOW);
		int SPACING = 5;
		int SIZE = 40;

		batch.setColor(color);
		if (isRunning) {
			for (int i = 0; i <= numberTriangles; i++)
				batch.draw(speedIndicator, SPACING * i + SIZE * (i - 1), SPACING, SIZE, SIZE);
		} else {
			batch.draw(pauseIndicator, SPACING, SPACING, SIZE, SIZE);
		}

		// Render brush type
		batch.setColor(Color.WHITE);
		batch.draw(brushType.getRegion(), Gdx.graphics.getWidth() - SPACING - SIZE, SPACING, SIZE, SIZE);
		batch.draw(brush, Gdx.graphics.getWidth() - 2*SPACING - 2*SIZE, SPACING, SIZE, SIZE);

		batch.end();
	}

	public void handleMouseInput() {
		// Get the cell coords that the mouse happens to be in
		int mouseCellX = Gdx.input.getX() / SQUARE_SIZE;
		int mouseCellY = (Gdx.graphics.getHeight() - Gdx.input.getY()) / SQUARE_SIZE;

		// Don't do anything if the coords are invalid
		if (!GRID.areCoordsValid(mouseCellX, mouseCellY))
			return;

		Tile tile = GRID.get(mouseCellX, mouseCellY);

		// LMB         - paint tiles / increase temp
		// LMB + SHIFT - increase temp fast
		// RMB         - destroy tiles / decrease temp
		// RMB + SHIFT - decrease temp fast
		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
			if (tile.isNotEmpty())
				tile.increaseTargetTemperatureBy(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? 10f : 1f);
			tile.setTileType(brushType);
		}
		if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && tile.isNotEmpty()) {
			tile.decreaseTargetTemperatureBy(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? 10f : 1f);
			if (tile.getTemperature() < 0f || Gdx.input.isKeyPressed(Input.Keys.E)) tile.reset();
		}
	}

	public void handleKeyboardInput() {
		// SPACE will pause/unpause the simulation
		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
			if (isRunning) System.out.println("Pausing...");
			else System.out.println("Unpausing...");
			isRunning = !isRunning;
		}

		// TAB will speed up/slow down the simulation
		if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
			if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
				System.out.println("Speeding up...");
				timeScale += 0.2f;
			} else {
				System.out.println("Slowing down...");
				timeScale -= 0.2f;
			}
			System.out.println("Time scale is now:" + timeScale);
		}

		// R will reset the simulation
		if (Gdx.input.isKeyJustPressed(Input.Keys.R)){
			System.out.println("Resetting...");
			isRunning = false;
			GRID.reset();
		}

		// LEFT/RIGHT ARROW will cycle the brush type
		if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
			System.out.println("Cycling brush types...");
			brushType = brushType.getNext();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
			System.out.println("Cycling brush types...");
			brushType = brushType.getPrevious();
		}
	}

	public void update () {
		handleMouseInput();
		handleKeyboardInput();

		if (!isRunning) return;

		// Update all tile temperatures
		GRID.update();
		SIMULATION_MODEL.diffuseHeat();
	}

	@Override
	public void dispose () {
		batch.dispose();
		main.dispose();
		outlineShader.dispose();

		speedIndicator.dispose();
		pauseIndicator.dispose();
		tilemap.dispose();
	}
}
