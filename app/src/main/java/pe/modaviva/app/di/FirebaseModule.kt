package pe.modaviva.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Los SDK de Firebase se crean a partir del google-services.json que el
 * plugin genera, por lo que no reciben dependencias por constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    /** Misma región en la que se despliegan las funciones (functions/src/index.ts). */
    @Provides
    @Singleton
    fun provideFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
}
