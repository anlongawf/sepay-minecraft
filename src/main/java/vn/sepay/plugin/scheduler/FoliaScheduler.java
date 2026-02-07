package vn.sepay.plugin.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class FoliaScheduler implements IScheduler {

    @Override
    public void runGlobal(Plugin plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void runEntity(Entity entity, Plugin plugin, Runnable task) {
        entity.getScheduler().run(plugin, (t) -> task.run(), null);
    }

    @Override
    public void runRegion(Plugin plugin, Location location, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, (t) -> task.run());
    }

    @Override
    public void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (t) -> task.run(), delayTicks);
    }
}
