package com.nebula.tempsim.tile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Null;

import java.util.Arrays;
import java.util.function.Function;

public class Tile {
    private float temperature;
    private float targetTemperature;
    private TileType type;

    public Tile () {
        this(TileType.EMPTY);
    }

    public Tile (TileType type) {
        this.type = type;
        temperature = targetTemperature = 0f;
    }

    public void update () {
        temperature = targetTemperature = type.updateFunction.apply(targetTemperature);
    }

    public void reset () {
        type = TileType.EMPTY;
        temperature = targetTemperature = 0f;
    }

    public float getTemperature () { return temperature; }

    public void setTargetTemperature (float target) { targetTemperature = target; }
    public void increaseTargetTemperatureBy (float delta_T) { targetTemperature += delta_T; }
    public void decreaseTargetTemperatureBy (float delta_T) { targetTemperature -= delta_T; }

    public boolean isNotEmpty () { return type != TileType.EMPTY; }
    public void setTileType (TileType newType) { type = newType; }
    public TextureRegion getRegion () { return type.region; }

    public float getHeatCapacity () { return type.heatCapacity; }
    public float getInsulation () { return type.insulation; }

    public enum TileType {
        EMPTY,
        NORMAL,
        INSULATOR(1f, 0f, $ -> $),
        HEAT_GEN(0f, 0f, t -> t + 20f),
        HEAT_SINK(0f, 0f, t -> Math.max(t - 20f, 0f))
        ;

        private static final TileType[] vals = Arrays.copyOfRange(values(), 1, values().length);

        @Null
        private TextureRegion region;

        // Unique behaviour
        // How to transform target temperature every update
        final Function<Float, Float> updateFunction;
        // Resistance to spread heat
        final float insulation;
        // Resistance to gain heat
        final float heatCapacity;

        TileType () {
            this(0, 0, $ -> $);
        }

        TileType (float insulation, float heatCapacity, Function<Float, Float> updateFunction) {
            this.insulation = insulation;
            this.heatCapacity = heatCapacity;
            this.updateFunction = updateFunction;
        }

        public void setRegion (TextureRegion region) { this.region = region; }
        public TextureRegion getRegion () { return region; }

        public TileType getNext () { return vals[Math.min(ordinal(), vals.length - 1)]; }
        public TileType getPrevious () { return vals[Math.max(ordinal() - 2, 0)]; }
    }
}
