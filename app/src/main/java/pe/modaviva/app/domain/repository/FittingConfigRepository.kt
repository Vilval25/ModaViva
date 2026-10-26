package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.modaviva.app.domain.model.Prenda

/**
 * Repositorio de configuración dinámica para el probador virtual (HU-16).
 * Administra las categorías y prendas habilitadas o restringidas mediante Firebase Remote Config.
 */
interface FittingConfigRepository {

    /**
     * Conjunto de slugs y nombres de categorías no habilitadas para el probador.
     */
    val disabledCategories: StateFlow<Set<String>>

    /**
     * Determina si una prenda específica está habilitada para el probador virtual.
     */
    fun isPrendaHabilitadaParaProbador(prenda: Prenda): Boolean

    /**
     * Determina si una categoría o subcategoría está habilitada para el probador virtual.
     */
    fun isCategoriaHabilitada(categoria: String, subcategoria: String? = null): Boolean

    /**
     * Sincroniza las reglas de Remote Config con un intervalo de refresco de 15 minutos (CA-03).
     */
    suspend fun fetchAndActivate(): Boolean
}
