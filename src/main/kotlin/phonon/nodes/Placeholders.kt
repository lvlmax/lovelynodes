package phonon.nodes.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import phonon.nodes.Nodes

class NodesExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "nodes" // el prefijo: %nodes_xxx%
    }

    override fun getAuthor(): String {
        return "TuNombre"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun persist(): Boolean {
        return true 
    }

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        if (player == null) return ""

        val resident = Nodes.getResident(player)

        return when (identifier.lowercase()) {
            "town" -> resident?.town?.name ?: "Sin Town"
            "nation" -> resident?.nation?.name ?: "Sin Nación"
            else -> null
        }
    }
}
