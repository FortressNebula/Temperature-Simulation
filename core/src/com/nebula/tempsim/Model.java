package com.nebula.tempsim;

import com.nebula.tempsim.math.Vector2i;

import java.util.Arrays;
import java.util.function.BiConsumer;

import static com.nebula.tempsim.TemperatureSimulation.*;


public enum Model {
    EQUALISE_WITH_COLDER_UNITS((x, y) -> {
        // Identify colder neighbours and their average
        float temperature = GRID.get(x, y).getTemperature();
        float totalHeatUnits = 0;

        Vector2i[] validNeighbours = Arrays.stream(NEIGHBOUR_OFFSETS)
                .filter(d -> GRID.areCoordsValid(x + d.x, y + d.y))
                .map(d -> new Vector2i(x + d.x, y + d.y))
                .filter(pos -> GRID.get(pos).isNotEmpty() && GRID.get(pos).getTemperature() <= temperature)
                .toArray(Vector2i[]::new);

        for (Vector2i pos : validNeighbours)
            totalHeatUnits += GRID.get(pos).getTemperature();

        // Now we can calculate the average heat distribution around
        float averageTemperature = totalHeatUnits / validNeighbours.length;

        // Equalise temperature with self, and neighbours
        for (Vector2i pos : validNeighbours)
           GRID.get(pos).increaseTargetTemperatureBy((averageTemperature - GRID.get(pos).getTemperature())
                   * timeScale * (1 - GRID.get(pos).getHeatCapacity()) * (1 - GRID.get(x, y).getInsulation())
           );

    }),
    CONVOLUTION((x, y) -> {
        // Get average temperature of surroundings
        float averageTemperature = 0;

        Vector2i[] validNeighbours = Arrays.stream(NEIGHBOUR_OFFSETS)
                .filter(d -> GRID.areCoordsValid(x + d.x, y + d.y))
                .map(d -> new Vector2i(x + d.x, y + d.y))
                .filter(pos -> GRID.get(pos).isNotEmpty())
                .toArray(Vector2i[]::new);

        for (Vector2i pos : validNeighbours)
            averageTemperature += GRID.get(pos).getTemperature();

        averageTemperature /= validNeighbours.length;
        GRID.get(x, y).setTargetTemperature(averageTemperature);
    }),
    HEAT_EQUATION_ESQUE((x, y) -> {
        float temperature = GRID.get(x, y).getTemperature();

        // Identify average temperature difference
        float netChange = 0;

        Vector2i[] validNeighbours = Arrays.stream(NEIGHBOUR_OFFSETS)
                .filter(d -> GRID.areCoordsValid(x + d.x, y + d.y))
                .filter(d -> d.x != 0f || d.y != 0f)
                .map(d -> new Vector2i(x + d.x, y + d.y))
                .filter(pos -> GRID.get(pos).isNotEmpty())
                .toArray(Vector2i[]::new);

        for (Vector2i pos : validNeighbours)
            netChange += (GRID.get(pos).getTemperature() - temperature) / 4;

        GRID.get(x, y).increaseTargetTemperatureBy(netChange * timeScale);
    });

    final BiConsumer<Integer, Integer> heatDiffusionFunction;

    Model (BiConsumer<Integer, Integer> heatDiffusionFunction) {
        this.heatDiffusionFunction = heatDiffusionFunction;
    }

    public void diffuseHeat () {
        GRID.forEverySquare((x, y) -> {
            if (GRID.presentAt(x, y))
                heatDiffusionFunction.accept(x, y);
        });
    }
}
