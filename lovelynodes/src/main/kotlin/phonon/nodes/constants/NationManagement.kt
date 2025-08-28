/**
 * Enum statuses for nation management functions
 * 
 */

package phonon.nodes.constants

// exceptions during nation creation
public val ErrorNationExists = Exception("El nombre de la nación ya existe")
public val ErrorTownHasNation = Exception("El jugador ya tiene una town")
public val ErrorPlayerHasNation = Exception("El jugador ya tiene una nación")
public val ErrorPlayerNotInTown = Exception("El jugador no está en la town")
public val ErrorNationDoesNotHaveTown = Exception("La nación no tiene una town")

// exception during loading nation
public val ErrorTownDoesNotExist = Exception("La town no existe")
