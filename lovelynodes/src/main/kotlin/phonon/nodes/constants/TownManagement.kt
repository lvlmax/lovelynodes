/**
 * Enum statuses for town management functions
 * 
 */

package phonon.nodes.constants

// town create errors
public val ErrorTownExists = Exception("Ese nombre ya lo tiene otra town")
public val ErrorPlayerHasTown = Exception("El jugador ya está en la town")
public val ErrorTerritoryOwned = Exception("El territorio ya tiene town") // doubles as territory claim error

// town territory claim/unclaim errors
// ErrorTerritoryOwned doubles as error
