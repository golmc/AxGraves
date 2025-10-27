package com.artillexstudios.axgraves;

import com.artillexstudios.axapi.AxPlugin;
import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.items.WrappedItemStack;
import com.artillexstudios.axapi.libs.boostedyaml.dvs.versioning.BasicVersioning;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.metrics.AxMetrics;
import com.artillexstudios.axapi.utils.MessageUtils;
import com.artillexstudios.axapi.utils.featureflags.FeatureFlags;
import com.artillexstudios.axgraves.commands.Commands;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.GravePlaceholders;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.artillexstudios.axgraves.listeners.DeathListener;
import com.artillexstudios.axgraves.listeners.PlayerInteractListener;
import com.artillexstudios.axgraves.schedulers.SaveGraves;
import com.artillexstudios.axgraves.schedulers.TickGraves;
import com.artillexstudios.axgraves.utils.UpdateNotifier;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class AxGraves extends AxPlugin {
    private static AxPlugin instance;
    public static Config CONFIG;
    public static Config MESSAGES;
    public static MessageUtils MESSAGEUTILS;
    public static ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static AxMetrics metrics;
    public static final List<WrappedItemStack> ITEMS = new ArrayList<>();

    public static AxPlugin getInstance() {
        return instance;
    }

    public void enable() {
        instance = this;
        createItems();

        new Metrics(this, 20332);

        CONFIG = new Config(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        MESSAGES = new Config(new File(getDataFolder(), "messages.yml"), getResource("messages.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());

        MESSAGEUTILS = new MessageUtils(MESSAGES.getBackingDocument(), "prefix", CONFIG.getBackingDocument());

        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);

        BukkitCommandHandler handler = BukkitCommandHandler.create(instance);
        handler.register(new Commands());

        GravePlaceholders.register();

        if (CONFIG.getBoolean("save-graves.enabled", true)) {
            SpawnedGraves.loadFromFile();
        }

        TickGraves.start();
        SaveGraves.start();

        metrics = new AxMetrics(this, 20);
        metrics.start();

        if (CONFIG.getBoolean("update-notifier.enabled", true)) new UpdateNotifier(this, 5076);
    }

    public void disable() {
        if (metrics != null) metrics.cancel();

        TickGraves.stop();
        SaveGraves.stop();

        for (Grave grave : SpawnedGraves.getGraves()) {
            if (!CONFIG.getBoolean("save-graves.enabled", true)) grave.remove();
            if (grave.getEntity() != null) grave.getEntity().remove();
            if (grave.getHologram() != null) grave.getHologram().remove();
            if (grave.getItem() != null) grave.getItem().remove();
        }

        if (CONFIG.getBoolean("save-graves.enabled", true)) {
            SpawnedGraves.saveToFile();
        }

        EXECUTOR.shutdownNow();
    }
    
    private void createItems() {
        ItemStack grave1 = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemStack grave2 = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemStack grave3 = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemStack grave4 = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta meta1 = grave1.getItemMeta();
        meta1.setItemModel(NamespacedKey.fromString("grave/gravestone"));
        grave1.setItemMeta(meta1);
        ItemMeta meta2 = grave2.getItemMeta();
        meta2.setItemModel(NamespacedKey.fromString("grave/gravestone_old"));
        grave2.setItemMeta(meta2);
        ItemMeta meta3 = grave3.getItemMeta();
        meta3.setItemModel(NamespacedKey.fromString("grave/gravestone_forgotten"));
        grave3.setItemMeta(meta3);
        ItemMeta meta4 = grave4.getItemMeta();
        meta4.setItemModel(NamespacedKey.fromString("grave/gravestone_weathered"));
        grave4.setItemMeta(meta4);
        ITEMS.add(WrappedItemStack.wrap(grave1));
        ITEMS.add(WrappedItemStack.wrap(grave2));
        ITEMS.add(WrappedItemStack.wrap(grave3));
        ITEMS.add(WrappedItemStack.wrap(grave4));
    }

    public void updateFlags() {
        FeatureFlags.USE_LEGACY_HEX_FORMATTER.set(true);
        FeatureFlags.PACKET_ENTITY_TRACKER_ENABLED.set(true);
        FeatureFlags.HOLOGRAM_UPDATE_TICKS.set(5L);
        FeatureFlags.ENABLE_PACKET_LISTENERS.set(true);
    }
}
