package me.kp56.dungeonsgen.generator.rooms;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import me.kp56.dungeonsgen.DungGenPlugin;
import me.kp56.dungeonsgen.generator.graphs.Coordinates;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Door {
    private static final Schematic DOORS = new Schematic(DungGenPlugin.getPlugin().getDungeonWorld(),
            new File("plugins/dungeonsgen/doors.schematic"));
    private static final Schematic WITHER_DOORS = new Schematic(DungGenPlugin.getPlugin().getDungeonWorld(),
            new File("plugins/dungeonsgen/witherdoors.schematic"));

    public final Coordinates coords1;
    public final Coordinates coords2;

    public Door(Coordinates coords1, Coordinates coords2) {
        this.coords1 = coords1;
        this.coords2 = coords2;
    }

    public void paste(Location location, boolean isWither, boolean isRotated) throws IOException, WorldEditException {
        Schematic toPaste = DOORS;

        if (isWither) toPaste = WITHER_DOORS;

        toPaste.load(new Vector(), 0);
        toPaste.load(new Vector(WITHER_DOORS.getWidth() / 2 + 1, 0, WITHER_DOORS.getLength() / 2 + 1),
                isRotated ? 90 : 0);
        toPaste.paste(location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Door door = (Door) o;
        return Objects.equals(coords1, door.coords1) && Objects.equals(coords2, door.coords2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coords1, coords2);
    }

    public Location calculateLocation() {
        if (coords1.x != coords2.x) {
            return new Location(DungGenPlugin.getPlugin().getDungeonWorld(), Math.min(coords1.x, coords2.x) * 32 + 30, 70,
                    coords1.y * 32 + 16);
        } else {
            return new Location(DungGenPlugin.getPlugin().getDungeonWorld(),coords1.x * 32 + 16, 70,
                    Math.min(coords1.y, coords2.y) * 32 + 30);
        }
    }

    @Override
    public String toString() {
        return "Door{" +
                "coords1=" + coords1 +
                ", coords2=" + coords2 +
                '}';
    }
}
