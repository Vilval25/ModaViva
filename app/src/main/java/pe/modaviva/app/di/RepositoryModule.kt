package pe.modaviva.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.modaviva.app.data.network.MonitorDeConexion
import pe.modaviva.app.data.network.MonitorDeConexionAndroid
import pe.modaviva.app.data.repository.FirebaseAuthRepository
import pe.modaviva.app.data.repository.FirebaseBackendRepository
import pe.modaviva.app.data.repository.FirestoreCatalogoRepository
import pe.modaviva.app.data.repository.FirestoreStockRepository
import pe.modaviva.app.data.repository.InMemorySessionRepository
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.repository.StockRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: InMemorySessionRepository,
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindBackendRepository(
        impl: FirebaseBackendRepository,
    ): BackendRepository

    @Binds
    @Singleton
    abstract fun bindCatalogoRepository(
        impl: FirestoreCatalogoRepository,
    ): CatalogoRepository

    @Binds
    @Singleton
    abstract fun bindMonitorDeConexion(
        impl: MonitorDeConexionAndroid,
    ): MonitorDeConexion

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        impl: FirestoreStockRepository,
    ): StockRepository
}