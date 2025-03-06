/**
 * /town (/s) command
 */

package phonon.nodes.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import phonon.nodes.Config
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.WorldMap
import phonon.nodes.constants.*
import phonon.nodes.objects.Coord
import phonon.nodes.objects.Resident
import phonon.nodes.objects.Town
import phonon.nodes.utils.sanitizeString
import phonon.nodes.utils.string.filterByStart
import phonon.nodes.utils.string.filterResident
import phonon.nodes.utils.string.filterTown
import phonon.nodes.utils.string.filterTownResident
import phonon.nodes.utils.stringInputIsValid
import java.util.concurrent.TimeUnit

// list of all subcommands, used for onTabComplete
private val SUBCOMMANDS: List<String> = listOf(
    "help",
    "ayuda",
    "officer",
    "promote",
    "demote",
    "apply",
    "join",
    "invite",
    "invitar",
    "accept",
    "deny",
    "reject",
    "leave",
    "kick",
    "spawn",
    "setspawn",
    "list",
    "info",
    "online",
    "income",
    "prefix",
    "suffix",
    "map",
    "minimap",
    "permissions",
    "permisos",
    "protect",
    "trust",
    "untrust",
    "anexar",
    "annex",
    "outpost"
)

private val OUTPOST_SUBCOMMANDS: List<String> = listOf(
    "list",
    "setspawn"
)

// town permissions types
private val PERMISSIONS_TYPES: List<String> = listOf(
    "build",
    "destroy",
    "interact",
    "chests",
    "items",
    "income"
)

// town permissions types
private val PERMISSIONS_GROUPS: List<String> = listOf(
    "town",
    "nation",
    "ally",
    "outsider",
    "trusted"
)

// town permissions flag
private val PERMISSIONS_FLAGS: List<String> = listOf(
    "allow",
    "deny"
)


// ==================================================
// Constants for /t map
// 
// symbols
val SHADE = "\u2592"      // medium shade
val HOME = "\u2588"       // full solid block
val CORE = "\u256B"       // core chunk H
val CONQUERED0 = "\u2561" // captured chunk
val CONQUERED1 = "\u255F" // other chunk flag symbol
val SPACER = "\u17f2"     // spacer

val MAP_STR_BEGIN = "    "

val MAP_STR_END = arrayOf(
    "",
    "       ${ChatColor.GOLD}N",
    "     ${ChatColor.GOLD}W + E",
    "       ${ChatColor.GOLD}S",
    "",
    "  ${ChatColor.GRAY}${SHADE}${ChatColor.DARK_GRAY}${SHADE} ${ChatColor.GRAY}- Unclaimed",
    "  ${ChatColor.GREEN}${SHADE}${ChatColor.DARK_GREEN}${SHADE} - Town",
    "  ${ChatColor.YELLOW}${SHADE}${ChatColor.GOLD}${SHADE} - Neutral",
    "  ${ChatColor.AQUA}${SHADE}${ChatColor.DARK_AQUA}${SHADE} - Ally",
    "  ${ChatColor.RED}${SHADE}${ChatColor.DARK_RED}${SHADE} - Enemy",
    "",
    "  ${ChatColor.WHITE}${HOME} - Home territory",
    "  ${ChatColor.WHITE}${CORE} - Core chunk",
    "  ${ChatColor.WHITE}${CONQUERED0},${CONQUERED1} - Captured",
    "",
    "",
    "",
    ""
)
// ==================================================


public class TownCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        
        val player = if ( sender is Player ) sender else null
    
        // no args, print current town info
        if ( args.size == 0 ) {
            if ( player != null ) {
                // print player's town info
                val resident = Nodes.getResident(player)
                if ( resident != null && resident.town != null ) {
                    resident.town!!.printInfo(player)
                }
                Message.print(player, "Use \"/town help\" to view commands")
            }
            return true
        }

        // parse subcommand
        when ( args[0].lowercase() ) {
            "help" -> printHelp(sender)
            "ayuda" -> printHelp(sender)
            "officer" -> setOfficer(player, args, null)
            "promote" -> setOfficer(player, args, true)
            "demote" -> setOfficer(player, args, false)
            "apply" -> appToTown(player, args)
            "join" -> appToTown(player, args)
            "invite" -> invite(player, args)
            "invitar" -> invite(player, args)
            "accept" -> accept(player, args)
            "deny" -> deny(player, args)
            "reject" -> deny(player, args)
            "leave" -> leaveTown(player)
            "kick" -> kickFromTown(player, args)
            "spawn" -> goToSpawn(player, args)
            "capital" -> goToSpawn(player, args)
            "setspawn" -> setSpawn(player)
            "list" -> listTowns(player)
            "info" -> getInfo(player, args)
            "online" -> getOnline(player, args)
            "income" -> getIncome(player)
            "prefix" -> prefix(player, args)
            "suffix" -> suffix(player, args)
            "map" -> printMap(player, args)
            "minimap" -> minimap(player, args)
            "perms",
            "permisos" -> setPermissions(player, args)
            "permissions" -> setPermissions(player, args)
            "protect" -> protectChests(player, args)
            "trust" -> trustPlayer(player, args, true)
            "untrust" -> trustPlayer(player, args, false)
            "anexar" -> annexTerritory(player, args)
            "annex" -> annexTerritory(player, args)
            "outpost" -> manageOutpost(sender, args)
            else -> { Message.error(sender, "Comando inválido, usa /town ayuda") }
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
            return filterByStart(SUBCOMMANDS, args[0])
        }
        // match each subcommand format
        else if ( args.size > 1 ) {
            // handle specific subcommands
            when ( args[0].lowercase() ) {

                // /town [subcommand] [resident]
                "officer",
                "promote",
                "demote",
                "leader" -> {
                    if ( args.size == 2 ) {
                        val town = Nodes.getResident(player)?.town
                        if ( town != null ) {
                            return filterTownResident(town, args[1])
                        }
                    }
                }
                
                // /town [subcommand] [player]
                "invite" -> {
                    if ( args.size == 2 ) {
                        return filterResident(args[1])
                    }
                }

                // /town [subcommand] [town]
                "apply",
                "join",
                "accept",
                "aceptar",
                "reject",
                "denegar",
                "deny" -> {
                    if ( args.size == 2 ) {
                        return filterTown(args[1])
                    }
                }

                // /town [subcommand] [resident]
                "kick",
                "trust",
                "untrust",
                "prefix",
                "suffix" -> {
                    if ( args.size == 2 ) {
                        val town = Nodes.getResident(player)?.town
                        if ( town != null ) {
                            return filterTownResident(town, args[1])
                        }
                    }
                }

                // /town info name
                "info" -> {
                    if ( args.size == 2 ) {
                        return filterTown(args[1])
                    }
                }

                // /town online name
                "online" -> {
                    if ( args.size == 2 ) {
                        return filterTown(args[1])
                    }
                }
                
                // /town permissions [type] [group] [flag]
                "permissions" -> {
                    if ( args.size == 2 ) {
                        return filterByStart(PERMISSIONS_TYPES, args[1])
                    }
                    else if ( args.size == 3 ) {
                        return filterByStart(PERMISSIONS_GROUPS, args[2])
                    }
                    else if ( args.size == 4 ) {
                        return filterByStart(PERMISSIONS_FLAGS, args[3])
                    }
                }

                // chest protection
                "protect" -> {
                    if ( args.size == 2 ) {
                        return listOf("show")
                    }
                }

                // spawn command
                "spawn" -> {
                    if ( args.size == 2 ) {
                        val town = Nodes.getResident(player)?.town
                        if ( town != null ) {
                            return filterByStart(town.outposts.keys.toList(), args[1])
                        }
                    }
                }

                // outpost subcommand
                "outpost" -> {
                    if ( args.size == 2 ) {
                        return filterByStart(OUTPOST_SUBCOMMANDS, args[1])
                    }
                }
            }
        }
        
        return listOf()
    }

    private fun printHelp(sender: CommandSender?) {
        Message.print(sender, "${ChatColor.BOLD}[Nodes] Comandos de towns:")
        Message.print(sender, "/town promote${ChatColor.WHITE}: Give officer rank to resident")
        Message.print(sender, "/town demote${ChatColor.WHITE}: Remove officer rank from resident")
        Message.print(sender, "/town apply${ChatColor.WHITE}: Pedirle a una town para unirse")
        Message.print(sender, "/town invitar${ChatColor.WHITE}: Invitar un jugador a tú town")
        Message.print(sender, "/town leave${ChatColor.WHITE}: Abandonar tú town")
        Message.print(sender, "/town kick${ChatColor.WHITE}: Expulsar a un jugador de tú town ")
        Message.print(sender, "/town capital${ChatColor.WHITE}: Ir a la capital de la town")
        Message.print(sender, "/town setspawn${ChatColor.WHITE}: Recolocar la localización de la capital, solo dentro del territorio en el que está colocada por default ")
        Message.print(sender, "/town list${ChatColor.WHITE}: Lista de todas las towns")
        Message.print(sender, "/town info${ChatColor.WHITE}: Ver detalles de la town")
        Message.print(sender, "/town online${ChatColor.WHITE}: Ver jugadores activos en el momento de una town")
        Message.print(sender, "/town prefix${ChatColor.WHITE}: Set player name prefix")
        Message.print(sender, "/town suffix${ChatColor.WHITE}: Set player name suffix")
        Message.print(sender, "/town map${ChatColor.WHITE}: Ver los claims en el chat)
        Message.print(sender, "/town minimap${ChatColor.WHITE}: Habilita/deshabilita el minimapa de chunks")
        Message.print(sender, "/town permissions${ChatColor.WHITE}: Configura los permisos de la town")
        Message.print(sender, "/town protect${ChatColor.WHITE}: Proteger los cofres en el territorio")
        Message.print(sender, "/town trust${ChatColor.WHITE}: Darle permisos de habitante a un usuario")
        Message.print(sender, "/town untrust${ChatColor.WHITE}: Remover permisos de habitante a un usuario")
        Message.print(sender, "/town outpost${ChatColor.WHITE}: Comandos de outpost")
        return
    }

    /**
     * @command /town create [name]
     * Create a new town with the specified name at location.
     */
    private fun createTown(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        if ( args.size == 1 ) {
            Message.print(player, "Uso: ${ChatColor.WHITE}/town create [nombre]")
            return
        }
        
        // do not allow during war
        if ( !Config.canCreateTownDuringWar && Nodes.war.enabled == true ) {
            Message.error(player, "No se pueden crear towns durante guerra")
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        // check if player has cooldown
        if ( resident.townCreateCooldown > 0 ) {
            val remainingTime = resident.townCreateCooldown
            val remainingTimeString = if ( remainingTime > 0 ) {
                val hour: Long = remainingTime/3600000L
                val min: Long = 1L + (remainingTime - hour * 3600000L)/60000L
                "${hour}hr ${min}min"
            }
            else {
                "0hr 0min"
            }

            Message.error(player, "No puedes crear otra town por: ${remainingTimeString} ")
            return
        }
        
        val name = args[1]
        if ( !stringInputIsValid(name) ) {
            Message.error(player, "Nombre de town inválida")
            return
        }

        val territory = Nodes.getTerritoryFromPlayer(player)
        if ( territory == null ) {
            Message.error(player, "Este chunk no tiene territorio asignado")
            return
        }

        val result = Nodes.createTown(sanitizeString(name), territory, resident)
        if ( result.isSuccess ) {
            Message.broadcast("${ChatColor.BOLD}${player.name} ha creado la town \"${name}\"")

            // check how much player is over town claim limit
            val overClaimsPenalty: Int = Math.max(0, Config.initialOverClaimsAmountScale * (territory.cost - Config.townInitialClaims))
            if ( overClaimsPenalty > 0 ) {
                Message.print(player, "${ChatColor.DARK_RED}Tú town está por encima de la cantidad inicial de claims ${Config.townInitialClaims}: estás recibiendo un -${overClaimsPenalty} de penalización de poner inicial")
            }
        }
        else {
            when ( result.exceptionOrNull() ) {
                ErrorTownExists -> Message.error(player, "La town \"${name}\" ya existe")
                ErrorPlayerHasTown -> Message.error(player, "Ya perteneces a una town")
                ErrorTerritoryOwned -> Message.error(player, "El territorio está claimeado por otra town")
            }
        }
    }

    /**
     * @command /town delete
     * Delete your town. Town leaders only.
     */
    private fun deleteTown(player: Player?) {
        if ( player == null ) {
            return
        }

        // check if player is town leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town, usa /town join \[nombre]\")
            return
        }

        val leader = town.leader
        if ( resident !== leader ) {
            Message.error(player, "No eres el líder de la town")
            return
        }

        val nation = town.nation
        if ( nation !== null && town === nation.capital ) {
            Message.error(player, "No puedes destruir la town siendo la capital de la nación, usa /n delete primeramente")
            return
        }

        // do not allow during war
        if ( !Config.canDestroyTownDuringWar && Nodes.war.enabled == true ) {
            Message.error(player, "No puedes borrar tú town durante guerra")
            return
        }

        Nodes.destroyTown(town)

        // add player penalty for destroying town
        Nodes.setResidentTownCreateCooldown(resident, Config.townCreateCooldown)

        Message.broadcast("${ChatColor.DARK_RED}${ChatColor.BOLD}La town \"${town.name}\" ha capitulado...")
    }

    /**
     * @command /town promote [name]
     * Makes player a town officer.
     * 
     * @command /town demote [name]
     * Removes player from town officers.
     */
    private fun setOfficer(player: Player?, args: Array<String>, toggle: Boolean?) {
        if ( player == null || args.size < 2 ) {
            Message.error(player, "Uso: /t promote/demote [jugador]")
            return
        }

        // check if player is town leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        val leader = town.leader
        if ( resident !== leader ) {
            Message.error(player, "No eres el líder de la town")
            return
        }

        // get other resident
        val target = Nodes.getResidentFromName(args[1])
        if ( target === null ) {
            Message.error(player, "Jugador no encontrado")
            return
        }
        if ( target === resident ) {
            Message.error(player, "Ya eres el líder de la town")
            return
        }

        val targetTown = target.town
        if ( targetTown !== town ) {
            Message.error(player, "El jugador no está en esta town")
            return
        }

        val targetPlayer = target.player()

        // if null, toggle officer
        if ( toggle === null ) {
            if ( town.officers.contains(target) ) {
                Nodes.townRemoveOfficer(town, target)
                Message.print(player, "${target.name} ha sido removido de los officers de la town")

                if ( targetPlayer !== null ) {
                    Message.error(targetPlayer, "Ya no eres officer en tú town")
                }
            }
            else {
                Nodes.townAddOfficer(town, target)
                Message.print(player, "${target.name} ahora es un officer de la town")

                if ( targetPlayer !== null ) {
                    Message.print(targetPlayer, "Ahora eres un officer de la town")
                }
            }
        }
        // add officer
        else if ( toggle === true ) {
            if ( !town.officers.contains(target) ) {
                Nodes.townAddOfficer(town, target)
                Message.print(player, "${target.name} ahora es un officer de la town")

                if ( targetPlayer !== null ) {
                    Message.print(targetPlayer, "Ahora eres un officer de la town")
                }
            }
        }
        // remove officer
        else if ( toggle === false ) {
            if ( town.officers.contains(target) ) {
                Nodes.townRemoveOfficer(town, target)
                Message.print(player, "${target.name} removido de los officers de la town")

                if ( targetPlayer !== null ) {
                    Message.error(targetPlayer, "Ya no eres un officer de tú town")
                }
            }
        }
        
    }

    /**
     * @command /town leader [name]
     * Give town leadership to another player in town.
     */
    private fun setLeader(player: Player?, args: Array<String>) {
        if ( player == null || args.size < 2 ) {
            Message.error(player, "Uso: /t leader [jugador]")
            return
        }

        // check if player is town leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        val leader = town.leader
        if ( resident !== leader ) {
            Message.error(player, "No eres el líder de la town")
            return
        }

        // get other resident
        val target = Nodes.getResidentFromName(args[1])
        if ( target === null ) {
            Message.error(player, "Este jugador no existe")
            return
        }
        if ( target === resident ) {
            Message.error(player, "Ya eres el líder de la town")
            return
        }

        val targetTown = target.town
        if ( targetTown !== town ) {
            Message.error(player, "El jugador no está en esta town")
            return
        }

        Nodes.townSetLeader(town, target)
        Message.print(player, "Has hecho a ${target.name} el nuevo líder de ${town.name}")
        
        val targetPlayer = target.player()
        if ( targetPlayer !== null ) {
            Message.print(targetPlayer, "Ahora eres el líder de ${town.name}")
        }
    }

    /**
     * @command /town apply [town]
     * Ask to join a town.
     */
    private fun appToTown(player: Player?, args: Array<String>) {
        if (player == null) {
            return
        }

        if ( args.size != 2) {
            Message.print(player, "Uso: /town apply [town]")
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        if ( resident.town != null ) {
            Message.error(player, "Ya eres miembro de una town")
            return
        }

        val town = Nodes.getTownFromName(args[1])
        if ( town == null ) {
            Message.error(player,"Esa town no existe")
            return
        }

        if ( town.isOpen == true ) {
            Nodes.addResidentToTown(town,resident)
            Message.print(player, "¡Ahora eres residente de ${town.name}!")
            return
        }

        if ( town.applications.containsKey(resident) ) {
            Message.error(player, "Ya has pedido entrar a ${town.name}")
            return
        }

        val approvers: ArrayList<Player> = ArrayList()
        Bukkit.getPlayer(town.leader!!.name)?.let { player ->
            approvers.add(player)
        }
        town.officers.forEach() { officer ->
            Bukkit.getPlayer(officer.name)?.let { player ->
                approvers.add(player)
            }
        }

        if ( approvers.isEmpty() ) {
            Message.error(player, "No hay officers activos en ${town.name} para recibir tú petición")
            return
        }

        approvers.forEach() { approver ->
            Message.print(approver, "${resident.name} ha pedido entrar a la town. \nEscribe \"/t aceptar\" para dejarlo entrar o \"/t denegar\" para rechazar su oferta.")
        }
        Message.print(player, "Tú petición ha sido enviada")

        town.applications.put(resident,
            Bukkit.getAsyncScheduler().runDelayed(Nodes.plugin!!, {
                player.scheduler.run(Nodes.plugin!!, { _ ->
                    if ( resident.town == null ) {
                        Message.print(player, "¡Nadie en ${town.name} ha respuesto tú petición!")
                        town.applications.remove(resident)
                    }
                }, null)
            }, 1200 * 50, TimeUnit.MILLISECONDS)
        )
    }

    /**
     * @command /town invite [player]
     * Invite a player to join your town. Town leader and officers only.
     */
    private fun invite(player: Player?, args: Array<String>) {
        if (player == null) {
            return
        }

        if ( args.size != 2) {
            Message.print(player, "Uso: /town invite [jugador]")
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }
        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        val invitee: Player? = Bukkit.getPlayer(args[1])
        if ( invitee == null ) {
            Message.error(player, "Ese jugador no está activo ahora mismo")
            return
        } else if ( invitee == player ) {
            Message.error(player, "Ya estás en tú town")
            return
        }

        val inviteeResident = Nodes.getResident(invitee)
        if ( inviteeResident == null ) {
            return
        }
        if ( inviteeResident.invitingTown == town) {
            Message.error(player, "Este jugador ya ha sido invitado por la town")
            return
        } else if ( inviteeResident.invitingTown != null) {
            Message.error(player, "Este jugador está considerando una invitación de otra town")
            return
        }
        val inviteeTown = inviteeResident.town
        if ( inviteeTown != null ) {
            Message.error(player, "Este jugador ya es un miembro de una town")
            return
        }

        if ( town.leader === resident || town.officers.contains(resident) ) {
            Message.print(player, "${invitee.name} ha sido invitado a tú town.")
            Message.print(invitee, "Has sido invitado para pertenecer a ${town.name}.\nEscribe \"/t aceptar\" para unirte or \"/t denegar\" para rechazar la oferta.")
            inviteeResident.invitingTown = town
            inviteeResident.invitingPlayer = player
            inviteeResident.inviteThread = Bukkit.getAsyncScheduler().runDelayed(Nodes.plugin!!, { _ ->
                Bukkit.getGlobalRegionScheduler().run(Nodes.plugin!!) {
                    if (inviteeResident.invitingPlayer == player) {
                        Message.print(player, "¡${invitee.name} no ha respuesto a la invitación!")
                        inviteeResident.invitingTown = null
                        inviteeResident.invitingPlayer = null
                        inviteeResident.inviteThread = null
                    }
                }
            }, 1200 * 50, TimeUnit.MILLISECONDS)
        } else {
            Message.error(player, "No tienes permitido invitar a nuevos jugadores")
        }
    }

    private fun accept(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            if ( resident.invitingTown == null ) {
                Message.error(player,"No has sido invitado a ninguna town, o puede que la invitación haya expirado")
                return
            }

            Message.print(player,"¡Ahora eres miembro de ${resident.invitingTown?.name}! Escribe \"/t capital\" para teletransportarte a tú town")
            Message.print(resident.invitingPlayer, "¡${resident.name} ha aceptado tú invitación!")

            Nodes.addResidentToTown(resident.invitingTown!!, resident)
            resident.invitingTown = null
            resident.invitingPlayer = null
            resident.inviteThread = null
        } else {
            if ( town.leader != resident && !town.officers.contains(resident) ) {
                Message.error(player, "No tienes permitido aceptar o denegar peticiones para unirse a la town")
                return
            }

            if ( town.applications.isEmpty() ) {
                Message.error(player, "No hay solicitudes activas")
                return
            }

            var applicant: Resident = resident
            if ( town.applications.size == 1 ) {
                town.applications.forEach { k, v ->
                    applicant = k
                }
                if ( args.size > 1 && args[1].lowercase() != applicant.name.lowercase()) {
                    Message.error(player, "Ese jugador no ha solicitado, o puede que ya haya expirado")
                    return
                }
            } else {
                if ( args.size == 1) {
                    val applicantsString = town.applications.map {application -> application.key.name}.joinToString(", ")
                    Message.print(player, "Hay varias solicitudes para unirse. Por favor, usa \"/town aceptar [jugador]\".\nSolicitantes actuales: ${applicantsString}")
                    return
                }

                applicant = Nodes.getResidentFromName(args[1])!!
                if ( !town.applications.containsKey(applicant!!)) {
                    Message.error(player, "Ese jugador no ha solicitado o su solicitud ha caducado")
                    return
                }
            }

            Message.print(player, "¡${applicant.name} ha sido aceptado en tú town!")
            val applicantPlayer = Bukkit.getPlayer(applicant.name)
            if ( applicantPlayer != null ) {
                Message.print(applicantPlayer, "¡Has sido aceptado en ${town.name}!")
            }

            Nodes.addResidentToTown(town, applicant)
            town.applications.remove(applicant)
        }
    }

    private fun deny(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            if ( resident.invitingTown == null ) {
                Message.error(player,"No has sido invitado por ninguna town o la invitación ha caducado")
                return
            }

            Message.print(player,"Has rechazado la solicitud para unirte a ${resident.invitingTown?.name}")
            Message.print(resident.invitingPlayer, "¡${resident.name} ha rechazado tú invitación!")
            resident.invitingTown = null
            resident.invitingPlayer = null
            resident.inviteThread = null
        } else {
            if ( town.leader != resident && !town.officers.contains(resident) ) {
                Message.error(player, "No tienes permitido manejar invitaciones de la town")
                return
            }

            if ( town.applications.isEmpty() ) {
                Message.error(player, "No hay solicitudes activas")
                return
            }

            var applicant: Resident = resident
            if ( town.applications.size == 1 ) {
                town.applications.forEach { k, v ->
                    applicant = k
                }
                if ( args.size > 1 && args[1] != applicant.name) {
                    Message.error(player, "Este jugador no ha solicitado o su solicitud ha caducado")
                    return
                }
            } else {
                if ( args.size == 1) {
                    val applicantsString = town.applications.map {application -> application.key.name}.joinToString(", ")
                    Message.print(player, "Hay varias solicitudes de la town. Por favor, usa \"/town aceptar [jugador]\".\nSolicitantes actuales: ${applicantsString}")
                    return
                }

                applicant = Nodes.getResidentFromName(args[1])!!
                if ( !town.applications.containsKey(applicant!!)) {
                    Message.error(player, "Este jugador no ha solicitado o su solicitud ha expirado")
                    return
                }
            }

            Message.print(player, "¡${applicant.name} se ha negado la residencia en tú town!")
            val applicantPlayer = Bukkit.getPlayer(applicant.name)
            if ( applicantPlayer != null ) {
                Message.print(applicantPlayer, "¡La solicitud para entrar a ${town.name} ha sido rechazada!")
            }

            town.applications.remove(applicant)
        }
    }

    /**
     * @command /town leave
     * Abandon membership in your town.
     */
    private fun leaveTown(player: Player?) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        if ( town.leader == resident ) {
            Message.error(player, "Necesitas transferir el liderazgo antes de abandonar la town")
            return
        }

        // do not allow during war?
        if ( !Config.canLeaveTownDuringWar && Nodes.war.enabled == true ) {
            Message.error(player, "No puedes abandonar mientras hay guerra")
            return
        }
        
        Message.print(player,"Has abandonado ${town.name}")
        Nodes.removeResidentFromTown(town,resident)
    }

    /**
     * @command /town kick [player]
     * Remove another player from your town. Town leader and officers only.
     */
    private fun kickFromTown(player: Player?, args: Array<String>) {
        if ( player === null || args.size < 2 ) {
            Message.error(player, "Uso: /t kick [jugador]")
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

        // check if player is leader or officer
        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "Solo el líder o officers pueden expulsar jugadores")
            return
        }

        // get other resident
        val target = Nodes.getResidentFromName(args[1])
        if ( target === null ) {
            Message.error(player, "Jugador no encontrado")
            return
        }

        val targetTown = target.town
        if ( targetTown !== town ) {
            Message.error(player, "El jugador no está en esta town")
            return
        }

        // cannot kick leaders or officers
        if ( target === leader || town.officers.contains(target) ) {
            Message.error(player, "No puedes sacar al líder o otros officers")
            return
        }

        Message.print(player, "Has expulsado a ${target.name} de la town, ¡Fuera de aquí!")

        val targetPlayer = target.player()
        if ( targetPlayer !== null ) {
            Message.print(targetPlayer, "${ChatColor.DARK_RED}Has sido expulsado de ${town.name} :(")
        }
        
        Nodes.removeResidentFromTown(town, target)
    }

    /**
     * @command /town spawn
     * Teleport to your town's main spawnpoint.
     * 
     * @subcommand /town spawn [outpost]
     * Teleport to an outpost's spawn point.
     */
    private fun goToSpawn(player: Player?, args: Array<String>) {
        if ( player === null ) {
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

        // check if already trying to teleport
        if ( resident.teleportThread !== null ) {
            Message.error(player, "Ya estás tratando de teletransportarte")
            return
        }

        // parse spawn destination
        val destination = if ( args.size > 1 ) {
            val outpostName = args[1]
            val outpost = town.outposts.get(outpostName)
            if ( outpost !== null ) {
                // pay item cost to teleport
                if ( Config.outpostTeleportCost.size > 0 ) {
                    val inventory = player.getInventory()
                    for ( (material, amount) in Config.outpostTeleportCost ) {
                        val items = ItemStack(material)
                        if ( !inventory.containsAtLeast(items, amount) ) {
                            Message.error(player, "No tienes el pago requerido para teletransportarte al outpost: ${Config.outpostTeleportCostString}")
                            return
                        }
                    }

                    // subtract cost
                    for ( (material, amount) in Config.outpostTeleportCost ) {
                        val items = ItemStack(material, amount)
                        inventory.removeItem(items)
                    }

                    Message.print(player, "Teletransportarse a un outpost, esto costará: ${Config.outpostTeleportCostString}")
                }

                resident.isTeleportingToOutpost = true
                outpost.spawn
            }
            else {
                if ( town.outposts.size > 0 ) {
                    Message.error(player, "La town no tiene un outpost llamado: ${outpostName}")
                    Message.error(player, "Outpost disponibles: ${town.outposts.keys}")
                }
                else {
                    Message.error(player, "Tú town no tiene outposts")
                }

                return
            }
        }
        // go to main town spawn
        else {
            resident.isTeleportingToOutpost = false
            town.spawnpoint
        }

        // ticks before teleport timer runs
        var teleportTimerTicks = Math.max(0.0, Config.townSpawnTime * 20.0)

        // multiplier during war and if home occupied
        if ( Nodes.war.enabled && Nodes.getTerritoryFromId(town.home)?.occupier !== null ) {
            Message.error(player, "${ChatColor.BOLD}Tú home está ocupado, ir al spawn de la town puede tomar algo de tiempo...")
            teleportTimerTicks *= Config.occupiedHomeTeleportMultiplier
        }

        resident.teleportThread = Bukkit.getAsyncScheduler().runDelayed(Nodes.plugin!!, {
            Bukkit.getGlobalRegionScheduler().run(Nodes.plugin!!) {
                player.teleportAsync(destination)
                resident.teleportThread = null
                resident.isTeleportingToOutpost = false
            }
        }, teleportTimerTicks.toLong() * 50L, TimeUnit.MILLISECONDS)

        if ( teleportTimerTicks > 0 ) {
            val seconds = teleportTimerTicks.toInt() / 20

            if ( resident.isTeleportingToOutpost ) {
                Message.print(player, "Teletransportando al outpost en ${seconds} segundos. No te muevas...")
            }
            else {
                Message.print(player, "Teletransportandose a la capital en ${seconds} segundos. No te muevas...")
            }
        }
    }

    /**
     * @command /town setspawn
     * Change your town's spawnpoint to another location in the home territory. Town leader only.
     */
    private fun setSpawn(player: Player?) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "No eres el líder o un officer de la town")
            return
        }

        val result = Nodes.setTownSpawn(town, player.location)
        
        if ( result == true ) {
            Message.print(player, "La capital fue colocada en tus coordenadas actuales")
        }
        else {
            Message.error(player,"El spawn de la town debe estar dentro del territorio capital de la town")
        }
    }
    /**
     * @command /town list
     * View list of all established towns
     */
    private fun listTowns(player: Player?) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null) {
            return
        }

        Message.print(player,"${ChatColor.BOLD}Town - Habitantes")
        val townsList = ArrayList(Nodes.towns.values)
        townsList.sortByDescending { it.residents.size }
        townsList.forEach { town ->
            Message.print(player, "${town.name}${ChatColor.WHITE} - ${town.residents.size}")
        }
    }


    /**
     * @command /town info
     * View your town's name, leader, officers, residents, and claims.
     * @subcommand /town info [town]
     * View details of another town
     */
    private fun getInfo(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null) {
            return
        }

        var town: Town? = null
        if ( args.size == 1 ) {
            if ( resident.town == null ) {
                Message.error(player, "No perteneces a una town")
                return
            }
            town = resident.town
        } else if ( args.size == 2 ) {
            if (!Nodes.towns.containsKey(args[1])) {
                Message.error(player, "Esa town no existe")
                return
            }
            town = Nodes.getTownFromName(args[1])
        } else {
            Message.error(player, "Uso: /town info [town]")
            return
        }

        town?.printInfo(player)
    }

    /**
     * @command /town online
     * View your town's online players
     * @subcommand /town online [town]
     * View another town's online players
     */
    private fun getOnline(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null) {
            return
        }

        var town: Town? = null
        if ( args.size == 1 ) {
            if ( resident.town == null ) {
                Message.error(player, "No perteneces a una town")
                return
            }
            town = resident.town
        } else if ( args.size == 2 ) {
            if ( !Nodes.towns.containsKey(args[1]) ) {
                Message.error(player, "Esa town no existe")
                return
            }
            town = Nodes.getTownFromName(args[1])
        } else {
            Message.error(player, "Uso: /town online [town]")
            return
        }

        if ( town == null ) {
            return
        }

        val numPlayersOnline = town.playersOnline.size
        val playersOnline = town.playersOnline.map({p -> p.name}).joinToString(", ")
        Message.print(player, "Jugadores activos en ${town.name} [${numPlayersOnline}]: ${ChatColor.WHITE}${playersOnline}")
    }

    /**
     * @command /town color [r] [g] [b]
     * Set town territory color for dynmap. Town leader only.
     */
    private fun setColor(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        if ( args.size < 4 ) {
            Message.print(player, "Uso: ${ChatColor.WHITE}/town color [r] [g] [b]")
            return
        }

        // check if player is town leader
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            return
        }

        val leader = town.leader
        if ( resident !== leader ) {
            Message.error(player,"Solamente el líder de la town puede hacer esto")
            return
        }

        // parse color
        try {
            val r = args[1].toInt().coerceIn(0, 255)
            val g = args[2].toInt().coerceIn(0, 255)
            val b = args[3].toInt().coerceIn(0, 255)
            
            Nodes.setTownColor(town, r, g, b)
            Message.print(player, "Color de la town: ${ChatColor.WHITE}${r} ${g} ${b}")
        }
        catch (e: NumberFormatException) {
            Message.error(player, "Color inválido (debe de ser [r] [g] [b] en un rango de 0-255)")
        }
        
    }

    /**
     * @command /town claim
     * Claim a contiguous territory for your town. Town leader and officers only.
     */
    private fun claimTerritory(player: Player?) {
        if ( player == null ) {
            return
        }

        // get town from player
        val resident = Nodes.getResident(player)
        val town = resident?.town
        if ( town == null ) {
            Message.error(player, "No puedes claimear sin ser de una town")
            return
        }
        
        if ( resident !== town.leader && !town.officers.contains(resident) ) {
            Message.error(player, "No eres el líder o un officer de la town")
            return
        }

        // get territory from chunk and run claim process
        val loc = player.getLocation()
        val territory = Nodes.getTerritoryFromBlock(loc.x.toInt(), loc.z.toInt())
        if ( territory == null ) {
            Message.error(player, "Aquí no hay un territorio para poder claimear")
            return
        }
        
        val result = Nodes.claimTerritory(town, territory)
        if ( result.isSuccess ) {
            Message.print(player, "Territorio (id=${territory.id}) claimeado")
        }
        else {
            when ( result.exceptionOrNull() ) {
                ErrorTooManyClaims -> Message.error(player, "No hay poder suficiente")
                ErrorTerritoryNotConnected -> Message.error(player, "El territorio debe estar adyacente a tú town")
                ErrorTerritoryHasClaim -> Message.error(player, "El territorio ya fue claimeado por una town")
            }
        }
    }

    /**
     * @command /town unclaim
     * Abandon your town's claim over a territory
     */
    private fun unclaimTerritory(player: Player?) {
        if ( player == null ) {
            return
        }

        // get town from player
        val resident = Nodes.getResident(player)
        val town = resident?.town
        if ( town == null ) {
            Message.error(player, "No perteneces a una town")
            return
        }
        
        if ( resident !== town.leader && !town.officers.contains(resident) ) {
            Message.error(player, "No eres el líder o un officer de la town")
            return
        }
        
        // get territory from chunk and run claim process
        val loc = player.getLocation()
        val territory = Nodes.getTerritoryFromBlock(loc.x.toInt(), loc.z.toInt())
        if ( territory == null ) {
            Message.error(player, "Este chunk no tiene un territorio")
            return
        }
        
        val result = Nodes.unclaimTerritory(town, territory)
        if ( result.isSuccess ) {
            Message.print(player, "Territorio (id=${territory.id}) unclaimeado, el poder del claim usado se regenerará durante el tiempo")
        }
        else {
            when ( result.exceptionOrNull() ) {
                ErrorTerritoryNotInTown -> Message.error(player, "El territorio no debe ser de una town")
                ErrorTerritoryIsTownHome -> Message.error(player, "No se puede unclaimear territorio del home")
            }
        }
    }

    /**
     * @command /town income
     * Collect income from territory bonuses. Town leader and officers only.
     */
    // TODO: check if player is leader or officer to collect income
    private fun getIncome(player: Player?) {
        if ( player == null ) {
            return
        }
        
        // get town from player
        val resident = Nodes.getResident(player)
        val town = resident?.town
        if ( town == null ) {
            Message.error(player, "No perteneces a una town")
            return
        }

        // check player permissions
        val hasPermissions = if ( resident === town.leader || town.officers.contains(resident) ) {
            true
        }
        else if ( town.permissions[TownPermissions.INCOME].contains(PermissionsGroup.TOWN) && resident.town === town ) {
            true
        }
        else if ( town.permissions[TownPermissions.INCOME].contains(PermissionsGroup.TRUSTED) && resident.town === town && resident.trusted ) {
            true
        }
        else {
            false
        }

        // open town inventory
        if ( hasPermissions ) {
            player.openInventory(Nodes.getTownIncomeInventory(town))
        }
        else {
            Message.error(player, "You do not have permissions to view town income")
        }
    }

    /**
     * @command /town prefix [prefix]
     * Set personal chat prefix
     * 
     * @subcommand /town prefix [player] [prefix]
     * Set a player's prefix (leader and officers only)
     */
    private fun prefix(player: Player?, args: Array<String>) {
        if ( player === null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident === null ) {
            return
        }
        
        // reset prefix
        if ( args.size == 1 ) {            
            // print usage
            Message.error(player, "Uso: \"/town prefix [nombre]\" para determinar tú prefijo")
            Message.error(player, "Uso: \"/town prefix remove\" para quitar tú prefijo")
            Message.error(player, "Uso: \"/town prefix [jugador] [nombre]\" para determinar el prefijo de un jugador")
            Message.error(player, "Uso: \"/town prefix [jugador] remove\" para sacarle el prefijo a un jugador")
        }
        // setting personal prefix
        else if ( args.size == 2 ) {
            val prefix = args[1]
            if ( prefix.lowercase() == "remove" ) {
                Nodes.setResidentPrefix(resident, "")
                Message.print(player, "Prefijo removido.")
            }
            else {
                Nodes.setResidentPrefix(resident, args[1])
                Message.print(player, "Tú prefijo ahora es: ${args[1]}")
            }
        }
        // setting a player's prefix in town
        else if ( args.size > 2 ) {
            val town = resident.town
            if ( town == null ) {
                Message.error(player, "No eres miembro de una town")
                return
            }

            // check if player is leader or officer
            val leader = town.leader
            if ( resident !== leader && !town.officers.contains(resident) ) {
                Message.error(player, "Solo el líder y los officers pueden determinar los prefijos/sufijos")
                return
            }

            // get other resident
            val target = Nodes.getResidentFromName(args[1])
            if ( target === null ) {
                Message.error(player, "Jugador no encontrado")
                return
            }

            val targetTown = target.town
            if ( targetTown !== town ) {
                Message.error(player, "El jugador no está en esta town")
                return
            }

            val prefix = args[2]
            if ( prefix.lowercase() == "remove" ) {
                Nodes.setResidentPrefix(target, "")
                Message.print(player, "Removido el prefijo ${target.name} .")
            }
            else {
                Nodes.setResidentPrefix(target, args[2])
                Message.print(player, "${target.name} prefijo establecido a: ${args[2]}")
            }
        }
    }

    /**
     * @command /town suffix [suffix]
     * Set personal chat suffix
     * 
     * @subcommand /town suffix [player] [suffix]
     * Set a player's suffix (leader and officers only)
     */
    private fun suffix(player: Player?, args: Array<String>) {
        if ( player === null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident === null ) {
            return
        }
        
        // reset prefix
        if ( args.size == 1 ) {
            // print usage
            Message.error(player, "Uso: \"/town suffix [nombre]\" para determinar tú sufijo")
            Message.error(player, "Uso: \"/town suffix remove\" para quitar tú sufijo")
            Message.error(player, "Uso: \"/town suffix [jugador] [nombre]\" para determinar el sufijo de un jugador")
            Message.error(player, "Uso: \"/town suffix [jugador] remove\" para quitar el sufijo de un jugador")
        }
        // setting personal prefix
        else if ( args.size == 2 ) {
            val prefix = args[1]
            if ( prefix.lowercase() == "remove" ) {
                Nodes.setResidentSuffix(resident, "")
                Message.print(player, "Sufijo removido.")
            }
            else {
                Nodes.setResidentSuffix(resident, args[1])
                Message.print(player, "Tú sufijo ahora es: ${args[1]}")
            }
        }
        // setting a player's prefix in town
        else if ( args.size > 2 ) {
            val town = resident.town
            if ( town == null ) {
                Message.error(player, "No eres miembro de una town")
                return
            }

            // check if player is leader or officer
            val leader = town.leader
            if ( resident !== leader && !town.officers.contains(resident) ) {
                Message.error(player, "Solo el líder o los officers pueden establecer el prefijo/sufijo")
                return
            }

            // get other resident
            val target = Nodes.getResidentFromName(args[1])
            if ( target === null ) {
                Message.error(player, "Jugador no encontrado")
                return
            }

            val targetTown = target.town
            if ( targetTown !== town ) {
                Message.error(player, "El jugador no está en esta town")
                return
            }

            val prefix = args[2]
            if ( prefix.lowercase() == "remove" ) {
                Nodes.setResidentSuffix(target, "")
                Message.print(player, "Sufijo ${target.name} removido.")
            }
            else {
                Nodes.setResidentSuffix(target, args[2])
                Message.print(player, "${target.name} sufijo establecido a: ${args[2]}")
            }
        }
    }

    /**
     * @command /town rename [new name]
     * Rename your town. Town leader only.
     */
    private fun renameTown(player: Player?, args: Array<String>) {
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

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No perteneces a una town")
            return
        }

        if ( resident != town.leader ) {
            Message.error(player, "Solamente el líder puede hacer esto")
            return
        }

        val name = args[1]
        if ( !stringInputIsValid(name) ) {
            Message.error(player, "Nombre de town inválido")
            return
        }

        if ( town.name.lowercase() == args[1].lowercase() ) {
            Message.error(player, "Tú town ya se llama ${town.name}")
            return
        }

        if ( Nodes.towns.containsKey(args[1]) ) {
            Message.error(player, "Ya hay una town llamada así")
            return
        }

        Nodes.renameTown(town,name)
        Message.print(player, "Town renombrada a ${town.name}!")
    }

    /**
     * @command /town map
     * Prints territory map into chat for player
     */
    private fun printMap(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }
        
        val loc = player.getLocation()
        val coordX = kotlin.math.floor(loc.x).toInt()
        val coordZ = kotlin.math.floor(loc.z).toInt()
        val coord = Coord.fromBlockCoords(coordX, coordZ)
        
        // minimap size
        val sizeY = 8
        val sizeX = 10

        Message.print(player, "\n${ChatColor.WHITE}--------------- Mapa Territorial ---------------")
        for ( (i, y) in (sizeY downTo -sizeY).withIndex() ) {
            val renderedLine = WorldMap.renderLine(resident, coord, coord.z - y, coord.x - sizeX, coord.x + sizeX)
            Message.print(player, MAP_STR_BEGIN + renderedLine + MAP_STR_END[i])
        }
        Message.print(player, "")
    }

    /**
     * @command /town minimap [3|4|5]
     * Turns on/off territory chunks minimap on sidebar.
     * Optionally specify size value: 3, 4, or 5.
     */
    private fun minimap(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        // if size input, create new minimap of that size
        // note: minimap creation internally handles removing old minimaps
        if ( args.size >= 2 ) {
            val size = try {
                Math.min(5, Math.max(3, args[1].toInt()))
            } catch (e: NumberFormatException) {
                Message.error(player, "Tamaño del minimapa inválido: ${args[1]}, debe de ser en un rango de 3-5. Usando predeterminado 5")
                5
            }
            resident.createMinimap(player, size)
            Message.print(player, "Minimapa habilitado (tamaño = ${size})")
        }
        else { // toggle minimap
            if ( resident.minimap != null ) {
                resident.destroyMinimap()
                Message.print(player, "Minimapa desactivado")
            }
            else {
                val size = 5
                resident.createMinimap(player, size)
                Message.print(player, "Minimapa habilitado (tamaño = ${size})")
            }
        }
    }

    /**
     * @command /town permissions [type] [group] [allow/deny]
     * Set permissions for interacting in town territory.
     * [type] can be: interact, build, destroy, chests, items
     * [group] can be: nation, ally, outsider.
     * Last entry is either "allow" or "deny"
     */
    private fun setPermissions(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No perteneces a una town")
            return
        }

        if ( args.size < 4 ) {
            // print current town permissions
            Message.print(player, "Permisos de la town:")
            for ( perm in enumValues<TownPermissions>() ) {
                val groups = town.permissions[perm]
                Message.print(player, "- ${perm}${ChatColor.WHITE}: ${groups}")
            }

            // print usage for leader, officers
            if ( resident === town.leader || town.officers.contains(resident) ) {
                Message.error(player, "Uso: /town permissions [tipo] [grupo] [aceptar/denegar]")
                Message.error(player, "[Tipo]: build, destroy, interact, chests, items, income")
                Message.error(player, "[Grupo]: town, nation, ally, outsider, trusted")
                Message.error(player, "[Aceptar/Denegar]: usa \"aceptar\" or \"denegar\"")
            }
            
            return
        }

        if ( resident !== town.leader && !town.officers.contains(resident) ) {
            Message.error(player, "Solo el líder o los officer pueden hacer esto")
            return
        }

        // match permissions and group
        val permissions: TownPermissions = when ( args[1].lowercase() ) {
            "build" -> TownPermissions.BUILD
            "destroy" -> TownPermissions.DESTROY
            "interact" -> TownPermissions.INTERACT
            "chests" -> TownPermissions.CHESTS
            "items" -> TownPermissions.USE_ITEMS
            "income" -> TownPermissions.INCOME
            else -> { 
                Message.error(player, "Tipo de permisos inválidos ${args[1]}. Opciones válidas: build, destroy, interact, items, income")
                return
            }
        }

        val group: PermissionsGroup = when ( args[2].lowercase() ) {
            "town" -> PermissionsGroup.TOWN
            "nation" -> PermissionsGroup.NATION
            "ally" -> PermissionsGroup.ALLY
            "outsider" -> PermissionsGroup.OUTSIDER
            "trusted" -> PermissionsGroup.TRUSTED
            else -> { 
                Message.error(player, "Invalid permissions group ${args[2]}. Valid options: town, nation, ally, outsider, trusted")
                return
            }
        }

        // get flag state (allow/deny)
        val flag = when ( args[3].lowercase() ) {
            "allow",
            "true" -> { true }
            
            "deny",
            "false" -> { false }
            
            else -> { 
                Message.error(player, "Invalid permissions flag ${args[3]}. Valid options: allow, deny")
                return
            }
        }

        Nodes.setTownPermissions(town, permissions, group, flag)

        Message.print(player, "Set permissions for ${town.name}: ${permissions} ${group} ${flag}")
    }

    /**
     * @command /town protect
     * Toggle protecting/unprotecting chests with mouse click.
     * 
     * @subcommand /town protect show
     * Shows protected chests with particles
     */
    private fun protectChests(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }
        
        if ( args.size > 1 ) {
            if ( args[1].lowercase() == "show" ) {
                Message.print(player, "Protected chests:")
                // print protected chests
                for ( block in town.protectedBlocks ) {
                    Message.print(player, "${ChatColor.WHITE}${block.type}: x: ${block.x}, y: ${block.y}, z: ${block.z}")
                }

                Nodes.showProtectedChests(town, resident)
            }
            else {
                Message.error(player, "Usage: \"/t protect\" to toggle protecting chests")
                Message.error(player, "Usage: \"/t protect show\" to show protected chests")
            }
            return
        }

        // check if player is town leader or officer
        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "Only leaders and officers can protect chests")
            return
        }

        if ( resident.isProtectingChests ) {
            Nodes.stopProtectingChests(resident)
            Message.print(player, "${ChatColor.DARK_AQUA}Stopped protecting chests.")
        }
        else {
            Nodes.startProtectingChests(resident)
            player.playSound(player.location, NODES_SOUND_CHEST_PROTECT, 1.0f, 1.0f)
            Message.print(player, "Click on a chest to protect or unprotect it. Use \"/t protect\" again to stop protecting, or click a non-chest block to stop.")
        }
    }

    /**
     * @command /town trust [name]
     * Mark player in town as trusted. Leader and officers only.
     * 
     * @command /town untrust [name]
     * Mark player in town as untrusted. Leader and officers only.
     */
    private fun trustPlayer(player: Player?, args: Array<String>, trust: Boolean) {
        if ( player == null || args.size < 2 ) {
            Message.error(player, "Usage: /t trust/untrust [player]")
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        // check if player is leader or officer
        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "Only leaders and officers can trust/untrust players")
            return
        }

        // get other resident
        val target = Nodes.getResidentFromName(args[1])
        if ( target == null ) {
            Message.error(player, "Jugador no encontrado")
            return
        }

        val targetTown = target.town
        if ( targetTown !== town ) {
            Message.error(player, "El jugador no está en esta town")
            return
        }
        
        // set player trust
        if ( trust ) {
            Nodes.setResidentTrust(target, true)
            Message.print(player, "${target.name} is now marked as trusted")
        }
        else {
            Nodes.setResidentTrust(target, false)
            Message.print(player, "${ChatColor.DARK_AQUA}${target.name} is marked as untrusted")
        }

    }

    /**
     * @command /town capital
     * Move town home territory to your current player location.
     * (This also changes town spawn location.)
     */
    private fun setCapital(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        // check if player is leader or officer
        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "Only leaders and officers can move the town's home capital territory")
            return
        }

        // check if territory belongs to town and isnt home already
        val territory = Nodes.getTerritoryFromPlayer(player)
        if ( territory == null ) {
            Message.error(player, "This region has no territory")
            return
        }
        if ( town !== territory.town ) {
            Message.error(player, "This is not your territory")
            return
        }
        if ( town.home == territory.id ) {
            Message.error(player, "This is already your home territory")
            return
        }
        if ( town.moveHomeCooldown > 0 ) {
            val remainingTime = town.moveHomeCooldown
            val remainingTimeString = if ( remainingTime > 0 ) {
                val hour: Long = remainingTime/3600000L
                val min: Long = 1L + (remainingTime - hour * 3600000L)/60000L
                "${hour}hr ${min}min"
            }
            else {
                "0hr 0min"
            }

            Message.error(player, "You cannot move the town's home territory for: ${remainingTimeString} ")
            return
        }

        // move home territory
        Nodes.setTownHomeTerritory(town, territory)
        Message.print(player, "You have moved the town's home territory to id = ${territory.id} (do not forget to update /t setspawn)")
    }

    /**
     * @command /town annex
     * Annex an occupied territory and add it to your town
     */
    private fun annexTerritory(player: Player?, args: Array<String>) {
        if ( player == null ) {
            return
        }

        if ( Config.annexDisabled ) {
            Message.error(player, "Annexing disabled")
            return
        }

        if ( !Nodes.war.enabled || !Nodes.war.canAnnexTerritories ) {
            Message.error(player, "You can only annex territories during war")
            return
        }

        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        // check if player is leader or officer
        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "Only leaders and officers can annex territories")
            return
        }

        // check if territory belongs to town and isnt home already
        val territory = Nodes.getTerritoryFromPlayer(player)
        if ( territory == null ) {
            Message.error(player, "This region has no territory")
            return
        }

        val territoryTown = territory.town
        if ( territoryTown === null ) {
            Message.error(player, "There is no town here")
            return
        }
        
        // check blacklist
        if ( Config.warUseBlacklist && Config.warBlacklist.contains(territoryTown.uuid) ) {
            Message.error(player, "Cannot annex this town (blacklisted)")
            return
        }
        if ( Config.useAnnexBlacklist && Config.annexBlacklist.contains(territoryTown.uuid) ) {
            Message.error(player, "Cannot annex this town (blacklisted)")
            return
        }

        // check whitelist
        if ( Config.warUseWhitelist ) {
            if ( !Config.warWhitelist.contains(territoryTown.uuid) ) {
                Message.error(player, "Cannot annex this town (not whitelisted)")
                return
            }
            else if ( Config.onlyWhitelistCanAnnex && !Config.warWhitelist.contains(town.uuid) ) {
                Message.error(player, "Cannot annex territories because your town is not white listed")
                return
            }
        }

        if ( town === territoryTown ) {
            Message.error(player, "This already your territory")
            return
        }
        if ( territory.occupier !== town ) {
            Message.error(player, "You have not occupied this territory")
            return
        }
        if ( territoryTown.home == territory.id && territoryTown.territories.size > 1 ) {
            Message.error(player, "You must annex all of this town's other territories before you can annex its home territory")
            return
        }

        val result = Nodes.annexTerritory(town, territory)
        if ( result == true ) {
            Message.print(player, "Annexed territory (id = ${territory.id})")
        }
        else {
            Message.error(player, "Failed to annex territory")
        }
    }

    // =====================================
    // town outpost management subcommands
    // 
    // general usage: /town outpost [subcommand] [args]
    // =====================================
    
    /**
     * @command /town outpost
     * Commands to manage town outposts.
     */
    private fun manageOutpost(sender: CommandSender, args: Array<String>) {
        val player = if ( sender is Player ) sender else null
        if ( player == null ) {
            printOutpostHelp(sender)
            return
        }

        if ( args.size < 2 ) {
            printOutpostHelp(sender)
        }
        else {
            // route subcommand function
            when ( args[1].lowercase() ) {
                "list" -> outpostList(player, args)
                "setspawn" -> outpostSetSpawn(player, args)
                else -> { printOutpostHelp(sender) }
            }
        }
    }

    private fun printOutpostHelp(sender: CommandSender) {
        Message.print(sender, "${ChatColor.BOLD}[Nodes] Town outpost management:")
        Message.print(sender, "/town outpost list${ChatColor.WHITE}: List town's outposts")
        Message.print(sender, "/town outpost setspawn${ChatColor.WHITE}: Set town outpost spawn to your current location")
        Message.print(sender, "Run a command with no args to see usage.")
    }
    
    /**
     * @subcommand /town outpost list
     * Print list of town's outposts.
     */
    private fun outpostList(player: Player, args: Array<String>) {
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        if ( town.outposts.size > 0 ) {
            Message.print(player, "Town outposts:")
            for ( (name, outpost) in town.outposts ) {
                val spawn = outpost.spawn
                Message.print(player, "- ${name}${ChatColor.WHITE}: Territory (id=${outpost.territory}, Spawn = (${spawn.x}, ${spawn.y}, ${spawn.z})")
            }
        }
        else {
            Message.error(player, "Town has no outposts")
        }
    }

    /**
     * @subcommand /town outpost setspawn
     * Set an outpost's spawn point. Player must be in the outpost territory.
     */
    private fun outpostSetSpawn(player: Player, args: Array<String>) {
        val resident = Nodes.getResident(player)
        if ( resident == null ) {
            return
        }

        val town = resident.town
        if ( town == null ) {
            Message.error(player, "No eres miembro de una town")
            return
        }

        // check if player is leader or officer
        val leader = town.leader
        if ( resident !== leader && !town.officers.contains(resident) ) {
            Message.error(player, "Only leaders and officers can move an outpost's spawn location")
            return
        }

        // check if territory belongs to town and isnt home already
        val territory = Nodes.getTerritoryFromPlayer(player)
        if ( territory == null ) {
            Message.error(player, "This region has no territory")
            return
        }
        if ( town !== territory.town ) {
            Message.error(player, "This is not your territory")
            return
        }
        
        // match outpost to territory
        for ( outpost in town.outposts.values ) {
            if ( outpost.territory == territory.id ) {
                val result = Nodes.setOutpostSpawn(town, outpost, player.location)
                if ( result == true ) {
                    Message.print(player, "Set outpost \"${outpost.name}\" spawn to current location")
                }
                else {
                    Message.error(player, "Failed to set outpost spawn in current location")
                }
                return
            }
        }

        // failed to match, return error
        Message.error(player, "Your town has no outpost in this location")
    }
}
