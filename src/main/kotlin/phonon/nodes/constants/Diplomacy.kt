/**
 * Constants for diplomatic relations
 * (town, nation ally/enemy functions)
 */

package phonon.nodes.constants

/**
 * Simple relationship groups:
 * Town - contains town residents
 * Ally - contains nation towns and other allies
 * Neutral - neutral towns, or players with no town
 * Enemy - enemy towns
 */
public enum class DiplomaticRelationship {
    TOWN,
    NATION,
    ALLY,
    NEUTRAL,
    ENEMY
}

// constants for setting enemy
public val ErrorWarAllyOrTruce = Exception("No se puede declarar guerra a un aliado o una tregua")
public val ErrorAlreadyEnemies = Exception("Ya enemigos")
public val ErrorAlreadyAllies = Exception("Ya aliados")

// constants for adding/removing ally
public val ErrorNotAllies = Exception("No aliados")

// constants for adding/remove truce
public val ErrorAlreadyTruce = Exception("Ya hay una tregua")
