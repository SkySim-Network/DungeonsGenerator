package me.kp56.dungeonsgen.generator.rooms;

import lombok.Getter;
import me.kp56.dungeonsgen.generator.graphs.Coordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Shape {
    ONE_BY_ONE(false, Collections.singletonList(new Coordinates(0, 0))),
    TWO_BY_ONE(false, Arrays.asList(
            new Coordinates(0, 0),
            new Coordinates(1, 0)
    )),
    THREE_BY_ONE(false, Arrays.asList(
            new Coordinates(0, 0),
            new Coordinates(1, 0),
            new Coordinates(2, 0)
    )),
    FOUR_BY_ONE(false, Arrays.asList(
            new Coordinates(0, 0),
            new Coordinates(1, 0),
            new Coordinates(2, 0),
            new Coordinates(3, 0)
    )),
    TWO_BY_TWO(true, Arrays.asList(
            new Coordinates(0, 0),
            new Coordinates(1, 0),
            new Coordinates(0, 1),
            new Coordinates(1, 1)
    )),
    L_SHAPED(true, Arrays.asList(
            new Coordinates(0, 0),
            new Coordinates(1, 0),
            new Coordinates(1, 1)
    ));

    @Getter
    private boolean rotateAroundMiddle;

    @Getter
    private List<Coordinates> unrotated;

    Shape(boolean rotateAroundMiddle, List<Coordinates> unrotated) {
        this.rotateAroundMiddle = rotateAroundMiddle;
        this.unrotated = unrotated;
    }

    public void rotateBy90Degrees(List<Coordinates> coords) {
        rotateBy90Degrees(coords, new ArrayList<>());
    }

    public void rotateBy90Degrees(List<Coordinates> coords, List<Door> doors) {
        List<Coordinates> copy = new ArrayList<>(coords);
        coords.clear();
        List<Door> doorCopy = new ArrayList<>(doors);
        doors.clear();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Coordinates coordinate : copy) {
            minX = Math.min(minX, coordinate.x);
            minY = Math.min(minY, coordinate.y);

            maxX = Math.max(maxX, coordinate.x);
            maxY = Math.max(maxY, coordinate.y);
        }

        double middleX = ((double) (minX + maxX)) / 2d;
        double middleY = ((double) (minY + maxY)) / 2d;

        for (Coordinates coordinate : copy) {
            if (rotateAroundMiddle) {
                coords.add(new Coordinates((int) ((coordinate.y - middleY) + middleY), (int) (-(coordinate.x - middleX) + middleX)));
            } else {
                coords.add(new Coordinates(coordinate.y, -coordinate.x));
            }
        }

        for (Door door : doorCopy) {
            Coordinates coords1;
            Coordinates coords2;

            if (rotateAroundMiddle) {
                coords1 = new Coordinates((int) (((door.coords1.y - middleY)) + middleY), (int) (-(door.coords1.x - middleX) + middleX));
                coords2 = new Coordinates((int) (((door.coords2.y - middleY)) + middleY), (int) (-(door.coords2.x - middleX) + middleX));
            } else {
                coords1 = new Coordinates(door.coords1.y, -door.coords1.x);
                coords2 = new Coordinates(door.coords2.y, -door.coords2.x);
            }

            doors.add(new Door(coords1, coords2));
        }
    }

    public Rotation detectRotation(List<Coordinates> coords) {
        List<Coordinates> copy = new ArrayList<>(unrotated);

        int i = 0;
        for (; !copy.equals(coords); i++) {
            rotateBy90Degrees(copy);
        }

        return Rotation.values()[i];
    }

    public static Shape detectShape(List<Coordinates> coords) {
        boolean isSquareShaped;

        int x = coords.get(0).x;
        int y = coords.get(0).y;
        for (Coordinates coordinates : coords) {
            if (x != -1 && coordinates.x != x) {
                x = -1;
            }

            if (y != -1 && coordinates.y != y) {
                y = -1;
            }
        }

        isSquareShaped = x == -1 && y == -1;

        switch (coords.size()) {
            case 1:
                return ONE_BY_ONE;
            case 2:
                return TWO_BY_ONE;
            case 4:
                if (isSquareShaped) return TWO_BY_TWO;
                return FOUR_BY_ONE;
            case 3:
                if (isSquareShaped) return L_SHAPED;
                return THREE_BY_ONE;
        }

        return null;
    }
}
