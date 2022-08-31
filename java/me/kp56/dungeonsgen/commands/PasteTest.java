package me.kp56.dungeonsgen.commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import me.kp56.dungeonsgen.generator.rooms.Schematic;
import me.kp56.dungeonsgen.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class PasteTest implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.isOp()) {
            if (sender instanceof Player) {
                Player p = (Player) sender;

                if (args.length > 0) {
                    String schem = args[0];
                    String rotation = args[1];

                    try {
                        Schematic schematic = new Schematic(p.getWorld(), new File("plugins/dungeonsgen/roomschematics/" + schem + ".schematic"));
                        schematic.load(new Vector(0,0,0), Double.parseDouble(rotation));
                        schematic.paste(p.getLocation());
                    } catch (IOException | WorldEditException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return true;
    }
}
