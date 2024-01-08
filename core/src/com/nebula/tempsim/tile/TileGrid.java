package com.nebula.tempsim.tile;

import com.nebula.tempsim.math.Vector2i;

import java.util.function.BiConsumer;

public class TileGrid {
    private Tile[][] raw;
    int width, height;

    // Simulation functions

    public void init (int width, int height) {
        this.width = width;
        this.height = height;
        raw = new Tile[width][height];
        forEverySquare((x, y) -> raw[x][y] = new Tile());
    }

    public void update () {
        forEverySquare((x, y) -> raw[x][y].update());
    }

    public void reset () {
        forEverySquare((x, y) -> raw[x][y].reset());
    }

    // Coordinate functions

    public void forEverySquare (BiConsumer<Integer, Integer> function) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                function.accept(x, y);
            }
        }
    }

    public boolean areCoordsValid (int x, int y) {
        return (x >= 0 && y >= 0) && (x < width && y < height);
    }

    // Tile-specific functions

    public Tile get (int x, int y) {
        return raw[x][y];
    }
    public Tile get (Vector2i pos) {
        return raw[pos.x][pos.y];
    }

    public boolean presentAt (int x, int y) {
        return raw[x][y].isNotEmpty();
    }

    public boolean emptyAt (int x, int y) {
        return !presentAt(x, y);
    }
}
