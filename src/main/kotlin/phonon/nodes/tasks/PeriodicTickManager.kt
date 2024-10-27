/**
 * Centralized handler for long periodic tasks:
 * - Income, backup, cooldowns
 */

package phonon.nodes

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import phonon.nodes.Nodes
import phonon.nodes.tasks.FileWriteTask
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


public object PeriodicTickManager {

    private var task: ScheduledTask? = null

    // previous tick time
    private var previousTime: Long = 0L

    // run scheduler for saving backups
    public fun start(plugin: Plugin, period: Long) {
        if ( this.task !== null ) {
            return
        }
        
        // initialize previous time
        previousTime = System.currentTimeMillis()

        // scheduler for writing backups
        val task = Runnable { // update time tick
            // capture previousTime locally so that functions that require dt = currTime - prevTime
            // have safe previous time value, in the rare/~impossible case that the main thread
            // lags so much that this runs and sets previousTime again before scheduled tasks run
            val currTime = System.currentTimeMillis()
            val capturedPreviousTime = previousTime
            previousTime = currTime

            // =================================
            // backup cycle
            // =================================
            if ( currTime > Nodes.lastBackupTime + Config.backupPeriod ) {
                Nodes.lastBackupTime = currTime
                Nodes.doBackup()

                // save current time
                Bukkit.getAsyncScheduler().runNow(Nodes.plugin!!) { FileWriteTask(currTime.toString(), Config.pathLastBackupTime) }
            }

            // =================================
            // income cycle
            // =================================
            if ( currTime > Nodes.lastIncomeTime + Config.incomePeriod ) {
                Nodes.lastIncomeTime = currTime

                // schedule main thread to run task
                Bukkit.getGlobalRegionScheduler().run(plugin) { Nodes.runIncome() }

                // save current time
                Bukkit.getAsyncScheduler().runNow(Nodes.plugin!!) {
                    FileWriteTask(
                        currTime.toString(),
                        Config.pathLastIncomeTime
                    )
                }
            }

            // =================================
            // town, resident, truce cooldowns
            // pipeline by running on offset
            // =================================
            Bukkit.getGlobalRegionScheduler().run(plugin) {
                val currTime = System.currentTimeMillis()
                val dt = currTime - capturedPreviousTime
                Nodes.townMoveHomeCooldownTick(dt)
            }
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, {
                val currTime = System.currentTimeMillis()
                val dt = currTime - capturedPreviousTime
                Nodes.claimsPowerRamp(dt)
            }, 1L)
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, {
                val currTime = System.currentTimeMillis()
                val dt = currTime - capturedPreviousTime
                Nodes.claimsPenaltyDecay(dt)
            }, 2L)
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, {
                val currTime = System.currentTimeMillis()
                val dt = currTime - capturedPreviousTime
                Nodes.residentTownCreateCooldownTick(dt)
            }, 3L)
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { Nodes.truceTick() }, 4L)
        }

        val convPeriod = period * 50
        this.task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, {task.run()}, convPeriod, convPeriod, TimeUnit.MILLISECONDS)
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