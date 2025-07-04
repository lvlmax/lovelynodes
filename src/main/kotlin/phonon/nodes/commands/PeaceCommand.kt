/**
 * Commands for requesting peace on other towns, nations
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
import phonon.nodes.objects.Nation
import phonon.nodes.objects.Town
import phonon.nodes.utils.string.filterTownOrNation
import phonon.nodes.war.Treaty

/**
 * @command /peace
 * Offer peace to a town or nation
 * 
 * @subcommand /peace [town]
 * Offer peace to a town
 * 
 * @subcommand /peace [nation]
 * Offer peace to a nation
 */
public class PeaceCommand : CommandExecutor, TabCompleter {

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
            Message.error(player, "Solamente la town capital de la nación puede ofrecer/proponer paz")
            return true
        }

        if ( resident !== town.leader && !town.officers.contains(resident) ) {
            Message.error(player, "Solo el lider y los officers pueden ofrecer paz")
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
        Message.print(sender, "[Nodes] Comandos de paz:")
        Message.print(sender, "/peace [town]${ChatColor.WHITE}: Ofrecer/aceptar paz con una town")
        Message.print(sender, "/peace [nación]${ChatColor.WHITE}: Ofrecer/aceptar paz con una nación")
        return
    }

    private fun parseTownOrNationName(player: Player, args: Array<String>, town: Town, townNation: Nation?) {
        val target = args[0]

        // 1. try nation
        var enemyNation = Nodes.nations.get(target)
        if ( enemyNation !== null ) {
            offerPeace(player, town, enemyNation.capital, townNation, enemyNation)
            return
        }

        // 2. try town
        //    if town has nation, use nation
        val enemyTown = Nodes.towns.get(target)
        if ( enemyTown !== null ) {
            enemyNation = enemyTown.nation
            if ( enemyNation !== null ) {
                offerPeace(player, town, enemyNation.capital, townNation, enemyNation)
            }
            else {
                offerPeace(player, town, enemyTown, townNation, enemyNation)
            }
            return
        }
        
        Message.error(player, "Town o nación \"${target}\" no existe")
    }

    // offer peace: open treaty gui dialog
    private fun offerPeace(player: Player, town: Town, enemy: Town, townNation: Nation?, enemyNation: Nation?) {
        if ( town === enemy ) {
            Message.error(player, "No puedes hacer un tratado de paz contigo mismo.")
            return
        }

        // returns true if treaty exists, else false creates new treaty
        val result = Treaty.show(player, town, enemy)
        if ( result === false ) { // print messages indicating new treaty occuring
            val enemyName = if ( enemyNation !== null ) {
                "nación ${enemyNation.name}"
            }
            else {
                "town ${enemy.name}"
            }

            val initiatorName = if ( townNation !== null ) {
                townNation.name
            }
            else {
                town.name
            }

            // print message for town residents
            if ( townNation !== null ) {
                for ( r in townNation.residents ) {
                    val p = r.player()
                    if ( p !== null ) {
                        Message.print(p, "Tú nación está negociando la paz con ${enemyName}")
                    }
                }
            }
            else {
                for ( r in town.residents ) {
                    val p = r.player()
                    if ( p !== null ) {
                        Message.print(p, "Tú town está negociando la paz con ${enemyName}")
                    }
                }
            }
            

            // print message for enemy residents
            if ( enemyNation !== null ) {
                for ( r in enemyNation.residents ) {
                    val p = r.player()
                    if ( p !== null ) {
                        Message.print(p, "${initiatorName} está ofreciendo un tratado de paz, usa \"/peace ${initiatorName}\" para negociar")
                    }
                }
            }
            else {
                for ( r in enemy.residents ) {
                    val p = r.player()
                    if ( p !== null ) {
                        Message.print(p, "${initiatorName} está ofreciendo un tratado de paz, usa \"/peace ${initiatorName}\" para negociar")
                    }
                }
            }
        }
    }
    
}
