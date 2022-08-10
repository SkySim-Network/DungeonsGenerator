package me.kp56.dungeonsgen.generator.rooms;

public enum Rotation {
    NO_ROTATION(0),
    CLOCKWISE_90(90),
    CLOCKWISE_180(180),
    COUNTERCLOCKWISE_90(270);

    public final int rotation;

    Rotation(int rotation) {
        this.rotation = rotation;
    }
}
