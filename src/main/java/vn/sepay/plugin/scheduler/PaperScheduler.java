package vn.sepay.plugin.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class PaperScheduler implements IScheduler {

    @Override
    public void runGlobal(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runEntity(Entity entity, Plugin plugin, Runnable task) {
        // En Paper, todo corre en el main thread, así que es seguro.
        // On Paper, everything runs on the main thread, so runTask is safe for entity operations.
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runRegion(Plugin plugin, Location location, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
