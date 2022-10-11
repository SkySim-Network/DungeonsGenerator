package me.kp56.dungeonsgen;

import com.avaje.ebean.validation.Past;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.SchematicWriter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.io.Closer;
import lombok.Getter;
import me.kp56.dungeonsgen.commands.GenerateDungeon;
import me.kp56.dungeonsgen.commands.PasteTest;
import me.kp56.dungeonsgen.generator.rooms.Schematic;
import me.kp56.dungeonsgen.listeners.PlayerListener;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public class DungGenPlugin extends JavaPlugin {
    @Getter
    private static DungGenPlugin plugin;

    @Getter
    private MultiverseCore core;
    @Getter
    private World dungeonWorld;

    public Dungeon dungeon;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        getCommand("generatedungeon").setExecutor(new GenerateDungeon());
        getCommand("pastetest").setExecutor(new PasteTest());

        core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        File[] dirs = new File[]{
                new File("plugins/dungeonsgen"),
                new File("plugins/dungeonsgen/roomschematics"),
        };

        for (File dir : dirs) {
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        dungeonWorld = Bukkit.getWorld("world");
        plugin = this;

        //fix all schematics
        for (Schematic schematic : Schematic.getLoadedSchematics()) {
            try {
                Vector foundSign = schematic.getMinPoint();
                for (Pair<BaseBlock, Vector> pair : schematic.getBlocks()) {
                    if (pair.getKey().getNbtData() != null) {
                        if (pair.getKey().getNbtData().getString("Text1").equalsIgnoreCase("{\"extra\":[\"door::this\"],\"text\":\"\"}")) {
                            foundSign = pair.getValue();
                        }
                    }
                }

                if (foundSign.getY() != 70) {
                    int increaseBy = (int) (70 - foundSign.getY());

                    Closer closer = Closer.create();
                    FileOutputStream fos = closer.register(new FileOutputStream(schematic.file));
                    DataOutputStream dos = closer.register(new DataOutputStream(fos));

                    BukkitWorld world = new BukkitWorld(dungeonWorld);
                    BlockArrayClipboard blockArrayClipboard = new BlockArrayClipboard(new CuboidRegion(world,
                            new Vector(schematic.getMinPoint().getBlockX(), schematic.getMinPoint().getBlockY() + increaseBy, schematic.getMinPoint().getBlockZ()),
                            new Vector(schematic.getMaxPoint().getBlockX(), schematic.getMaxPoint().getBlockY() + increaseBy, schematic.getMaxPoint().getBlockZ())));

                    for (Pair<BaseBlock, Vector> pair : schematic.getBlocks()) {
                        Vector newVector = new Vector(pair.getValue().getX(), pair.getValue().getY() + increaseBy,
                                pair.getValue().getZ());

                        //I need to save a new schematic with the block pair.getKey() and its location in newVector
                        if (!blockArrayClipboard.setBlock(newVector, pair.getKey())) {
                            System.out.println("There is a problem with World Edit regions in schematic: " + schematic.file);
                            System.out.println("Tried to paste block at position: " + newVector + " minPoint: "
                                    + blockArrayClipboard.getMinimumPoint() + " maxPoint: " + blockArrayClipboard.getMaximumPoint());
                        }
                    }

                    ClipboardWriter writer = closer.register(ClipboardFormat.SCHEMATIC.getWriter(dos));
                    writer.write(blockArrayClipboard, world.getWorldData());

                    closer.close();
                }
            } catch (IOException | WorldEditException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
