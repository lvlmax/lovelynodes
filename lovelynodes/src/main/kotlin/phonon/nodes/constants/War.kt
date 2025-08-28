/**
 * Enum statuses for war functions
 */

package phonon.nodes.constants

// flag war attack errors
public val ErrorNoTerritory = Exception("[War] Aquí no hay ningún territorio")
public val ErrorAlreadyUnderAttack = Exception("[War] Este chunk ya está bajo ataque")
public val ErrorAlreadyCaptured = Exception("[War] Chunk ya capturado por la town o un aliado")
public val ErrorTownBlacklisted = Exception("[War] No puedes atacar a esta town (en la lista negra)")
public val ErrorTownNotWhitelisted = Exception("[War] No puedes atacar esta town (no está en la whitelist)")
public val ErrorNotEnemy = Exception("[War] Este chunk no le pertenece a un enemigo")
public val ErrorNotBorderTerritory = Exception("[War] Solo se puede atacar territorios que hagan frontera")
public val ErrorChunkNotEdge = Exception("[War] El chunk no está en el borde")
public val ErrorFlagTooHigh = Exception("[War] La flag está colocada muy alta, no se puede crear")
public val ErrorSkyBlocked = Exception("[War] La flag debe de poder ver el cielo")
public val ErrorTooManyAttacks = Exception("[War] No puedes atacar más chunks a la vez")
public val ErrorAttackCustomCancel = Exception("[War] Evento custom cancelado")
