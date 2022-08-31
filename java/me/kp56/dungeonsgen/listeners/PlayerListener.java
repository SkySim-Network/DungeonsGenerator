package me.kp56.dungeonsgen.listeners;

import me.kp56.dungeonsgen.DungGenPlugin;
import me.kp56.dungeonsgen.Dungeon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Dungeon currentDungeon = DungGenPlugin.getPlugin().dungeon;

        if (currentDungeon != null) {
            int x1 = event.getFrom().getBlockX() / 32;
            int x2 = event.getTo().getBlockX() / 32;

            int y1 = event.getFrom().getBlockZ() / 32;
            int y2 = event.getTo().getBlockZ() / 32;

            try {
                if (currentDungeon.getRoomLayout()[x1][y1] != currentDungeon.getRoomLayout()[x2][y2]) {
                    event.getPlayer().sendMessage("You have just entered room: " + currentDungeon.getRooms()
                            .get(currentDungeon.getRoomLayout()[x2][y2]) + " (" + currentDungeon.getRoomLayout()[x2][y2] + ")");
                }
            } catch (ArrayIndexOutOfBoundsException ignored) {

            }
        }
    }
}
