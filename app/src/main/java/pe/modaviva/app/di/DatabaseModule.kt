package pe.modaviva.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.modaviva.app.data.local.CartDao
import pe.modaviva.app.data.local.CatalogoDao
import pe.modaviva.app.data.local.Migraciones
import pe.modaviva.app.data.local.ModaVivaDatabase
import pe.modaviva.app.data.local.PortadaDao
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ModaVivaDatabase =
        Room.databaseBuilder(context, ModaVivaDatabase::class.java, "modaviva.db")
            .addMigrations(*Migraciones.TODAS)
            // Las versiones 1 a 4 no guardaron su esquema, así que no se
            // pueden migrar: esas bases (solo de desarrollo) se borran una
            // vez. Desde la 5, un cambio sin migración hace fallar la app en
            // vez de borrar datos en silencio.
            .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
            .build()

    @Provides
    fun provideCatalogoDao(database: ModaVivaDatabase): CatalogoDao = database.catalogoDao()

    @Provides
    fun provideCartDao(database: ModaVivaDatabase): CartDao = database.cartDao()

    @Provides
    fun providePortadaDao(database: ModaVivaDatabase): PortadaDao = database.portadaDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
