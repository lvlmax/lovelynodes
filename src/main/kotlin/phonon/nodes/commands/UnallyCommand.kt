/**
 * Commands for breaking alliance with other towns, nations
 */

package phonon.nodes.commands

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.constants.ErrorNotAllies
import phonon.nodes.objects.Nation
import phonon.nodes.objects.Town
import phonon.nodes.utils.string.filterTownOrNation

/**
 * @command /unally
 * Break alliances with towns or nations
 * 
 * @subcommand /unally [town]
 * Break alliance with a town
 * 
 * @subcommand /unally [nation]
 * Break alliance with a nation
 */
public class UnallyCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        // no args, print help
        if ( args.size == 0 ) {
            printHelp(sender)
            return true
        }

        if ( !(sender is Player) ) {
            return true
        }

        val player: Player = sender
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return true
        }
        
        val town = resident.town
        if ( town == null ) {
            return true
        }

        val nation = town.nation
        if ( nation !== null && town !== nation.capital ) {
            Message.error(player, "Solo la town capital de la nación puede ofrecer/aceptar alianzas")
            return true
        }

        if ( resident !== town.leader && !town.officers.contains(resident) ) {
            Message.error(player, "Solo el líder y los officers pueden ofrecer/aceptar alianzas")
            return true
        }

        parseTownOrNationName(player, args, town, nation)

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if ( args.size > 0 ) {
            return filterTownOrNation(args[0])
        }
        
        return listOf()
    }

    private fun printHelp(sender: CommandSender) {
        Message.print(sender, "[Nodes] Comandos de alianzas:")
        Message.print(sender, "/ally [town]${ChatColor.WHITE}: Ofrecer/aceptar alianza con una town")
        Message.print(sender, "/ally [nación]${ChatColor.WHITE}: Ofrecer/aceptar alianza con una nación")
        Message.print(sender, "/unally [town]${ChatColor.WHITE}: Romper alianza con una town")
        Message.print(sender, "/unally [nación]${ChatColor.WHITE}: Romper alianza con una nación")
        return
    }

    private fun parseTownOrNationName(player: Player, args: Array<String>, town: Town, townNation: Nation?) {
        val target = args[0]

        // 1. try nation
        var otherNation = Nodes.nations.get(target)
        if ( otherNation !== null ) {
            breakAlliance(player, town, otherNation.capital, townNation, otherNation)
            return
        }

        // 2. try town
        //    if town has nation, use nation
        val otherTown = Nodes.towns.get(target)
        if ( otherTown !== null ) {
            otherNation = otherTown.nation
            if ( otherNation !== null ) {
                breakAlliance(player, town, otherNation.capital, townNation, otherNation)
            }
            else {
                breakAlliance(player, town, otherTown, townNation, otherNation)
            }
            return
        }
        
        Message.error(player, "La town o nación \"${target}\" no existe")
    }

    // break alliance with other side
    private fun breakAlliance(player: Player, town: Town, other: Town, townNation: Nation?, otherNation: Nation?) {
        if ( town === other ) {
            Message.error(player, "No puedes hacer un tratado de paz contigo mismo")
            return
        }

        val result = Nodes.removeAlly(town, other)
        if ( result.isSuccess ) {
            // add truce period
            Nodes.addTruce(town, other)

            // broadcast message
            val thisSide = if ( townNation !== null ) {
                townNation.name
            } else {
                town.name
            }

            val otherSide = if ( otherNation !== null ) {
                otherNation.name
            } else {
                other.name
            }
            
            Message.print(player, "${ChatColor.DARK_RED}${ChatColor.BOLD}${thisSide} ha roto su alianza con ${otherSide}")
        }
        else {
            when ( result.exceptionOrNull() ) {
                ErrorNotAllies -> Message.error(player, "No estás aliado con esa town o nación")
            }
        }
    }
    
}
