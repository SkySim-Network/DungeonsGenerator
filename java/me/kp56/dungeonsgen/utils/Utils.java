package me.kp56.dungeonsgen.utils;

import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.SchematicWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.World;
import me.kp56.dungeonsgen.generator.graphs.Coordinates;
import me.kp56.dungeonsgen.generator.rooms.Door;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
    public static void pasteSchem(Location location, File file, Vector rotateAround, double rotation) throws IOException, WorldEditException {
        Closer closer = Closer.create();
        FileInputStream fis = closer.register(new FileInputStream(file));
        BufferedInputStream bis = closer.register(new BufferedInputStream(fis));

        pasteSchem(location, bis, rotateAround, rotation);

        closer.close();
    }

    public static void pasteSchem(Location location, InputStream stream, Vector rotateAround, double rotation) throws IOException, WorldEditException {
        World world = new BukkitWorld(location.getWorld());

        for (Pair<BaseBlock, Vector> pair : readSchem(location, stream, rotateAround, rotation)) {
            world.setBlock(pair.getValue(), pair.getKey());
        }
    }

    public static List<Pair<BaseBlock, Vector>> readSchem(Location location, InputStream stream, Vector rotateAround, double rotation) throws IOException {
        World world = new BukkitWorld(location.getWorld());
        ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(stream);
        Clipboard clipboard = reader.read(world.getWorldData());

        AffineTransform transform = new AffineTransform();
        transform = transform.rotateY(-rotation);

        BlockTransformExtent transformExtent = new BlockTransformExtent(clipboard, transform,
                world.getWorldData().getBlockRegistry());

        List<Pair<BaseBlock, Vector>> list = new ArrayList<>();
        for (int x = 0; x < clipboard.getRegion().getWidth(); x++) {
            for (int y = 0; y < clipboard.getRegion().getHeight(); y++) {
                for (int z = 0; z < clipboard.getRegion().getLength(); z++) {
                    Vector minPoint = clipboard.getMinimumPoint();
                    Vector clipboardLoc = new Vector(minPoint.getX() + x, minPoint.getY() + y, minPoint.getZ() + z);

                    Vector pasteLoc = new Vector(x, y, z).add(new Vector(location.getX(), location.getY(), location.getZ()));
                    pasteLoc = rotateAroundPoint(pasteLoc, rotateAround, rotation);

                    BaseBlock block = clipboard.getBlock(clipboardLoc);
                    if (block.getId() != 0) {
                        list.add(Pair.of(transformExtent.getBlock(clipboardLoc), pasteLoc));
                    }
                }
            }
        }

        return list;
    }

    public static Vector rotateAroundPoint(Vector vec, Vector rotateAround, double rotation) {
        Vector vec2 = vec.subtract(rotateAround);

        if (rotation == 90) {
            vec2 = new Vector(vec2.getBlockZ(), vec.getBlockY(), -vec2.getBlockX());
        } else if (rotation == 180) {
            vec2 = new Vector(-vec2.getBlockX(), vec.getBlockY(), -vec2.getBlockZ());
        } else if (rotation == 270) {
            vec2 = new Vector(-vec2.getBlockZ(), vec.getBlockY(), vec2.getBlockX());
        } else if (rotation != 0 && rotation != 360) {
            throw new RuntimeException("Rotation has to be a multiple of 90.");
        }

        return vec2.add(rotateAround);
    }

    public static String phraseFromArgs(String[] args, int begin) {
        if (args.length == 0)
            return "";

        StringBuilder str = new StringBuilder(args[begin]);
        for (int i = begin + 1; i < args.length; i++) {
            str.append(" ").append(args[i]);
        }

        return str.toString();
    }

    public static List<Block> getBlocksBetween(Location from, Location to) {
        List<Block> b = new ArrayList<>();

        for (int x = Math.min(from.getBlockX(), to.getBlockX()); x < Math.max(from.getX(), to.getX()); x++) {
            for (int y = Math.min(from.getBlockY(), to.getBlockY()); y < Math.max(from.getBlockY(), to.getBlockY()); y++) {
                for (int z = Math.min(from.getBlockZ(), to.getBlockZ()); z < Math.max(from.getBlockZ(), to.getBlockZ()); z++) {
                    b.add(from.getWorld().getBlockAt(x, y, z));
                }
            }
        }

        return b;
    }

    public static String transform(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
    
    public static void fillArea(Material block, Location from, Location to) {
        for (Block b : getBlocksBetween(from, to)) {
            b.setType(block);
        }
    }

    public static void toSchem(Location from, Location to, File file) throws IOException, WorldEditException {
        Closer closer = Closer.create();
        FileOutputStream fos = closer.register(new FileOutputStream(file));
        DataOutputStream dos = closer.register(new DataOutputStream(fos));

        toSchem(from, to, dos);
    }

    public static void toSchem(Location from, Location to, OutputStream stream) throws WorldEditException, IOException {
        World world = new BukkitWorld(from.getWorld());
        BlockArrayClipboard blockArrayClipboard = new BlockArrayClipboard(new CuboidRegion(world, new Vector(from.getBlockX(), from.getBlockY(), from.getBlockZ()),
                new Vector(to.getBlockX(), to.getBlockY(), to.getBlockZ())));
        for (Block b : getBlocksBetween(from, to)) {
            Vector loc = new Vector(b.getX(), b.getY(), b.getZ());
            BaseBlock block = world.getBlock(loc);
            blockArrayClipboard.setBlock(loc, block);
        }

        Closer closer = Closer.create();

        ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(stream);
        writer.write(blockArrayClipboard, world.getWorldData());

        closer.close();
    }

    public static void possibleEvents(Runnable... runnables) {
        runnables[(int) (Math.random() * runnables.length)].run();
    }

    public static long twoIntsToLong(int x, int y) {
        long a = x;
        a <<= 32;
        a |= y;

        return a;
    }

    public static <T>void iterateThroughBorder(T[][] arr, TwoDimArrElHandler<T> handler) {
        if (arr.length >= 1 && arr[0].length >= 1) {
            for (int x = 0; x < arr.length; x++) {
                if (handler.handle(x, 0, arr[x][0]) || handler.handle(x, arr[0].length - 1, arr[x][arr[0].length - 1]))
                    return;
            }
            
            for (int y = 1; y < arr[0].length - 1; y++) {
                if (handler.handle(0, y, arr[0][y]) || handler.handle(arr.length - 1, y, arr[arr.length - 1][y]))
                    return;
            }
        } else {
            throw new IllegalArgumentException("The two-dimensional array needs to have at least 1 element.");
        }
    }

    public static Integer[][] twoDimIntArrayToInteger(int[][] arr) {
        Integer[][] arr2 = new Integer[arr.length][arr[0].length];

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                arr2[i][j] = arr[i][j];
            }
        }

        return arr2;
    }

    public static int distanceSquared(int x, int y, int x2, int y2) {
        return (x - x2) * (x - x2) + (y - y2) * (y - y2);
    }

    public static Door pairToDoor(Pair<Coordinates, Coordinates> pair) {
        return new Door(pair.getKey(), pair.getValue());
    }

    public static List<Coordinates> getRelCoords() {
        return Arrays.asList(
                new Coordinates(1, 0),
                new Coordinates(0, 1),
                new Coordinates(-1, 0),
                new Coordinates(0, -1)
        );
    }

    public static boolean isInBounds(int x, int y, int[][] arr) {
        return x >= 0 && x < arr.length && y >= 0 && y < arr[0].length;
    }
}
