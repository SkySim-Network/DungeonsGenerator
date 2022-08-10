package me.kp56.dungeonsgen.commands;

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
            new Generator(player.getWorld(), 5, 5).generate();

            System.out.println("Generation took: " + (((double) (System.currentTimeMillis() - time)) / 1000d) + " seconds.");
        }

        return true;
    }
}
