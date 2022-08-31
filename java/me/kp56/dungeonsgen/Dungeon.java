package me.kp56.dungeonsgen;

import lombok.Getter;
import me.kp56.dungeonsgen.generator.rooms.Room;
import org.bukkit.World;

import java.util.List;

public class Dungeon {
    public final int width;
    public final int height;
    public final World world;

    @Getter
    private List<Room> rooms;
    @Getter
    private int[][] roomLayout;

    public Dungeon(int width, int height, World world, List<Room> rooms, int[][] roomLayout) {
        this.width = width;
        this.height = height;
        this.world = world;
        this.rooms = rooms;
        this.roomLayout = roomLayout;
    }
}
