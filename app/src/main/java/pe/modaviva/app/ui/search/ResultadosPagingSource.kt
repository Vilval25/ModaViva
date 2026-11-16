package pe.modaviva.app.ui.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import pe.modaviva.app.domain.model.Prenda

/**
 * HU-06 CA-05 — Entrega los resultados ya filtrados y ordenados en páginas de [TAMANO_PAGINA].
 * La clave es la posición de inicio de cada página.
 */
class ResultadosPagingSource(
    private val resultados: List<Prenda>,
    private val esperaEntrePaginasMs: Long = ESPERA_ENTRE_PAGINAS_MS,
) : PagingSource<Int, Prenda>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Prenda> {
        val inicio = params.key ?: 0
        // Las páginas siguientes llegan con una breve espera para que el indicador de carga del
        // final de la lista sea visible (CA-05); la primera se muestra al instante.
        if (inicio > 0 && esperaEntrePaginasMs > 0) delay(esperaEntrePaginasMs)
        val fin = minOf(inicio + params.loadSize, resultados.size)
        return LoadResult.Page(
            data = if (inicio < fin) resultados.subList(inicio, fin) else emptyList(),
            prevKey = if (inicio == 0) null else maxOf(0, inicio - params.loadSize),
            nextKey = if (fin >= resultados.size) null else fin,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Prenda>): Int? =
        state.anchorPosition?.let { posicion -> (posicion / TAMANO_PAGINA) * TAMANO_PAGINA }

    companion object {
        const val TAMANO_PAGINA = 20
        private const val ESPERA_ENTRE_PAGINAS_MS = 400L
    }
}
