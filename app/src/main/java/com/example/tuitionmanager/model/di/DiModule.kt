package com.example.tuitionmanager.model.di

import com.example.tuitionmanager.model.Repo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        
        // Enable offline persistence (default is already enabled on Android)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
        
        return firestore
    }

    @Provides
    @Singleton
    fun provideFireAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideRepo(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): Repo {
        return Repo(firestore, firebaseAuth)
    }
}
