package me.kp56.dungeonsgen.generator.rooms;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.util.io.Closer;
import lombok.Getter;
import me.kp56.dungeonsgen.DungGenPlugin;
import me.kp56.dungeonsgen.generator.graphs.Coordinates;
import me.kp56.dungeonsgen.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.util.*;

public class Schematic {
    public final File file;
    private World world;
    private List<Pair<BaseBlock, Vector>> blocks;
    @Getter
    private Vector minPoint;
    @Getter
    private Vector maxPoint;
    @Getter
    private boolean isLoaded = false;
    private List<Coordinates> preEvaluatedSegments;

    private BaseBlock[][][] helperArray;

    @Getter
    private double rotation;
    @Getter
    private Vector rotateAround;

    private static List<Schematic> allSchematics;
    private static Map<File, Map<Double, Map<Vector, Schematic>>> cache = new HashMap<>();
    private static Map<File, byte[]> filesCached = new HashMap<>();

    static {
        List<Schematic> schems = new ArrayList<>();
        for (File f : new File("plugins/dungeonsgen/roomschematics").listFiles()) {
            schems.add(new Schematic(DungGenPlugin.getPlugin().getDungeonWorld(), f));
        }

        allSchematics = schems;
    }

    public Schematic(World world, File file) {
        this.file = file;
        this.world = world;
    }

    public void load(Vector rotateAround, double rotation) throws IOException {
        if (cache.get(file) != null && cache.get(file).get(rotation) != null && cache.get(file).get(rotation).get(rotateAround) != null) {
            Schematic schematic = cache.get(file).get(rotation).get(rotateAround);

            blocks = schematic.blocks;
            minPoint = schematic.minPoint;
            maxPoint = schematic.maxPoint;
            isLoaded = schematic.isLoaded;
            preEvaluatedSegments = schematic.preEvaluatedSegments;
            helperArray = schematic.helperArray;
            world = schematic.world;
        } else {
            if (filesCached.get(file) == null) {
                byte[] fileBytes = Files.toByteArray(file);

                filesCached.put(file, fileBytes);
            }
            Closer closer = Closer.create();

            ByteArrayInputStream bais = closer.register(new ByteArrayInputStream(filesCached.get(file)));
            BufferedInputStream bis = closer.register(new BufferedInputStream(bais));

            blocks = Utils.readSchem(new Location(world, 0, 0, 0), bis, rotateAround, rotation);

            closer.close();

            if (cache.get(file) != null && cache.get(file).get(rotation) != null) {
                cache.get(file).get(rotation).put(rotateAround, this);
            } else if (cache.get(file) != null) {
                Map<Vector, Schematic> map = new HashMap<>();
                map.put(rotateAround, this);

                cache.get(file).put(rotation, map);
            } else {
                Map<Vector, Schematic> map = new HashMap<>();
                map.put(rotateAround, this);

                Map<Double, Map<Vector, Schematic>> map2 = new HashMap<>();
                map2.put(rotation, map);

                cache.put(file, map2);
            }

            Vector minVec = new Vector(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            Vector maxVec = new Vector(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            for (Pair<BaseBlock, Vector> pair : blocks) {
                Vector vec = pair.getValue();
                minVec = new Vector(Math.min(minVec.getX(), vec.getX()), Math.min(minVec.getY(), vec.getY()),
                        Math.min(minVec.getZ(), vec.getZ()));
                maxVec = new Vector(Math.max(maxVec.getX(), vec.getX()), Math.max(maxVec.getY(), vec.getY()),
                        Math.max(maxVec.getZ(), vec.getZ()));
            }

            /*System.out.println("Loading schematic " + file.getName());
            System.out.println("Min point: " + minVec);
            System.out.println("Max point: " + maxVec);
            System.out.println("Rotation: " + rotation);
            System.out.println("Rotating around: " + rotateAround);*/

            helperArray = new BaseBlock[maxVec.getBlockX() - minVec.getBlockX() + 1][maxVec.getBlockY() - minVec.getBlockY() + 1]
                    [maxVec.getBlockZ() - minVec.getBlockZ() + 1];

            for (Pair<BaseBlock, Vector> pair : blocks) {
                Vector vec = pair.getValue();
                helperArray[vec.getBlockX() - minVec.getBlockX()][vec.getBlockY() - minVec.getBlockY()]
                        [vec.getBlockZ() - minVec.getBlockZ()] = pair.getKey();
            }

            minPoint = minVec;
            maxPoint = maxVec;

            isLoaded = true;
        }

        this.rotation = rotation;
        this.rotateAround = rotateAround;
    }

    public BaseBlock getBlockAt(int x, int y, int z) {
        try {
            return helperArray[x - minPoint.getBlockX()][y - minPoint.getBlockY()][z - minPoint.getBlockZ()];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public List<Pair<BaseBlock, Vector>> getBlocks() {
        return ImmutableList.<Pair<BaseBlock, Vector>>builder().addAll(blocks).build();
    }

    public void paste(Location location) throws WorldEditException {
        BukkitWorld bkWorld = new BukkitWorld(location.getWorld());

        for (Pair<BaseBlock, Vector> pair : blocks) {
            Vector vec = pair.getValue();
            bkWorld.setBlock(pair.getValue().add(vec.getBlockX() + location.getBlockX(),
                            vec.getBlockY() + location.getBlockY(),
                            vec.getBlockZ() + location.getBlockZ()),
                    pair.getKey());
        }
    }

    public List<Coordinates> evaluateSegments() {
        if (preEvaluatedSegments == null) {
            List<Vector> blocks = new ArrayList<>();

            for (int y = 0; y < 4; y++) {
                getAllCorners(32 * y, blocks, true);
                getAllCorners(32 * y + 30, blocks, true);
            }

            List<Vector> allBlocks = new ArrayList<>();
            for (int y = 0; y < 4; y++) {
                getAllCorners(32 * y, allBlocks, false);
                getAllCorners(32 * y + 30, allBlocks, false);
            }

            List<Coordinates> segments = new ArrayList<>();
            for (Vector l : allBlocks) {
                List<Vector> matching = new ArrayList<>(Collections.singletonList(l));
                List<Vector> relative = Arrays.asList(new Vector(l.getBlockX() + 30, l.getBlockY(), l.getBlockZ()),
                        new Vector(l.getBlockX(), l.getBlockY(), l.getBlockZ() + 30),
                        new Vector(l.getBlockX() + 30, l.getBlockY(), l.getBlockZ() + 30));

                for (Vector l2 : blocks) {
                    if (relative.stream().anyMatch((l3) -> l3.equals(l2))) {
                        matching.add(l2);
                    }
                }

                if (matching.size() == 4 || matching.size() == 3) {
                    segments.add(new Coordinates(l.getBlockX() / 32, l.getBlockZ() / 32));
                }
            }

            preEvaluatedSegments = segments;

            return segments;
        } else {
            return preEvaluatedSegments;
        }
    }

    private void getAllCorners(int z, List<Vector> blocks, boolean checkForAir) {
        Vector currentLocUpper = new Vector(0, 70, z);

        while (currentLocUpper.getX() <= 127) {
            if ((getBlockAt(currentLocUpper) != null && getBlockAt(currentLocUpper).getId() != 0) || !checkForAir) {
                blocks.add(currentLocUpper);
            }
            currentLocUpper = currentLocUpper.add(30, 0, 0);
            if ((getBlockAt(currentLocUpper) != null && getBlockAt(currentLocUpper).getId() != 0) || !checkForAir) {
                blocks.add(currentLocUpper);
            }
            currentLocUpper = currentLocUpper.add(2, 0, 0);
        }
    }

    public int getWidth() {
        return helperArray.length;
    }

    public int getHeight() {
        return helperArray[0].length;
    }

    public int getLength() {
        return helperArray[0][0].length;
    }

    private BaseBlock getBlockAt(Vector location) {
        return getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static List<Schematic> getLoadedSchematics() {
        for (Schematic s : allSchematics) {
            try {
                s.load(new Vector(0, 0, 0), 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new ArrayList<>(allSchematics);
    }
}
