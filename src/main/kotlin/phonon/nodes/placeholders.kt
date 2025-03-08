package phonon.nodes;
package at.helpch.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

import com.google.gson.JsonObject
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import phonon.nodes.chat.ChatMode
import phonon.nodes.constants.*
import phonon.nodes.event.AllianceCreatedEvent
import phonon.nodes.event.TruceExpiredEvent
import phonon.nodes.listeners.NodesPlayerChestProtectListener
import phonon.nodes.objects.*
import phonon.nodes.serdes.Deserializer
import phonon.nodes.serdes.Serializer
import phonon.nodes.tasks.FileWriteTask
import phonon.nodes.tasks.NodesDynmapJsonWriter
import phonon.nodes.tasks.OverMaxClaimsReminder
import phonon.nodes.tasks.SaveManager
import phonon.nodes.utils.Color
import phonon.nodes.utils.sanitizeString
import phonon.nodes.utils.saveStringToFile
import phonon.nodes.war.FlagWar
import phonon.nodes.war.Truce
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.ThreadLocalRandom
import java.util.logging.Logger
import kotlin.system.measureNanoTime

public class NodesPAPI extends Placeholderexpansion implements Listener {
  
  public String getIdentifier() {
    return "nodes_nation";
  }

  public String onRequest(OfflinePlayer player, String identifier) {
      if (identifier.equals("nodes_town")) {
          return resident.town();
      }
      return null;
  }

  public String onRequest(OfflinePlayer player, String identifier) {
      if (identifier.equals("nodes_nation")) {
          return resident.nation();
      }
      return null;
  }
}  
