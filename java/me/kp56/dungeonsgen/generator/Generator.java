package me.kp56.dungeonsgen.generator;

import com.sk89q.worldedit.WorldEditException;
import lombok.Getter;
import me.kp56.dungeonsgen.generator.graphs.Graph;
import me.kp56.dungeonsgen.generator.graphs.traverse.BFS;
import me.kp56.dungeonsgen.generator.graphs.traverse.ShortestPathFinder;
import me.kp56.dungeonsgen.generator.graphs.traverse.TraverseAlgorithm;
import me.kp56.dungeonsgen.generator.rooms.*;
import me.kp56.dungeonsgen.utils.Utils;
import me.kp56.dungeonsgen.generator.graphs.Coordinates;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.World;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Generator {
    //May be useful in the future if Generator runs async to check the current stage
    @Getter
    private GenerationStage currentStage;
    public final int width;
    public final int height;
    public final World world;
    private double progress = 0d;

    public Generator(World world, int width, int height) {
        this.width = width;
        this.height = height;
        this.world = world;
    }

    public void generate() {
        while (true) {
            try {
                attemptGeneration();
                break;
            } catch (Exception e) {

            }
        }
    }

    private void attemptGeneration() throws IOException, WorldEditException {
        currentStage = GenerationStage.GENERATING_LAYOUT;

        int[][] roomLayout = new int[width][height];
        Arrays.stream(roomLayout).forEach(a -> Arrays.fill(a, -1));

        List<Room> rooms = new ArrayList<>();
        while (!generateLayout(rooms, roomLayout)) {
            //if we couldn't generate a layout we reset everything
            rooms = new ArrayList<>();
            roomLayout = new int[width][height];
            Arrays.stream(roomLayout).forEach(a -> Arrays.fill(a, -1));
        }

        currentStage = GenerationStage.PICKING_ROOM_TYPES;

        List<Coordinates> specialRooms = pickStartingBloodFairy(rooms, roomLayout);
        if (specialRooms == null) {
            throw new RuntimeException("Could not find a starting room/fairy room/blood room");
        }

        Coordinates startingRoom = specialRooms.get(0);
        Coordinates bloodRoom = specialRooms.get(1);
        Coordinates fairyRoom = specialRooms.get(2);

        rooms.get(roomLayout[startingRoom.x][startingRoom.y]).setRoomType(Room.RoomType.STARTING);
        rooms.get(roomLayout[bloodRoom.x][bloodRoom.y]).setRoomType(Room.RoomType.BLOOD);
        rooms.get(roomLayout[fairyRoom.x][fairyRoom.y]).setRoomType(Room.RoomType.FAIRY);

        List<Room> freeOneByOne = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getRoomType() == Room.RoomType.NORMAL && room.shape == Shape.ONE_BY_ONE) {
                freeOneByOne.add(room);
            }
        }

        if (freeOneByOne.size() <= 3) {
            throw new RuntimeException("Could not find available trap, puzzle and lost adventurer rooms.");
        }

        freeOneByOne.get(0).setRoomType(Room.RoomType.PUZZLE);
        freeOneByOne.get(1).setRoomType(Room.RoomType.PUZZLE);
        freeOneByOne.get(2).setRoomType(Room.RoomType.LOST_ADVENTURER);
        freeOneByOne.get(3).setRoomType(Room.RoomType.TRAP);

        for (int i = 4; i < freeOneByOne.size(); i++) {
            if (Math.random() < 0.3) {
                freeOneByOne.get(i).setRoomType(Room.RoomType.PUZZLE);
            }
        }

        currentStage = GenerationStage.CREATING_WITHER_DOORS;
        List<Door> witherDoors = new ArrayList<>();
        Set<Integer> idsOfWitherRooms = new HashSet<>();

        List<Room> finalRooms = rooms;
        ShortestPathFinder.FoundPath witherDoorsFound = (prev) -> {
            idsOfWitherRooms.addAll(prev);

            List<Door> doorsToConstruct = new ArrayList<>();

            List<Pair<Integer, Integer>> connectionsByID = Graph.constructPath(prev);

            for (Pair<Integer, Integer> pair : connectionsByID) {
                Room room1 = finalRooms.get(pair.getKey());
                Room room2 = finalRooms.get(pair.getValue());

                Coordinates segment1 = room1.findSegmentConnectedTo(room2, null);
                Coordinates segment2 = room2.findSegmentConnectedTo(room1, segment1);

                doorsToConstruct.add(new Door(segment1, segment2));
            }

            witherDoors.addAll(doorsToConstruct);
        };

        Graph fromArr = Graph.fullyConnectedFromTwoDimIntArray(roomLayout);
        BFS findFromStartToFairy = new BFS(fromArr, new ShortestPathFinder(roomLayout[fairyRoom.x][fairyRoom.y],
                witherDoorsFound), roomLayout[startingRoom.x][startingRoom.y]);

        BFS findFromBloodToFairy = new BFS(fromArr, new ShortestPathFinder(roomLayout[fairyRoom.x][fairyRoom.y],
                witherDoorsFound), roomLayout[bloodRoom.x][bloodRoom.y]);

        findFromStartToFairy.traverse();
        findFromBloodToFairy.traverse();

        currentStage = GenerationStage.CREATING_OTHER_DOORS;

        //each normal room has a pair of (list<integer>, integer) storing information about the path from
        //the closest wither room and the distance between both rooms

        List<Pair<List<Integer>, Integer>> pairs = new ArrayList<>();
        for (int i = 0; i < fromArr.size(); i++) {
            if (idsOfWitherRooms.contains(i)) pairs.add(null);
            else pairs.add(Pair.of(new ArrayList<>(Collections.singletonList(-1)), Integer.MAX_VALUE));
        }

        TraverseAlgorithm.TraverseStepHandler optimalConnectionFinder = (node1, node2, algorithm, info) -> {
            //updating distance
            if (!info.containsKey("dist")) info.put("dist", 0);
            else info.replace("dist", ((int) info.get("dist")) + 1);

            if (!info.containsKey("path")) info.put("path", new ArrayList<>(Collections.singletonList(node1)));
            else ((ArrayList<Integer>) info.get("path")).add(node1);

            Pair<List<Integer>, Integer> pair = pairs.get(node2);
            if (pair == null)
                return false;

            int dist = (int) info.get("dist");
            List<Integer> path = (List<Integer>) info.get("path");
            path.add(node2);

            if (pair.getValue() > dist) {
                Pair<List<Integer>, Integer> newPair = Pair.of(path, dist);

                pairs.replaceAll(pair2 -> {
                    if (pair == pair2) {
                        return newPair;
                    }

                    return pair2;
                });
            }

            return true;
        };

        for (int witherRoom : idsOfWitherRooms) {
            BFS battleForRooms = new BFS(fromArr, optimalConnectionFinder, witherRoom);
            battleForRooms.traverse();
        }

        Graph withActualDoors = Graph.fromTwoDimIntArray(roomLayout);

        for (Pair<List<Integer>, Integer> pair : pairs) {
            if (pair != null) {
                int prev = pair.getKey().get(0);
                for (int j = 1; j < pair.getKey().size(); j++) {
                    int a = pair.getKey().get(j);
                    if (!withActualDoors.getConnected(prev).contains(a)) {
                        withActualDoors.connectNodeBidirectionally(a, prev);
                    }
                }
            }
        }

        Set<Door> doorsHashSet = new HashSet<>(witherDoors);
        for (int i = 0; i < withActualDoors.size(); i++) {
            for (int j : withActualDoors.getConnected(i)) {
                Room room1 = finalRooms.get(i);
                Room room2 = finalRooms.get(j);

                Coordinates segment1 = room1.findSegmentConnectedTo(room2, null);
                Coordinates segment2 = room2.findSegmentConnectedTo(room1, segment1);

                doorsHashSet.add(new Door(segment1, segment2));
            }
        }

        List<Door> doors = new ArrayList<>(doorsHashSet);

        for (Door door : doors) {
            rooms.get(roomLayout[door.coords1.x][door.coords1.y]).doors.add(door);
        }

        /*System.out.println(idsOfWitherRooms);
        System.out.println(Arrays.deepToString(roomLayout));
        System.out.println(doors);*/

        currentStage = GenerationStage.CHOOSING_ROOMS;

        List<Schematic> loaded = Schematic.getLoadedSchematics();
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (!room.findCorrectSchematic(loaded)) {
                double generationProgress = i / (double) rooms.size();

                if (progress < generationProgress) {
                    progress = generationProgress;
                    System.out.println("Generating a possible dungeon (" + (progress * 100) + "%)");
                }

                if (room.getRoomType() != Room.RoomType.NORMAL) {
                    System.out.println("The generator was unable to find a schematic for type=" + room.getRoomType());
                }

                throw new RuntimeException("Could not find any suitable schematic for type=" + room.getRoomType()
                        + " shape=" + room.shape + ".");
            }
        }
        System.out.println("Generating a possible dungeon (100%)");

        System.out.println("Found all needed schematics. Pasting rooms...");

        currentStage = GenerationStage.PASTING_ROOMS;

        for (Room room : rooms) {
            try {
                room.paste(world);
            } catch (WorldEditException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Pasted all rooms. Pasting doors...");

        currentStage = GenerationStage.PASTING_DOORS;
        for (Door door : doors) {
            door.paste(door.calculateLocation() , false, door.coords1.x == door.coords2.x);
        }

        for (Door door : witherDoors) {
            door.paste(door.calculateLocation() , true, door.coords1.x == door.coords2.x);
        }

        System.out.println("Generation has finished!");
        currentStage = GenerationStage.FINISHED;
    }

    private List<Coordinates> pickStartingBloodFairy(List<Room> rooms, int[][] roomLayout) {
        AtomicReference<Coordinates> startingRoomReference = new AtomicReference<>();
        AtomicReference<Coordinates> bloodRoomReference = new AtomicReference<>();

        Utils.iterateThroughBorder(Utils.twoDimIntArrayToInteger(roomLayout), (x, y, el) -> {
            if (rooms.get(el).shape == Shape.ONE_BY_ONE) {
                if (startingRoomReference.get() == null) {
                    startingRoomReference.set(new Coordinates(x, y));
                } else {
                    bloodRoomReference.set(new Coordinates(x, y));
                    return true;
                }
            }

            return false;
        });

        Coordinates startingRoom = startingRoomReference.get();
        Coordinates bloodRoom = bloodRoomReference.get();

        if (startingRoom == null || bloodRoom == null) {
            return null;
        }

        Coordinates fairyRoom = null;
        int minimumDistSquaredSum = Integer.MAX_VALUE;
        for (int x = 1; x < roomLayout.length - 1; x++) {
            for (int y = 1; y < roomLayout[0].length - 1; y++) {
                int distSquared = Utils.distanceSquared(x, y, startingRoom.x, startingRoom.y) + Utils.distanceSquared(x, y, bloodRoom.x, bloodRoom.y);
                if (minimumDistSquaredSum > distSquared && rooms.get(roomLayout[x][y]).shape == Shape.ONE_BY_ONE) {
                    minimumDistSquaredSum = distSquared;
                    fairyRoom = new Coordinates(x, y);
                }
            }
        }

        if (fairyRoom != null) {
            return Arrays.asList(startingRoom, bloodRoom, fairyRoom);
        } else {
            return null;
        }
    }

    //attempts to generate a layout
    private boolean generateLayout(List<Room> rooms, int[][] roomLayout) {
        //we know there need to be a starting and a blood room at opposite sides and a fairy room in the middle of the map

        AtomicReference<Coordinates> firstRoom = new AtomicReference<>();
        AtomicReference<Coordinates> secondRoom = new AtomicReference<>();
        Utils.possibleEvents(() -> {
            firstRoom.set(pickLocationBetween(new Coordinates(0, 0), new Coordinates(0, height - 1)));
            secondRoom.set(pickLocationBetween(new Coordinates(width - 1, 0), new Coordinates(width - 1, height - 1)));
        }, () -> {
            firstRoom.set(pickLocationBetween(new Coordinates(0, 0), new Coordinates(width - 1, 0)));
            secondRoom.set(pickLocationBetween(new Coordinates(0, height - 1), new Coordinates(width - 1, height - 1)));
        });

        roomLayout[firstRoom.get().x][firstRoom.get().y] = 0;
        roomLayout[secondRoom.get().x][secondRoom.get().y] = 1;

        rooms.add(new Room(firstRoom.get().x, firstRoom.get().y, Shape.ONE_BY_ONE, Rotation.NO_ROTATION));
        rooms.add(new Room(secondRoom.get().x, secondRoom.get().y, Shape.ONE_BY_ONE, Rotation.NO_ROTATION));

        AtomicInteger currentRoomID = new AtomicInteger(2);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (roomLayout[x][y] == -1) {
                    int finalX = x;
                    int finalY = y;

                    AtomicBoolean generated = new AtomicBoolean(true);
                    Utils.possibleEvents(() -> {
                        //1x1 room
                        Room oneByOne = new Room(finalX, finalY, Shape.ONE_BY_ONE, Rotation.NO_ROTATION);
                        rooms.add(oneByOne);

                        placeRoom(currentRoomID.getAndIncrement(), oneByOne.getSegments(), roomLayout);
                    }, () -> {
                        //2x1, 3x1, 4x1 rooms
                        if (!attemptWithOneRotation(currentRoomID, finalX, finalY, rooms, roomLayout, Shape.values()[(int) (Math.random() * 3) + 1])) {
                            generated.set(false);
                        }
                    }, () -> {
                        //2x2, L-shaped rooms
                        if (!attemptWithAllRotations(currentRoomID, finalX, finalY, rooms, roomLayout, Shape.values()[(int) (Math.random() * 2) + 4])) {
                            generated.set(false);
                        }
                    });

                    if (!generated.get())
                        return false;
                }
            }
        }

        return true;
    }

    //a small optimization trick, basically some rooms layouts in latter rotations are equal to the layout with another earlier rotation
    private boolean attemptWithOneRotation(AtomicInteger currentRoomID, int x, int y, List<Room> rooms, int[][] roomLayout, Shape shape) {
        Room room = new Room(x, y, shape, Rotation.NO_ROTATION);
        if (!canFit(room.getSegments(), roomLayout)) {
            room.rotateBy90Degrees();
        }

        if (canFit(room.getSegments(), roomLayout)) {
            rooms.add(room);

            placeRoom(currentRoomID.getAndIncrement(), room.getSegments(), roomLayout);

            return true;
        } else {
            return false;
        }
    }

    private boolean attemptWithAllRotations(AtomicInteger currentRoomID, int x, int y, List<Room> rooms, int[][] roomLayout, Shape shape) {
        Room room = new Room(x, y, shape, Rotation.NO_ROTATION);
        for (int i = 0; i < 3 && !canFit(room.getSegments(), roomLayout); i++) {
            room.rotateBy90Degrees();
        }

        if (canFit(room.getSegments(), roomLayout)) {
            rooms.add(room);

            placeRoom(currentRoomID.getAndIncrement(), room.getSegments(), roomLayout);

            return true;
        } else {
            return false;
        }
    }

    private boolean canFit(List<Coordinates> room, int[][] roomLayout) {
        for (Coordinates coordinates : room) {
            if (!Utils.isInBounds(coordinates.x, coordinates.y, roomLayout))
                return false;
        }

        for (Coordinates coordinates : room) {
            if (roomLayout[coordinates.x][coordinates.y] != -1) {
                return false;
            }
        }

        return true;
    }

    private void placeRoom(int id, List<Coordinates> room, int[][] roomLayout) {
        for (Coordinates coords : room) {
            roomLayout[coords.x][coords.y] = id;
        }
    }

    private Coordinates pickLocationBetween(Coordinates point1, Coordinates point2) {
        int maxX = Math.max(point1.x, point2.x);
        int maxY = Math.max(point1.y, point2.y);

        int minX = Math.min(point1.x, point2.x);
        int minY = Math.min(point1.y, point2.y);

        int spaceBetween = (maxX - minX + 1) * (maxY - minY + 1);

        Random random = new Random();
        int choosen = random.nextInt(spaceBetween);

        return new Coordinates(minX + choosen % (maxX - minX + 1), minY + choosen / (maxX - minX + 1));
    }

    public static enum GenerationStage {
        GENERATING_LAYOUT,
        PICKING_ROOM_TYPES,
        CREATING_WITHER_DOORS,
        CREATING_OTHER_DOORS,
        CHOOSING_ROOMS,
        PASTING_ROOMS,
        PASTING_DOORS,
        FINISHED;
    }
}
