package vn.sepay.plugin.scheduler;

import org.bukkit.Bukkit;

public class SchedulerAdapter {
    private static IScheduler scheduler;
    private static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static IScheduler getScheduler() {
        if (scheduler == null) {
            if (isFolia) {
                scheduler = new FoliaScheduler();
            } else {
                scheduler = new PaperScheduler();
            }
        }
        return scheduler;
    }

    public static boolean isFolia() {
        return isFolia;
    }
}
