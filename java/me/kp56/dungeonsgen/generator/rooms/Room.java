package me.kp56.dungeonsgen.generator.rooms;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import lombok.Getter;
import lombok.Setter;
import me.kp56.dungeonsgen.generator.graphs.Coordinates;
import me.kp56.dungeonsgen.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Room {
    @Getter
    private List<Coordinates> segments;
    public final Shape shape;
    @Getter
    private Rotation rotation;
    @Getter
    @Setter
    private Schematic schematic;

    @Getter
    private RoomType roomType = RoomType.NORMAL;

    public List<Door> doors = new ArrayList<>();

    public Room(int x, int y, Shape shape, Rotation rotation) {
        this.shape = shape;
        this.rotation = rotation;

        List<Coordinates> list = new ArrayList<>();
        for (Coordinates coordinates : shape.getUnrotated()) {
            list.add(new Coordinates(coordinates.x + x, coordinates.y + y));
        }

        segments = list;
    }

    public void rotateBy90Degrees() {
        //rotate all segments by 90 degrees
        shape.rotateBy90Degrees(segments);
        //change rotation to the next one
        rotation = Rotation.values()[(Arrays.asList(Rotation.values()).indexOf(rotation) + 1) % Rotation.values().length];
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Coordinates findSegmentConnectedTo(Room room, Coordinates evaluatedSegment) {
        List<Coordinates> rel = Utils.getRelCoords();

        for (Coordinates segment : segments) {
            for (Coordinates r : rel) {
                if (evaluatedSegment == null) {
                    if (room.getSegments().contains(new Coordinates(segment.x + r.x, segment.y + r.y))) {
                        return segment;
                    }
                } else {
                    if (evaluatedSegment.equals(new Coordinates(segment.x + r.x, segment.y + r.y))) {
                        return segment;
                    }
                }
            }
        }

        return null;
    }

    public boolean findCorrectSchematic(List<Schematic> loadedSchematics) throws IOException {
        Coordinates min = getMinCoords();

        schematicLoop:
        for (int i = 0; i < loadedSchematics.size(); i++) {
            Schematic schematic = loadedSchematics.get(i);
            if (schematic.isLoaded()) {
                RoomType schemType = RoomType.NORMAL;
                for (RoomType type : RoomType.values()) {
                    if (schematic.file.getName().startsWith(type.name())) {
                        schemType = type;
                        break;
                    }
                }

                if (schemType == roomType) {
                    List<Coordinates> schemsCoords = schematic.evaluateSegments();
                    Shape schemShape = Shape.detectShape(schemsCoords);

                    //if shape matches
                    if (shape == schemShape) {
                        List<Door> schemDoors = schematic.evaluateDoors();

                        Rotation schemRot = Rotation.NO_ROTATION;
                        while (!new HashSet<>(schemsCoords.stream().map((c) -> new Coordinates(c.x + min.x, c.y + min.y))
                                .collect(Collectors.toList()))
                                .containsAll(segments)) {
                            //here we rotate the schematic
                            /*System.out.println("Rotating:");
                            System.out.println("SchemCoords: " + schemsCoords);
                            System.out.println("Segments: " + segments);*/

                            schemShape.rotateBy90Degrees(schemsCoords, schemDoors);
                            schemRot = Rotation.values()[(Arrays.asList(Rotation.values()).indexOf(schemRot) + 1) % Rotation.values().length];
                        }

                        if (schemsCoords.size() == 1) {
                            //for 1x1 rooms we want to rotate until the doors match (if they ever do)
                            //this should speed up the generation process as we won't be discarding dungeons
                            //just because 1x1 rooms aren't rotated properly
                            boolean doorsDidMatch = false;
                            rotation1x1Loop:
                            for (int j = 0; j < 4; j++) {
                                for (Door door : doors) {
                                    if (schemDoors.stream().noneMatch((d) -> door.equals(
                                            new Door(new Coordinates(d.coords1.x + min.x, d.coords1.y + min.y),
                                                    new Coordinates(d.coords2.x + min.x, d.coords2.y + min.y))))) {
                                        schemShape.rotateBy90Degrees(schemsCoords, schemDoors);
                                        schemRot = Rotation.values()[(Arrays.asList(Rotation.values()).indexOf(schemRot) + 1) % Rotation.values().length];
                                        continue rotation1x1Loop;
                                    }
                                }

                                //if all doors matched and we didn't skip
                                doorsDidMatch = true;
                                break;
                            }

                            if (!doorsDidMatch) {
                                System.out.println("Non of the doors: " + schemDoors + " matched the doors: " + doors + " even after rotating multiple times");
                                continue schematicLoop;
                            }
                        }

                        System.out.println("Min of the room is: " + min);
                        for (Door door : doors) {
                            if (schemDoors.stream().noneMatch((d) -> door.equals(
                                    new Door(new Coordinates(d.coords1.x + min.x, d.coords1.y + min.y),
                                            new Coordinates(d.coords2.x + min.x, d.coords2.y + min.y))))) {
                                System.out.println("Non of the doors: " + schemDoors + " match the door: " + door);
                                continue schematicLoop;
                            }
                        }

                        schematic.load(schemShape.isRotateAroundMiddle() ?
                                        new Vector(schematic.getWidth() / 2, 0, schematic.getLength() / 2) :
                                        new Vector(15, 0, 15),
                                schemRot.rotation);

                        loadedSchematics.remove(schematic);

                        this.schematic = schematic;

                        return true;
                    }
                }
            }
        }

        //System.out.println("Could not find a matching schematic to shape=" + shape + ", rotation=" + rotation);
        return false;
    }

    public Coordinates getMinCoords() {
        Coordinates min = new Coordinates(Integer.MAX_VALUE, Integer.MAX_VALUE);

        for (Coordinates coords : segments) {
            min = new Coordinates(Math.min(min.x, coords.x), Math.min(min.y, coords.y));
        }

        return min;
    }

    public void paste(World world) throws WorldEditException, IOException {
        Coordinates minCoords = new Coordinates(Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (Coordinates coordinates : segments) {
            minCoords = new Coordinates(Math.min(coordinates.x, minCoords.x), Math.min(coordinates.y, minCoords.y));
        }

        schematic.paste(new Location(world, minCoords.x * 32, 0, minCoords.y * 32));
    }

    @Override
    public String toString() {
        return "Room{" +
                "segments=" + segments +
                ", shape=" + shape +
                ", rotation=" + rotation +
                ", schematic=" + schematic +
                ", roomType=" + roomType +
                ", doors=" + doors +
                '}';
    }

    public static enum RoomType {
        NORMAL,
        FAIRY,
        PUZZLE,
        STARTING,
        BLOOD,
        LOST_ADVENTURER,
        TRAP;
    }
}
