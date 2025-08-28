/**
 * CustomIncomeItem
 * 
 * Represents an item with custom model data for the income system.
 * This allows the income system to support items from ItemsAdder or
 * other plugins that use CustomModelData.
 */

package phonon.nodes.objects

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Represents a custom item for income with support for CustomModelData
 */
data class CustomIncomeItem(
    val material: Material,
    val customModelData: Int? = null,
    val displayName: String? = null,
    val lore: List<String>? = null
) {
    
    /**
     * Create an ItemStack from this custom income item
     */
    fun toItemStack(amount: Int): ItemStack {
        val itemStack = ItemStack(material, amount)
        
        if (customModelData != null || displayName != null || lore != null) {
            val meta = itemStack.itemMeta
            
            customModelData?.let { meta?.setCustomModelData(it) }
            displayName?.let { meta?.setDisplayName(it) }
            lore?.let { meta?.lore = it }
            
            itemStack.itemMeta = meta
        }
        
        return itemStack
    }
    
    /**
     * Check if an ItemStack matches this custom income item
     */
    fun matches(itemStack: ItemStack): Boolean {
        if (itemStack.type != material) return false
        
        val meta = itemStack.itemMeta ?: return (customModelData == null && displayName == null && lore == null)
        
        // Check CustomModelData
        if (customModelData != null) {
            if (!meta.hasCustomModelData() || meta.customModelData != customModelData) {
                return false
            }
        } else if (meta.hasCustomModelData()) {
            return false
        }
        
        // Check display name
        if (displayName != null) {
            if (!meta.hasDisplayName() || meta.displayName != displayName) {
                return false
            }
        } else if (meta.hasDisplayName()) {
            return false
        }
        
        // Check lore
        if (lore != null) {
            if (!meta.hasLore() || meta.lore != lore) {
                return false
            }
        } else if (meta.hasLore()) {
            return false
        }
        
        return true
    }
    
    /**
     * Get a string identifier for this item
     */
    fun getIdentifier(): String {
        return buildString {
            append(material.name.lowercase())
            customModelData?.let { append(":$it") }
            displayName?.let { append(":name:${it.replace(":", "\\:")}")}
            lore?.let { append(":lore:${it.joinToString("|") { line -> line.replace(":", "\\:").replace("|", "\\|") }}") }
        }
    }
    
    companion object {
        /**
         * Parse a custom income item from string identifier
         * Format: "material" or "material:customModelData" or "material:customModelData:name:displayName:lore:line1|line2"
         */
        fun fromIdentifier(identifier: String): CustomIncomeItem? {
            val parts = identifier.split(":")
            if (parts.isEmpty()) return null
            
            val material = Material.matchMaterial(parts[0].uppercase()) ?: return null
            
            var customModelData: Int? = null
            var displayName: String? = null
            var lore: List<String>? = null
            
            if (parts.size > 1) {
                customModelData = parts[1].toIntOrNull()
            }
            
            if (parts.size > 3 && parts[2] == "name") {
                displayName = parts[3].replace("\\:", ":")
            }
            
            if (parts.size > 5 && parts[4] == "lore") {
                lore = parts[5].split("|").map { it.replace("\\:", ":").replace("\\|", "|") }
            }
            
            return CustomIncomeItem(material, customModelData, displayName, lore)
        }
        
        /**
         * Create a CustomIncomeItem from an existing ItemStack
         */
        fun fromItemStack(itemStack: ItemStack): CustomIncomeItem {
            val meta = itemStack.itemMeta
            return CustomIncomeItem(
                material = itemStack.type,
                customModelData = if (meta?.hasCustomModelData() == true) meta.customModelData else null,
                displayName = if (meta?.hasDisplayName() == true) meta.displayName else null,
                lore = if (meta?.hasLore() == true) meta.lore else null
            )
        }
    }
}
