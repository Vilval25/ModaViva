package pe.modaviva.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.modaviva.app.data.local.CatalogoDao
import pe.modaviva.app.data.local.ModaVivaDatabase
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ModaVivaDatabase =
        Room.databaseBuilder(context, ModaVivaDatabase::class.java, "modaviva.db")
            // Solo hay copias de datos del servidor: ante un cambio de esquema
            // se borra y se vuelve a descargar (ver ModaVivaDatabase).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideCatalogoDao(database: ModaVivaDatabase): CatalogoDao = database.catalogoDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
