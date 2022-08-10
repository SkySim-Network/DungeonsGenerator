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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
        if (this.roomType == null) {
            this.roomType = roomType;
        }
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
                        List<Door> schemDoors = new ArrayList<>();

                        //find doors
                        for (Pair<BaseBlock, Vector> pair : schematic.getBlocks()) {
                            BaseBlock block = pair.getKey();
                            Vector location = pair.getValue().subtract(schematic.getMinPoint());

                            if (block.getNbtData() != null) {
                                if (block.getNbtData().getString("Text1").toLowerCase(Locale.ROOT).equals("{\"extra\":[\"door::this\"],\"text\":\"\"}")) {
                                    int x1 = location.getBlockX() / 32;
                                    int y1 = location.getBlockZ() / 32;

                                    int relX = location.getBlockX() - x1 * 32;
                                    int relY = location.getBlockZ() - y1 * 32;

                                    int x2;
                                    int y2;
                                    if (relX == 15) {
                                        x2 = x1;
                                        y2 = relY < 16 ? y1 - 1 : y1 + 1;
                                    } else if (relY == 15) {
                                        y2 = y1;
                                        x2 = relX < 16 ? x1 - 1 : x1 + 1;
                                    } else {
                                        System.out.println("Schematic " + schematic.file.getName() + " has an invalid door place " + location);
                                        System.out.println("Min point: " + schematic.getMinPoint());
                                        System.out.println("Max point: " + schematic.getMaxPoint());
                                        System.out.println("Schematic's rotation: " + schematic.getRotation());
                                        System.out.println("Schematic is rotated around: " + schematic.getRotateAround());

                                        throw new RuntimeException("Found an invalid door in the schematic.");
                                    }

                                    schemDoors.add(new Door(new Coordinates(x1, y1), new Coordinates(x2, y2)));
                                }
                            }
                        }

                        Rotation schemRot = Rotation.NO_ROTATION;
                        //we need to rotate schematic until schemRot == rotation
                        while (!schemsCoords.stream().map((c) -> new Coordinates(c.x + min.x, c.y + min.y))
                                .collect(Collectors.toList())
                                .equals(segments)) {
                            //here we rotate the schematic
                            schemShape.rotateBy90Degrees(schemsCoords, schemDoors);
                            schemRot = Rotation.values()[(Arrays.asList(Rotation.values()).indexOf(schemRot) + 1) % Rotation.values().length];
                        }

                        for (Door door : doors) {
                            if (schemDoors.stream().noneMatch((d) -> door.equals(
                                    new Door(new Coordinates(d.coords1.x + min.x, d.coords1.y + min.y),
                                            new Coordinates(d.coords2.x + min.x, d.coords2.y + min.y))))) {
                                continue schematicLoop;
                            }
                        }

                        schematic.load(schemShape.isRotateAroundMiddle() ?
                                        new Vector(schematic.getWidth() / 2 + 1, 0, schematic.getLength() / 2 + 1) :
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
        schematic.paste(new Location(world, segments.get(0).x * 32, 0, segments.get(0).y * 32));
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
