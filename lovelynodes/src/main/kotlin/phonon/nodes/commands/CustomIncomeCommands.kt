/**
 * CustomIncomeCommands
 * 
 * Comandos para administrar custom income items con CustomModelData
 */

package phonon.nodes.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import phonon.nodes.Nodes
import phonon.nodes.Message
import phonon.nodes.objects.CustomIncomeItem

object CustomIncomeCommands {
    
    /**
     * @command /nodesadmin customincome add [identifier] [amount] [town]
     * Add custom income item to a town directly
     */
    fun addCustomIncome(sender: CommandSender, args: Array<String>) {
        if (args.size < 5) {
            Message.error(sender, "Usage: /nodesadmin customincome add [identifier] [amount] [town]")
            return
        }
        
        val identifier = args[2]
        val amount = args[3].toIntOrNull() ?: run {
            Message.error(sender, "Invalid amount: ${args[3]}")
            return
        }
        val townName = args[4]
        
        val town = Nodes.towns[townName] ?: run {
            Message.error(sender, "Town not found: $townName")
            return
        }
        
        val customItem = CustomIncomeItem.fromIdentifier(identifier) ?: run {
            Message.error(sender, "Invalid custom item identifier: $identifier")
            return
        }
        
        town.income.addCustomItem(customItem, amount)
        Message.print(sender, "Added $amount x ${customItem.getIdentifier()} to ${town.name}'s income")
    }
    
    /**
     * @command /nodesadmin customincome create [material] [customModelData] [displayName] [lore]
     * Create a custom income item from hand or parameters
     */
    fun createCustomIncomeItem(sender: CommandSender, args: Array<String>) {
        val player = sender as? Player ?: run {
            Message.error(sender, "This command must be run by a player")
            return
        }
        
        val itemInHand = player.inventory.itemInMainHand
        
        if (args.size >= 3) {
            // Create from parameters
            val materialName = args[2]
            val material = Material.matchMaterial(materialName) ?: run {
                Message.error(sender, "Invalid material: $materialName")
                return
            }
            
            val customModelData = if (args.size > 3) args[3].toIntOrNull() else null
            val displayName = if (args.size > 4) args[4].replace("_", " ") else null
            val lore = if (args.size > 5) args[5].split("|").map { it.replace("_", " ") } else null
            
            val customItem = CustomIncomeItem(material, customModelData, displayName, lore)
            val itemStack = customItem.toItemStack(1)
            
            player.inventory.addItem(itemStack)
            Message.print(sender, "Created custom item: ${customItem.getIdentifier()}")
        } else {
            // Create from item in hand
            if (itemInHand.type == Material.AIR) {
                Message.error(sender, "Hold an item in your hand or provide parameters")
                return
            }
            
            val customItem = CustomIncomeItem.fromItemStack(itemInHand)
            Message.print(sender, "Item identifier: ${customItem.getIdentifier()}")
            Message.print(sender, "Use this identifier in config files or resource nodes")
        }
    }
    
    /**
     * @command /nodesadmin customincome give [identifier] [amount] [player]
     * Give a custom income item to a player
     */
    fun giveCustomIncomeItem(sender: CommandSender, args: Array<String>) {
        if (args.size < 5) {
            Message.error(sender, "Usage: /nodesadmin customincome give [identifier] [amount] [player]")
            return
        }
        
        val identifier = args[2]
        val amount = args[3].toIntOrNull() ?: run {
            Message.error(sender, "Invalid amount: ${args[3]}")
            return
        }
        val playerName = args[4]
        
        val customItem = CustomIncomeItem.fromIdentifier(identifier) ?: run {
            Message.error(sender, "Invalid custom item identifier: $identifier")
            return
        }
        
        val targetPlayer = Nodes.plugin?.server?.getPlayer(playerName) ?: run {
            Message.error(sender, "Player not found: $playerName")
            return
        }
        
        val itemStack = customItem.toItemStack(amount)
        targetPlayer.inventory.addItem(itemStack)
        
        Message.print(sender, "Gave $amount x ${customItem.getIdentifier()} to ${targetPlayer.name}")
        Message.print(targetPlayer, "Received $amount x custom item")
    }
    
    /**
     * @command /nodesadmin customincome list [town]
     * List custom income items for a town
     */
    fun listCustomIncome(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            Message.error(sender, "Usage: /nodesadmin customincome list [town]")
            return
        }
        
        val townName = args[2]
        val town = Nodes.towns[townName] ?: run {
            Message.error(sender, "Town not found: $townName")
            return
        }
        
        Message.print(sender, "Custom income for ${town.name}:")
        if (town.income.storageCustomItems.isEmpty()) {
            Message.print(sender, "No custom income items")
            return
        }
        
        for ((identifier, amount) in town.income.storageCustomItems) {
            val customItem = CustomIncomeItem.fromIdentifier(identifier)
            if (customItem != null) {
                Message.print(sender, "- $amount x ${customItem.material.name}" + 
                    (customItem.customModelData?.let { " (CMD: $it)" } ?: "") +
                    (customItem.displayName?.let { " \"$it\"" } ?: ""))
            } else {
                Message.print(sender, "- $amount x $identifier (invalid)")
            }
        }
    }
}
