/**
 * Contains all command executors to join/leave
 * ingame chat channels
 */

package phonon.nodes.commands

import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import phonon.nodes.Nodes
import phonon.nodes.Message
import phonon.nodes.objects.Resident
import phonon.nodes.chat.*
import phonon.nodes.utils.string.*

// toggle chat mode then print message
private fun toggleChatMode(player: Player, resident: Resident, chatMode: ChatMode) {

    val newChatMode = Nodes.toggleChatMode(resident, chatMode)
    
    when ( newChatMode ) {
        ChatMode.GLOBAL -> Message.print(player, "${ChatColor.BOLD}Ahora estás hablando en el chat general.")
        ChatMode.TOWN -> Message.print(player, "${ChatColor.DARK_AQUA}${ChatColor.BOLD}Ahora estás hablando en el chat de tú town.")
        ChatMode.NATION -> Message.print(player, "${ChatColor.GOLD}${ChatColor.BOLD}Ahora estás hablando en el chat de la nación.")
        ChatMode.ALLY -> Message.print(player, "${ChatColor.GREEN}${ChatColor.BOLD}Ahora estás hablando en el chat de aliados.")
    }

}

// list of /gc subcommands, used for onTabComplete
private val globalChatSubcommands: List<String> = listOf(
    "join",
    "unmute",
    "mute",
    "leave"
)

/**
 * @command /globalchat (or /gc)
 * Set chat to global
 * 
 * @subcommand /globalchat mute
 * Mute global chat
 * 
 * @subcommand /globalchat unmute
 * Enable global chat
 */
public class GlobalChatCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        if ( !(sender is Player) ) {
            return true
        }

        val player: Player = sender
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return true
        }

        // no args, toggle chat mode
        if ( args.size == 0 ) {
            toggleChatMode(player, resident, ChatMode.GLOBAL)
        }
        else {
            // parse subcommand
            when ( args[0].lowercase() ) {
                "join" -> enableChannel(sender)
                "unmute" -> enableChannel(sender)
                "leave" -> disableChannel(sender)
                "mute" -> disableChannel(sender)
                else -> { Message.error(player, "Comando Inválido") }
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if ( args.size == 1 ) {
            return filterByStart(globalChatSubcommands, args[0])
        }

        return listOf()
    }

    private fun enableChannel(player: Player) {
        Chat.enableGlobalChat(player)
        Message.print(player, "Chat global habilitado")
    }

    private fun disableChannel(player: Player) {
        Chat.disableGlobalChat(player)
        Message.print(player, "Chat global silenciado")
    }
}

/**
 * @command /townchat (or /tc)
 * Set chat to town members only
 */
public class TownChatCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        if ( !(sender is Player) ) {
            return true
        }

        val player: Player = sender
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return true
        }

        // no args, toggle chat mode
        if ( args.size == 0 ) {
            toggleChatMode(player, resident, ChatMode.TOWN)
        }
        else {
            // parse subcommand
            when ( args[0].lowercase() ) {
                "leave" -> leaveChannel(sender)
                else -> { Message.error(player, "Comando Inválido") }
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return listOf()
    }

    private fun leaveChannel(player: Player) {
        // TODO
    }
}

/**
 * @command /nationchat (or /nc)
 * Set chat to all members of nation.
 */
public class NationChatCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        if ( !(sender is Player) ) {
            return true
        }

        val player: Player = sender
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return true
        }

        // no args, toggle chat mode
        if ( args.size == 0 ) {
            toggleChatMode(player, resident, ChatMode.NATION)
        }
        else {
            // parse subcommand
            when ( args[0].lowercase() ) {
                "leave" -> leaveChannel(sender)
                else -> { Message.error(player, "Comando Inválido") }
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return listOf()
    }

    private fun leaveChannel(player: Player) {
        // TODO
    }
}

/**
 * @command /allychat (or /ac)
 * Set chat to town and all allied towns.
 */
public class AllyChatCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        if ( !(sender is Player) ) {
            return true
        }

        val player: Player = sender
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return true
        }

        // no args, toggle chat mode
        if ( args.size == 0 ) {
            toggleChatMode(player, resident, ChatMode.ALLY)
        }
        else {
            // parse subcommand
            when ( args[0].lowercase() ) {
                "leave" -> leaveChannel(sender)
                else -> { Message.error(player, "Comando Inválido") }
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return listOf()
    }

    private fun leaveChannel(player: Player) {
        // TODO
    }
}
