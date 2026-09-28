package pe.modaviva.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.data.repository.FakeAuthRepository
import pe.modaviva.app.data.repository.InMemorySessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: InMemorySessionRepository,
    ): SessionRepository
}
