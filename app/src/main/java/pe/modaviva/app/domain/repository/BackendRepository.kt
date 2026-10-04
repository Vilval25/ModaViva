package pe.modaviva.app.domain.repository

import pe.modaviva.app.domain.model.BackendPing

interface BackendRepository {

    /** Prueba de humo del backend (HT-02 CA-05): llama a la Cloud Function `ping`. */
    suspend fun ping(): Result<BackendPing>
}
