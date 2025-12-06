package com.example.tuitionmanager.model

import com.example.tuitionmanager.model.data.Batch
import com.example.tuitionmanager.model.data.FeePayment
import com.example.tuitionmanager.model.data.Student
import com.example.tuitionmanager.model.data.Teacher
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing all app data with Firebase Firestore.
 * Uses offline-first architecture with real-time sync.
 * 
 * Firestore Structure:
 * teachers/{auth_uid}/batches/{batchId}
 * teachers/{auth_uid}/students/{studentId}
 */
@Singleton
class Repo @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    // ==================== Helper Properties ====================
    
    /**
     * Get current authenticated user's UID.
     * Returns null if not authenticated.
     */
    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid
    
    /**
     * Get the batches collection reference for the current user.
     */
    private fun batchesCollection() = currentUid?.let {
        firestore.collection("teachers").document(it).collection("batches")
    }
    
    /**
     * Get the students collection reference for the current user.
     */
    private fun studentsCollection() = currentUid?.let {
        firestore.collection("teachers").document(it).collection("students")
    }
    
    /**
     * Get the teacher document reference for the current user.
     * This stores the user's profile (name, email, photoUrl) in Firestore.
     */
    private fun teacherDocument() = currentUid?.let {
        firestore.collection("teachers").document(it)
    }

    // ==================== Teacher/Auth Operations ====================

    /**
     * Check if a user is currently signed in.
     * Also verifies that email is verified for email/password accounts.
     */
    fun isUserSignedIn(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        // For email/password users, require email verification
        // Google users are always considered verified
        val providers = user.providerData.map { it.providerId }
        val isGoogleUser = providers.contains("google.com")
        return isGoogleUser || user.isEmailVerified
    }

    /**
     * Get the current Firebase User.
     */
    fun getCurrentUser() = firebaseAuth.currentUser

    /**
     * Get current teacher information from Firestore.
     * Uses Firestore as Single Source of Truth for profile data.
     * IMPORTANT: Only returns teacher for verified users (email verified OR Google sign-in).
     * Unverified email users are signed out to prevent unauthorized access.
     */
    fun getCurrentTeacher(): Flow<ResultState<Teacher?>> = callbackFlow {
        trySend(ResultState.Loading)
        
        var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            
            // Remove previous Firestore listener if any
            firestoreListener?.remove()
            
            if (user != null) {
                // Reload user to get fresh verification status
                user.reload().addOnCompleteListener { _ ->
                    val refreshedUser = firebaseAuth.currentUser
                    
                    if (refreshedUser != null) {
                        // Check if user is properly validated
                        val providers = refreshedUser.providerData.map { it.providerId }
                        val isGoogleUser = providers.contains("google.com")
                        val isVerified = isGoogleUser || refreshedUser.isEmailVerified
                        
                        if (isVerified) {
                            // User is verified - listen to Firestore for teacher data
                            val teacherDoc = firestore.collection("teachers").document(refreshedUser.uid)
                            firestoreListener = teacherDoc.addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    // Fallback to Auth profile on error
                                    val teacher = Teacher(
                                        uid = refreshedUser.uid,
                                        name = refreshedUser.displayName ?: "Teacher",
                                        email = refreshedUser.email ?: "",
                                        photoUrl = refreshedUser.photoUrl?.toString()
                                    )
                                    trySend(ResultState.Success(teacher))
                                    return@addSnapshotListener
                                }
                                
                                if (snapshot != null && snapshot.exists()) {
                                    // Use Firestore data
                                    val teacher = Teacher(
                                        uid = refreshedUser.uid,
                                        name = snapshot.getString("name") ?: refreshedUser.displayName ?: "Teacher",
                                        email = snapshot.getString("email") ?: refreshedUser.email ?: "",
                                        photoUrl = snapshot.getString("photoUrl") ?: refreshedUser.photoUrl?.toString()
                                    )
                                    trySend(ResultState.Success(teacher))
                                } else {
                                    // Firestore doc doesn't exist yet, use Auth profile
                                    val teacher = Teacher(
                                        uid = refreshedUser.uid,
                                        name = refreshedUser.displayName ?: "Teacher",
                                        email = refreshedUser.email ?: "",
                                        photoUrl = refreshedUser.photoUrl?.toString()
                                    )
                                    trySend(ResultState.Success(teacher))
                                }
                            }
                        } else {
                            // User exists but email NOT verified - sign out and return null
                            firebaseAuth.signOut()
                            trySend(ResultState.Success(null))
                        }
                    } else {
                        trySend(ResultState.Success(null))
                    }
                }
            } else {
                trySend(ResultState.Success(null))
            }
        }
        
        firebaseAuth.addAuthStateListener(authListener)
        
        awaitClose {
            firestoreListener?.remove()
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }
    
    /**
     * Force refresh teacher data.
     * Returns null if user is not verified (email/password without verification).
     */
    suspend fun refreshCurrentTeacher(): Teacher? {
        val user = firebaseAuth.currentUser ?: return null
        try {
            user.reload().await()
        } catch (e: Exception) {
            // Ignore reload errors, use cached data
        }
        val refreshedUser = firebaseAuth.currentUser ?: return null
        
        // Check if user is properly validated
        val providers = refreshedUser.providerData.map { it.providerId }
        val isGoogleUser = providers.contains("google.com")
        val isVerified = isGoogleUser || refreshedUser.isEmailVerified
        
        if (!isVerified) {
            // User not verified - don't return teacher data
            return null
        }
        
        return Teacher(
            uid = refreshedUser.uid,
            name = refreshedUser.displayName ?: "Teacher",
            email = refreshedUser.email ?: "",
            photoUrl = refreshedUser.photoUrl?.toString()
        )
    }

    /**
     * Sign in with email and password.
     * Checks if the email is verified before allowing sign in.
     * Users cannot bypass email verification.
     */
    suspend fun signInWithEmail(email: String, password: String): ResultState<Unit> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            // IMPORTANT: Check if email is verified - no bypass allowed
            if (user != null && !user.isEmailVerified) {
                // Sign out immediately to prevent access
                firebaseAuth.signOut()
                return ResultState.Error("EMAIL_NOT_VERIFIED")
            }
            
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Sign in failed")
        }
    }

    /**
     * Create a new account with email and password.
     * Sends verification email and signs out until verified.
     * Saves profile to Firestore for consistency.
     */
    suspend fun signUpWithEmail(name: String, email: String, password: String): ResultState<Unit> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            
            // Update the user profile with their name
            authResult.user?.let { user ->
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }
                user.updateProfile(profileUpdates).await()
                
                // Try to save profile to Firestore (non-blocking - don't fail signup if this fails)
                try {
                    val teacherData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("teachers").document(user.uid)
                        .set(teacherData, SetOptions.merge()).await()
                } catch (firestoreError: Exception) {
                    // Log but don't fail - Firestore doc will be created on first successful login
                    android.util.Log.w("Repo", "Firestore profile save failed: ${firestoreError.message}")
                }
                
                // Send verification email - MUST succeed for signup to complete
                user.sendEmailVerification().await()
                
                // IMPORTANT: Sign out immediately - user MUST verify before signing in
                firebaseAuth.signOut()
            }
            
            // Return success - verification email has been sent
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Sign up failed")
        }
    }
    
    /**
     * Send password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): ResultState<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to send reset email")
        }
    }
    
    /**
     * Resend verification email to the user.
     */
    suspend fun resendVerificationEmail(email: String, password: String): ResultState<Unit> {
        return try {
            // Sign in temporarily to resend
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.sendEmailVerification()?.await()
            firebaseAuth.signOut()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to resend verification")
        }
    }

    /**
     * Sign in with Google ID Token (from Credential Manager).
     * Handles account linking if the email already exists with a different provider.
     * Preserves existing Firestore profile name if available.
     */
    suspend fun signInWithGoogle(idToken: String): ResultState<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            
            try {
                // Attempt to sign in with Google credential
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                
                authResult.user?.let { user ->
                    // Check if Firestore profile already exists (from previous email signup)
                    val existingDoc = firestore.collection("teachers").document(user.uid).get().await()
                    
                    if (!existingDoc.exists()) {
                        // No existing profile - create one with Google data
                        val googleUser = authResult.additionalUserInfo?.profile
                        val googleName = googleUser?.get("name") as? String ?: user.displayName ?: "Teacher"
                        val googlePhoto = user.photoUrl?.toString()
                        
                        val teacherData = hashMapOf(
                            "name" to googleName,
                            "email" to (user.email ?: ""),
                            "photoUrl" to googlePhoto,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        firestore.collection("teachers").document(user.uid)
                            .set(teacherData, SetOptions.merge()).await()
                    }
                    // If profile exists, keep the existing name (from email signup)
                }
                
                ResultState.Success(Unit)
            } catch (e: FirebaseAuthUserCollisionException) {
                // Account exists with different credential
                try {
                    firebaseAuth.signInWithCredential(credential).await()
                    ResultState.Success(Unit)
                } catch (innerE: Exception) {
                    ResultState.Error("Account exists with email/password. Please sign in with email.")
                }
            }
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Google sign in failed")
        }
    }

    /**
     * Update the user's display name in both Auth and Firestore.
     */
    suspend fun updateUserProfile(name: String): ResultState<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return ResultState.Error("Not signed in")
            
            // Update Auth profile
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            user.updateProfile(profileUpdates).await()
            
            // Update Firestore profile (Single Source of Truth)
            val teacherData = hashMapOf<String, Any>(
                "name" to name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("teachers").document(user.uid)
                .set(teacherData, SetOptions.merge()).await()
            
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to update profile")
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    // ==================== Batch Operations ====================

    /**
     * Get all batches for the current user with real-time updates.
     */
    fun getAllBatches(): Flow<ResultState<List<Batch>>> = callbackFlow {
        trySend(ResultState.Loading)
        
        val collection = batchesCollection()
        if (collection == null) {
            trySend(ResultState.Error("User not authenticated"))
            close()
            return@callbackFlow
        }
        
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(ResultState.Error(error.message ?: "Failed to load batches"))
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val batches = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Batch::class.java)
                }
                trySend(ResultState.Success(batches))
            }
        }
        
        awaitClose { listener.remove() }
    }

    /**
     * Add a new batch.
     */
    suspend fun addBatch(batch: Batch): ResultState<String> {
        return try {
            val collection = batchesCollection()
                ?: return ResultState.Error("User not authenticated")
            
            val batchId = batch.id.ifEmpty { UUID.randomUUID().toString() }
            
            // Create a map for Firestore, letting ServerTimestamp work for creationDate
            // NOTE: Do NOT include "id" in the data - it's handled by @DocumentId annotation
            val batchData = hashMapOf(
                "name" to batch.name,
                "standard" to batch.standard,
                "feeAmount" to batch.feeAmount,
                "schedule" to batch.schedule.map { schedule ->
                    mapOf("day" to schedule.day, "time" to schedule.time)
                },
                "creationDate" to FieldValue.serverTimestamp()
            )
            
            collection.document(batchId).set(batchData).await()
            ResultState.Success(batchId)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to add batch")
        }
    }

    /**
     * Update an existing batch.
     * Preserves the original creationDate.
     */
    suspend fun updateBatch(batch: Batch): ResultState<Unit> {
        return try {
            val collection = batchesCollection()
                ?: return ResultState.Error("User not authenticated")
            
            // Update only editable fields, preserve creationDate
            val updates = hashMapOf<String, Any?>(
                "name" to batch.name,
                "standard" to batch.standard,
                "feeAmount" to batch.feeAmount,
                "schedule" to batch.schedule.map { schedule ->
                    mapOf("day" to schedule.day, "time" to schedule.time)
                }
            )
            
            collection.document(batch.id).update(updates).await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to update batch")
        }
    }

    /**
     * Delete a batch and all its students (cascading delete).
     */
    suspend fun deleteBatch(batchId: String): ResultState<Unit> {
        return try {
            val batchCollection = batchesCollection()
                ?: return ResultState.Error("User not authenticated")
            val studentCollection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            // First, delete all students in this batch
            val studentsSnapshot = studentCollection
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            
            // Use batch write for atomic deletion
            val writeBatch = firestore.batch()
            
            for (studentDoc in studentsSnapshot.documents) {
                writeBatch.delete(studentDoc.reference)
            }
            
            // Delete the batch itself
            writeBatch.delete(batchCollection.document(batchId))
            
            // Commit the batch
            writeBatch.commit().await()
            
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to delete batch")
        }
    }

    /**
     * Get a single batch by ID.
     */
    suspend fun getBatchById(batchId: String): ResultState<Batch?> {
        return try {
            val collection = batchesCollection()
                ?: return ResultState.Error("User not authenticated")
            
            val doc = collection.document(batchId).get().await()
            val batch = doc.toObject(Batch::class.java)
            ResultState.Success(batch)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to get batch")
        }
    }

    // ==================== Student Operations ====================

    /**
     * Get all students for the current user with real-time updates.
     */
    fun getAllStudents(): Flow<ResultState<List<Student>>> = callbackFlow {
        trySend(ResultState.Loading)
        
        val collection = studentsCollection()
        if (collection == null) {
            trySend(ResultState.Error("User not authenticated"))
            close()
            return@callbackFlow
        }
        
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(ResultState.Error(error.message ?: "Failed to load students"))
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val students = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Student::class.java)
                }
                trySend(ResultState.Success(students))
            }
        }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get students in a specific batch with real-time updates.
     */
    fun getStudentsInBatch(batchId: String): Flow<ResultState<List<Student>>> = callbackFlow {
        trySend(ResultState.Loading)
        
        val collection = studentsCollection()
        if (collection == null) {
            trySend(ResultState.Error("User not authenticated"))
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .whereEqualTo("batchId", batchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ResultState.Error(error.message ?: "Failed to load students"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val students = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Student::class.java)
                    }
                    trySend(ResultState.Success(students))
                }
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Add a new student.
     */
    suspend fun addStudent(student: Student): ResultState<String> {
        return try {
            val collection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            val studentId = student.id.ifEmpty { UUID.randomUUID().toString() }
            
            // Create a map for Firestore
            // NOTE: Do NOT include "id" in the data - it's handled by @DocumentId annotation
            val studentData = hashMapOf(
                "batchId" to student.batchId,
                "name" to student.name,
                "phone" to student.phone,
                "joiningDate" to student.joiningDate,
                "feesPaid" to student.feesPaid.mapValues { (_, payment) ->
                    mapOf(
                        "status" to payment.status,
                        "paidAt" to payment.paidAt
                    )
                }
            )
            
            collection.document(studentId).set(studentData).await()
            ResultState.Success(studentId)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to add student")
        }
    }

    /**
     * Update an existing student.
     */
    suspend fun updateStudent(student: Student): ResultState<Unit> {
        return try {
            val collection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            // NOTE: Do NOT include "id" in the data - it's handled by @DocumentId annotation
            val studentData = hashMapOf(
                "batchId" to student.batchId,
                "name" to student.name,
                "phone" to student.phone,
                "joiningDate" to student.joiningDate,
                "feesPaid" to student.feesPaid.mapValues { (_, payment) ->
                    mapOf(
                        "status" to payment.status,
                        "paidAt" to payment.paidAt
                    )
                }
            )
            
            collection.document(student.id).set(studentData, SetOptions.merge()).await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to update student")
        }
    }

    /**
     * Delete a student.
     */
    suspend fun deleteStudent(studentId: String): ResultState<Unit> {
        return try {
            val collection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            collection.document(studentId).delete().await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to delete student")
        }
    }

    /**
     * Get a single student by ID.
     */
    suspend fun getStudentById(studentId: String): ResultState<Student?> {
        return try {
            val collection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            val doc = collection.document(studentId).get().await()
            val student = doc.toObject(Student::class.java)
            ResultState.Success(student)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to get student")
        }
    }

    // ==================== Fee Operations ====================

    /**
     * Mark fee as paid for a student for a specific month.
     * @param studentId The student's ID
     * @param monthKey Format: "MM-yyyy"
     */
    suspend fun markFeePaid(studentId: String, monthKey: String): ResultState<Unit> {
        return try {
            val collection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            // Update the feesPaid map with the new payment
            val paymentData = mapOf(
                "status" to "PAID",
                "paidAt" to FieldValue.serverTimestamp()
            )
            
            collection.document(studentId)
                .update("feesPaid.$monthKey", paymentData)
                .await()
            
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to mark fee as paid")
        }
    }

    /**
     * Mark fee as unpaid (remove payment record) for a student for a specific month.
     * @param studentId The student's ID
     * @param monthKey Format: "MM-yyyy"
     */
    suspend fun markFeeUnpaid(studentId: String, monthKey: String): ResultState<Unit> {
        return try {
            val collection = studentsCollection()
                ?: return ResultState.Error("User not authenticated")
            
            // Remove the payment key from feesPaid map
            collection.document(studentId)
                .update("feesPaid.$monthKey", FieldValue.delete())
                .await()
            
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to mark fee as unpaid")
        }
    }
}
