package me.kp56.dungeonsgen.commands;

import me.kp56.dungeonsgen.DungGenPlugin;
import me.kp56.dungeonsgen.Dungeon;
import me.kp56.dungeonsgen.generator.Generator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GenerateDungeon implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            System.out.println("Started generation...");
            long time = System.currentTimeMillis();
            DungGenPlugin.getPlugin().dungeon = new Generator(player.getWorld(), 5, 5).generate();

            System.out.println("Generation took: " + (((double) (System.currentTimeMillis() - time)) / 1000d) + " seconds.");

            Dungeon dungeon = DungGenPlugin.getPlugin().dungeon;
            for (int y = 0; y < dungeon.getRoomLayout().length; y++) {
                StringBuilder str = new StringBuilder();
                for (int x = 0; x < dungeon.getRoomLayout()[y].length; x++) {
                    str.append(dungeon.getRoomLayout()[x][y]).append((dungeon.getRoomLayout()[x][y] < 10) ? "  " : " ");
                }
                System.out.println(str);
            }
        }

        return true;
    }
}
