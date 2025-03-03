/**
 * /nation (/n) command
 */

package phonon.nodes.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import phonon.nodes.Nodes
import phonon.nodes.Config
import phonon.nodes.Message
import phonon.nodes.objects.Nation
import phonon.nodes.constants.*
import phonon.nodes.utils.sanitizeString
import phonon.nodes.utils.stringInputIsValid
import phonon.nodes.utils.string.*
import java.util.concurrent.TimeUnit

// list of all subcommands, used for onTabComplete
private val subcommands: List<String> = listOf(
    "help",
    "ayuda",
    "leave",
    "capital",
    "accept",
    "deny",
    "reject",
    "list",
    "online",
    "info",
    "spawn"
)

public class NationCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        val player = if ( sender is Player ) sender else null
    
        // no args, print current nation info
        if ( args.size == 0 ) {
            if ( player != null ) {
                // print player's nation info
                val resident = Nodes.getResident(player)
                if ( resident != null && resident.nation != null ) {
                    resident.nation!!.printInfo(player)
                }
                Message.print(player, "Usa \"/nation ayuda\" para ver los comandos")
            }
            return true
        }

        // parse subcommand
        when ( args[0].lowercase() ) {
            "help" -> printHelp(sender)
            "ayuda" -> printHelp(sender)
            "leave" -> leaveNation(player)
            "capital" -> setCapital(player, args)
            "accept" -> accept(player)
            "deny" -> deny(player)
            "reject" -> deny(player)
            "list" -> listNations(player)
            "online" -> getOnline(player, args)
            "info" -> getInfo(player, args)
            "spawn" -> goToNationTownSpawn(player, args)
            else -> { Message.error(player, "Comando Inválido, usa /nation ayuda") }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        val player: Player = if ( sender is Player ) {
            sender
        } else {
            return listOf()
        }

        // match subcommand
        if ( args.size == 1 ) {
            return filterByStart(subcommands, args[0])
        }
        // match each subcommand format
        else if ( args.size > 1 ) {
            // handle specific subcommands
            when ( args[0].lowercase() ) {

                // /nation invite town
                "invite" -> {
                    if ( args.size == 2 ) {
                        return filterTown(args[1])
                    }
                }

                // /nation [subcommand] [nation]
                "list",
                "online",
                "info" -> {
                    if ( args.size == 2 ) {
                        return filterNation(args[1])
                    }
                }

                // /nation [subcommand] [nation town]
                "capital",
                "spawn" -> {
                    if ( args.size == 2 ) {
                        val nation = Nodes.getResident(player)?.nation
                        if ( nation !== null ) {
                            return filterNationTown(nation, args[1])
                        }
                    }
                }
            }
        }

        return listOf()
    }

    private fun printHelp(sender: CommandSender) {
        Message.print(sender, "${ChatColor.BOLD}[Nodes] Comandos de nación:")
        Message.print(sender, "/nation leave${ChatColor.WHITE}: Abandonar tú nación")
        Message.print(sender, "/nation list${ChatColor.WHITE}: Lista de naciones")
        Message.print(sender, "¿Buscas los comandos de la town?${ChatColor.WHITE} Usa /town ayuda")
        return
    }

    /**
     * @command /nation create [name]
     * Create a new nation with your town as capital.
     */
    private fun createNation(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        if ( args.size < 2 ) {
            Message.print(player, "Uso: ${ChatColor.WHITE}/nation create [nombre]")
            return
        }

        // do not allow during war
        if ( Nodes.war.enabled == true ) {
            Message.error(player, "No se pueden crear naciones durante guerra")
            return
        }
        
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "Necesitas una town para formar una nación")
            return
        }

        // only allow leaders to create nation
        if ( resident !== town.leader ) {
            Message.error(player, "Solamente el líder de la town puede crear una nación")
            return
        }

        val name = args[1]
        if ( !stringInputIsValid(name) ) {
            Message.error(player, "Nombre de nación inválido")
            return
        }

        val result = Nodes.createNation(sanitizeString(name), town, resident)
        if ( result.isSuccess ) {
            Message.broadcast("${ChatColor.BOLD}¡La nación ${name} ha sido formada por ${town.name}!")
        }
        else {
            when ( result.exceptionOrNull() ) {
                ErrorNationExists -> Message.error(player, "La nación \"${name}\" ya existe")
                ErrorTownHasNation -> Message.error(player, "Ya perteneces a una nación")
                ErrorPlayerHasNation -> Message.error(player, "Ya perteneces a una nación")
            }
        }
    }

    /**
     * @command /nation delete
     * Delete your nation. Leader of capital town only.
     */
    private fun deleteNation(player: Player?) {
        if ( player == null ) {
            return
        }

        // check if player is nation leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val nation = resident.nation
        if ( nation == null ) {
            Message.error(player,"No perteneces a una nación")
            return
        }

        val leader = nation.capital.leader
        if ( resident !== leader ) {
            Message.error(player,"No eres el líder de la nación")
            return
        }

        // do not allow during war
        if ( Nodes.war.enabled == true ) {
            Message.error(player, "No puedes borrar tú nación durante guerra")
            return
        }
        
        Nodes.destroyNation(nation)
        Message.broadcast("${ChatColor.DARK_RED}${ChatColor.BOLD}La nación de ${nation.name} ha capitulado")
    }

    /**
     * @command /nation leave
     * Leave your nation. Used by town leaders only.
     */
    private fun leaveNation(player: Player?) {
        if ( player == null ) {
            return
        }

        // check if player is nation leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player,"No perteneces a una town")
            return
        }

        val nation = town.nation
        if ( nation == null ) {
            Message.error(player,"No perteneces a una nación")
            return
        }

        if ( town === nation.capital ) {
            Message.error(player, "La capital de la nación no puede irse (usa /n delete)")
            return
        }

        val leader = town.leader
        if ( resident !== leader ) {
            Message.error(player,"Tú no eres el líder de la nación")
            return
        }

        // do not allow during war
        if ( Nodes.war.enabled == true && Config.canLeaveNationDuringWar == false ) {
            Message.error(player, "No puedes abandonar a tú nación durante la guerra")
            return
        }
        
        // remove town
        Nodes.removeTownFromNation(nation, town)

        for ( r in town.residents ) {
            val p = r.player()
            if ( p != null ) {
                Message.print(p, "${ChatColor.BOLD}${ChatColor.DARK_RED}Tú town ha abandonado la nación ${ChatColor.WHITE}${nation.name}")
            }
        }
    }

    /**
     * @command /nation capital [town]
     * Set another town in your nation as its capital
     */
    private fun setCapital(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val nation = resident.nation
        if ( nation == null ) {
            Message.error(player,"No perteneces a una nación")
            return
        }

        val leader = nation.capital.leader
        if ( resident !== leader ) {
            Message.error(player, "Solo los líderes de la nación pueden cambiar la capital")
            return
        }

        if ( args.size < 2 ) {
            Message.print(player, "Uso: ${ChatColor.WHITE}/nation capital [town]")
            return
        }

        val newCapital = Nodes.getTownFromName(args[1])
        if ( newCapital === null) {
            Message.error(player, "Esa town no existe")
            return
        }
        if ( newCapital.nation !== nation ) {
            Message.error(player, "Esa town no pertenece a esta nación")
            return
        }
        if ( newCapital === nation.capital ) {
            Message.error(player, "Esa town ya es la capital de la nación")
            return
        }

        Nodes.setNationCapital(nation, newCapital)
        
        // broadcast message
        Message.broadcast("${ChatColor.BOLD}${newCapital.name} ahora es la capital de ${nation.name}")
    }

    /**
     * @command /nation invite [town]
     * Invite another town to join your nation. Leader of capital town only.
     */
    private fun inviteToNation(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val nation = resident.nation
        if ( nation == null ) {
            Message.error(player,"No perteneces a una nación")
            return
        }

        val leader = nation.capital.leader
        if ( resident !== leader ) {
            Message.error(player, "Solo los líderes de la nación pueden invitar towns")
            return
        }

        if ( args.size < 2 ) {
            Message.print(player, "Uso: ${ChatColor.WHITE}/nation invite [town]")
            return
        }

        val inviteeTown = Nodes.getTownFromName(args[1])
        if ( inviteeTown == null) {
            Message.error(player, "Esa town no existe")
            return
        }
        if ( inviteeTown.nation != null ) {
            Message.error(player, "Esa town pertenece a otra nación")
            return
        }

        val inviteeResident = inviteeTown.leader
        if ( inviteeResident == null ) {
            Message.error(player, "Esa town no tiene líder (?)")
            return
        }
        val invitee: Player? = Bukkit.getPlayer(inviteeResident.uuid)
        if ( invitee == null) {
            Message.error(player, "El líder de la town no está online")
            return
        }

        Message.print(player, "${inviteeTown.name} fue invitado a tú nación.")
        Message.print(invitee, "Tú town ha sido invitada a la nación ${nation.name} por ${player.name}. \nEscribe \"/n accept\" para aceptar o \"/n reject\" para rechazar la oferta.")
        inviteeResident.invitingNation = nation
        inviteeResident.invitingTown = inviteeTown
        inviteeResident.invitingPlayer = player
        inviteeResident.inviteThread = Bukkit.getAsyncScheduler().runDelayed(Nodes.plugin!!, {
                Bukkit.getGlobalRegionScheduler().run(Nodes.plugin!!) {
                    if (inviteeResident.invitingPlayer == player) {
                        Message.print(player, "¡${invitee.name} no ha respondido a la invitación de tú nación!")
                        inviteeResident.invitingNation = null
                        inviteeResident.invitingTown = null
                        inviteeResident.invitingPlayer = null
                        inviteeResident.inviteThread = null
                    }
                }
        }, 1200 * 50, TimeUnit.MILLISECONDS)
    }

    private fun accept(player: Player?) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        if ( resident.town != resident.invitingTown ) {
            Message.error(player, "Invitación inválida")
            return
        }

        if ( resident.invitingNation == null ) {
            Message.error(player,"No has sido invitado a ninguna nación")
            return
        }

        Message.print(player,"${resident.town?.name} está ahora en la jurisdicción de ${resident.invitingNation?.name}!")
        Message.print(resident.invitingPlayer, "¡${resident.town?.name} ha aceptado tú autoridad!")

        Nodes.addTownToNation(resident.invitingNation!!,resident.town!!)
        resident.invitingNation = null
        resident.invitingTown = null
        resident.invitingPlayer = null
        resident.inviteThread = null
    }

    private fun deny(player: Player?) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        if ( resident.invitingNation == null ) {
            Message.error(player,"No has sido invitado a ninguna nación")
            return
        }

        Message.print(player,"¡Has rechazado la invitación a ${resident.invitingNation?.name}!")
        Message.print(resident.invitingPlayer, "¡${resident.town?.name} ha rechazado tú autoridad!")

        resident.invitingNation = null
        resident.invitingTown = null
        resident.invitingPlayer = null
        resident.inviteThread = null
    }

    /**
     * @command /nation list
     * View list of all established nations and their towns
     */
    private fun listNations(player: Player?) {
        Message.print(player, "${ChatColor.BOLD}Nación - Población - Towns")
        val nationsList = ArrayList(Nodes.nations.values)
        nationsList.sortByDescending { it.residents.size }
        for ( n in nationsList ) {
            val townsList = ArrayList(n.towns)
            townsList.sortByDescending { it.residents.size }
            var towns = ""
            for ( (i, t) in townsList.withIndex() ) {
                towns += t.name
                towns += " (${t.residents.size})"
                if ( i < n.towns.size - 1 ) {
                    towns += ", "
                }
            }
            Message.print(player, "${n.name} ${ChatColor.WHITE}- ${n.residents.size} - ${towns}")
        }
    }

    /**
     * @command /nation color [r] [g] [b]
     * Set territory color on dynmap for all towns in nation. Leader of capital town only.
     */
    private fun setColor(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        if ( args.size < 4 ) {
            Message.print(player, "Uso: ${ChatColor.WHITE}/nation color [r] [g] [b]")
            return
        }

        // check if player is town leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val nation = resident.nation
        if ( nation == null ) {
            return
        }

        val leader = nation.capital.leader
        if ( resident !== leader ) {
            Message.error(player,"Solo los líderes de nación pueden hacer esto")
            return
        }

        // parse color
        try {
            val r = args[1].toInt().coerceIn(0, 255)
            val g = args[2].toInt().coerceIn(0, 255)
            val b = args[3].toInt().coerceIn(0, 255)
            
            Nodes.setNationColor(nation, r, g, b)
            Message.print(player, "Color de nación: ${ChatColor.WHITE}${r} ${g} ${b}")
        }
        catch (e: NumberFormatException) {
            Message.error(player, "Color inválido (debe de ser [r] [g] [b] en un rango entre 0-255)")
        }
    }

    /**
     * @command /nation rename [name]
     * Renames your nation. Leader of capital town only.
     */
    private fun renameNation(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        if ( args.size == 1 ) {
            Message.print(player, "Uso: /n rename [nuevo_nombre]")
            return
        }

        val nation = resident.nation
        if ( nation == null ) {
            Message.error(player, "No perteneces a una nación")
            return
        }

        if ( resident != nation.capital.leader ) {
            Message.error(player, "Solo los líderes pueden hacer esto")
            return
        }

        val name = args[1]
        if ( !stringInputIsValid(name) ) {
            Message.error(player, "Nombre de nación inválida")
            return
        }

        if ( nation.name.lowercase() == args[1].lowercase() ) {
            Message.error(player, "Tú nación ya se llama ${nation.name}")
            return
        }

        if ( Nodes.nations.containsKey(args[1]) ) {
            Message.error(player, "Ya hay una nación con este nombre")
            return
        }

        Nodes.renameNation(nation,name)
        Message.print(player, "Nación renombrada a ${nation.name}!")
    }

    /**
     * @command /nation online
     * View your nation's online players
     * @subcommand /nation online [nation]
     * View another nation's online players
     */
    private fun getOnline(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null) {
            return
        }

        var nation: Nation? = null
        if ( args.size == 1 ) {
            if ( resident.nation == null ) {
                Message.error(player, "No perteneces a una nación")
                return
            }
            nation = resident.nation
        } else if ( args.size == 2 ) {
            if ( !Nodes.nations.containsKey(args[1]) ) {
                Message.error(player, "Esa nación no existe")
                return
            }
            nation = Nodes.getNationFromName(args[1])
        } else {
            Message.error(player, "Uso: /nation online [nation]")
            return
        }

        if ( nation == null ) {
            return
        }

        val numPlayersOnline = nation.playersOnline.size
        val playersOnline = nation.playersOnline.map({p -> p.name}).joinToString(", ")
        Message.print(player, "Jugadores online en ${nation.name} [${numPlayersOnline}]: ${ChatColor.WHITE}${playersOnline}")
    }

    /**
     * @command /nation info [nation]
     * View nation's info
     */
    private fun getInfo(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null) {
            return
        }

        var nation: Nation? = null
        if ( args.size == 1 ) {
            if ( resident.nation == null ) {
                Message.error(player, "No perteneces a una nación")
                return
            }
            nation = resident.nation
        } else if ( args.size == 2 ) {
            if ( !Nodes.nations.containsKey(args[1]) ) {
                Message.error(player, "Esa nación no existe")
                return
            }
            nation = Nodes.getNationFromName(args[1])
        } else {
            Message.error(player, "Uso: /nation info [nación]")
            return
        }

        nation?.printInfo(player)
    }

    /**
     * @command /nation spawn [town]
     * Teleport to town inside your nation. May cost items to use.
     */
    private fun goToNationTownSpawn(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        if ( !Config.allowNationTownSpawn ) {
            Message.error(player, "El servidor ha deshabilitado el TP a otras towns en tú nación")
            return
        }

        if ( args.size < 2 ) {
            Message.error(player, "Uso: /nation spawn [town]")
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident === null ) {
            return
        }

        val town = resident.town
        if ( town === null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        val nation = town.nation
        if ( nation === null ) {
            Message.error(player, "No eres miembro de una nación")
            return
        }

        val destinationName = args[1]
        val destinationTown = Nodes.getTownFromName(destinationName)
        if ( destinationTown === null ) {
            Message.error(player, "La town destinataria no existe: ${destinationName}")
            return
        }

        if ( destinationTown === town ) {
            Message.error(player, "La town destinataria es tú town, usa /town spawn")
            return
        }

        val destinationNation = destinationTown.nation
        if ( nation !== destinationNation ) {
            Message.error(player, "La town destinataria no está en la misma nación: ${destinationName}")
            return
        }
        
        // already teleporting
        if ( resident.teleportThread !== null ) {
            Message.error(player, "¡Ya estás intentando teletransportarte!")
            return
        }

        // pay item cost to teleport
        if ( Config.nationTownTeleportCost.size > 0 ) {
            val inventory = player.getInventory()
            for ( (material, amount) in Config.nationTownTeleportCost ) {
                val items = ItemStack(material)
                if ( !inventory.containsAtLeast(items, amount) ) {
                    Message.error(player, "No tienes el pagamiento requerido para teletransportarte: ${Config.nationTownTeleportCostString}")
                    return
                }
            }

            // subtract cost
            for ( (material, amount) in Config.nationTownTeleportCost ) {
                val items = ItemStack(material, amount)
                inventory.removeItem(items)
            }
        }

        // location destination
        val destination = destinationTown.spawnpoint

        // ticks before teleport timer runs
        val teleportTimerTicks: Long = Math.max(0, Config.townSpawnTime * 20).toLong()

        resident.isTeleportingToNationTown = true

        resident.teleportThread = Bukkit.getAsyncScheduler().runDelayed(Nodes.plugin!!, {
                player.scheduler.run(Nodes.plugin!!, { _ ->
                    player.teleportAsync(destination)
                    resident.teleportThread = null
                    resident.isTeleportingToNationTown = false
                }, null)
        }, teleportTimerTicks * 50, TimeUnit.MILLISECONDS)

        if ( teleportTimerTicks > 0 ) {
            Message.print(player, "Teletransportando a ${destinationName} en ${Config.townSpawnTime} segundos. No te muevas...")
        }
    }
}
