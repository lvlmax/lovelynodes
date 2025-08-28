/**
 * Income background scheduler to periodically run
 * message that towns are over max claims
 */

package phonon.nodes.tasks

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import phonon.nodes.Nodes
import phonon.nodes.Config
import java.util.concurrent.TimeUnit


public object OverMaxClaimsReminder {

    private var task: ScheduledTask? = null

    // run scheduler for saving backups
    public fun start(plugin: Plugin, period: Long) {
        if ( this.task !== null ) {
            return
        }

        // scheduler for writing backups
        val task = Runnable { // schedule main thread to run income tick
            Bukkit.getGlobalRegionScheduler().run(plugin) { Nodes.overMaxClaimsReminder() }
        }

        this.task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, {task.run()}, period, period, TimeUnit.SECONDS)
    }

    public fun stop() {
        val task = this.task
        if ( task === null ) {
            return
        }

        task.cancel()
        this.task = null
    }
}