package vn.sepay.plugin.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface IScheduler {
    
    /**
     * Run a task that modifies the world/entity state globally or at main thread.
     */
    void runGlobal(Plugin plugin, Runnable task);

    /**
     * Run a task for a specific entity (Player, Mob, etc).
     */
    void runEntity(Entity entity, Plugin plugin, Runnable task);
    
    /**
     * Run a task at a specific location (Block).
     */
    void runRegion(Plugin plugin, Location location, Runnable task);

    /**
     * Run an asynchronous task (Database, Network).
     */
    void runAsync(Plugin plugin, Runnable task);
    
    /**
     * Schedule a global task to run later.
     */
    void runTaskLater(Plugin plugin, Runnable task, long delayTicks);
}
