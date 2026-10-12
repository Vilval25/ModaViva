package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Favorito
import pe.modaviva.app.domain.model.PrendaRetirada

/**
 * Favoritos del cliente con sesión (HU-08). Se guardan en Firestore
 * (clientes/{uid}/favoritos) para verlos en cualquier dispositivo y se copian
 * en Room para leerlos sin conexión (CA-06).
 */
interface FavoritosRepository {

    /** Favoritos de la cuenta, de la más reciente a la más antigua (CA-02). */
    val favoritos: Flow<List<Favorito>>

    suspend fun marcar(prendaId: String)

    suspend fun quitar(prendaId: String)

    /** Vuelve a guardar un favorito recién quitado, con su fecha original (CA-05). */
    suspend fun restaurar(favorito: Favorito)

    /**
     * Prenda que un invitado quiso marcar: se marca cuando inicie sesión
     * (CA-08). null la olvida (p. ej. si cierra el aviso sin entrar).
     */
    fun marcarAlIniciarSesion(prendaId: String?)

    /** Nombre, marca y foto de una favorita que ya no está publicada (CA-03). */
    suspend fun buscarRetirada(prendaId: String): PrendaRetirada?
}
