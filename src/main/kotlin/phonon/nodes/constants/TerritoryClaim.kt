/**
 * Enum statuses for result of claim/unclaim territory
 * for a town.
 * 
 * Allows printing/handling different error results of
 * claiming territory
 */

package phonon.nodes.constants

// claim errors
val ErrorTooManyClaims = Exception("La town tiene muchos claims")
val ErrorTerritoryNotConnected = Exception("Territorio no conectado a los claims de la town")
val ErrorTerritoryHasClaim = Exception("El territorio ya tiene una town")

// unclaim errors
val ErrorTerritoryIsTownHome = Exception("El territorio es el home de la town")
val ErrorTerritoryNotInTown = Exception("El territorio no pertenece a una town")
