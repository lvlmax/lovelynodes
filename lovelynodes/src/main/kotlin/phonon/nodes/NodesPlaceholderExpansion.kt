package phonon.nodes.hooks

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import phonon.nodes.Nodes

class NodesPlaceholderExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "node"
    }

    override fun getAuthor(): String {
        return "Desau pa los placeholders, de nada"
    }

    override fun getVersion(): String {
        return "1.0"
    }

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        if(player == null) {
            return ""
        }

        val resident = Nodes.getResident(player) ?: return ""

        return when(identifier) {
            "nacion" -> {
                resident.nation?.name ?: "Sin Nación"
            }
            "town" -> {
                resident.town?.name ?: "Sin Ciudad" 
            }
            else -> null
        }
    }
}