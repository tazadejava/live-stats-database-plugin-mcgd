package me.tazadejava.main;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles making player not move when challenge is inactive.
 */
public class ChallengeListener implements Listener {

    private LiveStatsDatabasePlugin plugin;

    public ChallengeListener(LiveStatsDatabasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(!plugin.isChallengeActive()) {
            plugin.addPotionEffects(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if(!plugin.isChallengeActive()) {
            plugin.addPotionEffects(event.getPlayer());
        }
    }
}
