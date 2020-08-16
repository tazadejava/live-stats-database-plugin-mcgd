package me.tazadejava.main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class LiveStatsDatabasePlugin extends JavaPlugin {

    private LogAppender appender;

    public static final String CHALLENGE_WAIT_MESSAGE = ChatColor.LIGHT_PURPLE + "The challenge will start shortly. Please standby...";
    public static final String CHALLENGE_START_MESSAGE = "" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "The challenge has started!";

    private String serverGroup;
    private boolean isChallengeCurrentlyActive;

    @EventHandler
    public void onEnable() {
        String[] data = loadConfigDatabase();
        hookServerLogger(data);

        getServer().getPluginManager().registerEvents(new ChallengeListener(this), this);

        isChallengeCurrentlyActive = isChallengeActive();

        if(!isChallengeCurrentlyActive) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                addPotionEffects(p);
            }
        }

        //Handles challenge active status. Will prevent players from moving if the challenge is inactive.
        new BukkitRunnable() {

            int count = 0;

            @Override
            public void run() {
                if(isChallengeActive()) {
                    if(!isChallengeCurrentlyActive) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            for (PotionEffect effect : p.getActivePotionEffects()) {
                                p.removePotionEffect(effect.getType());
                            }

                            p.sendMessage(CHALLENGE_START_MESSAGE);
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
                        }
                    }
                    isChallengeCurrentlyActive = true;
                } else {
                    if(isChallengeCurrentlyActive) {
                        isChallengeCurrentlyActive = false;

                        for(Player p : Bukkit.getOnlinePlayers()) {
                            addPotionEffects(p);
                        }
                    }

                    count++;

                    if(count % 20 == 0) {
                        for(Player p : Bukkit.getOnlinePlayers()) {
                            if(p.isOp()) {
                                continue;
                            }

                            p.sendMessage(CHALLENGE_WAIT_MESSAGE);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 10L);
    }

    public void addPotionEffects(Player p) {
        for(PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }

        p.sendMessage(CHALLENGE_WAIT_MESSAGE);

        if(p.isOp()) {
            p.sendMessage("Warning that the server is currently offline. Further messages will be silenced.");
            return;
        }

        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 100000, 1000, false));
    }

    /**
     * Requires backend webserver to be online. Will check if challenge is currently active or not.
     * @return
     */
    public boolean isChallengeActive() {
        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/challenge");
            URLConnection req = url.openConnection();
            req.connect();

            JsonObject root = new JsonParser().parse(new InputStreamReader((InputStream) req.getContent())).getAsJsonObject();

            return root.get("active").getAsBoolean();
        } catch(ConnectException ex) {
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Requires the Mongo database to be defined under the TeamChallengeMCGD config, under the value "mongo-database".
     * @return
     */
    private String[] loadConfigDatabase() {
        File configFile = new File(getDataFolder().getParentFile().getAbsolutePath() + "/TeamChallengeMCGD/config.yml");

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);

            serverGroup = config.getString("current-group");
            return new String[] {config.getString("mongo-database"), serverGroup};
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Allows us to log everything that happens on the server to post on the frontend website to view.
     * @param data
     */
    private void hookServerLogger(String[] data) {
        appender = new LogAppender(data[0], data[1]);
        ((Logger) LogManager.getRootLogger()).addAppender(appender);
    }

    @EventHandler
    public void onDisable() {
        ((Logger) LogManager.getRootLogger()).removeAppender(appender);
    }
}
