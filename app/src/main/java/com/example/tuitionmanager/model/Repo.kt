package com.example.tuitionmanager.model

import com.example.tuitionmanager.model.data.Batch
import com.example.tuitionmanager.model.data.FeePayment
import com.example.tuitionmanager.model.data.Schedule
import com.example.tuitionmanager.model.data.Student
import com.example.tuitionmanager.model.data.Teacher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing all app data.
 * Currently uses dummy data for UI testing.
 * Will be connected to Firebase Firestore in the backend phase.
 */
@Singleton
class Repo @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    // Current teacher (user) information
    private val _currentTeacher = MutableStateFlow<Teacher?>(null)
    val currentTeacher: StateFlow<Teacher?> = _currentTeacher.asStateFlow()

    // All batches
    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

    // All students
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Initialize with dummy data for UI testing
        loadDummyData()
    }

    /**
     * Load dummy data for UI testing.
     * This will be replaced with Firestore data loading.
     */
    private fun loadDummyData() {
        // Dummy teacher
        _currentTeacher.value = Teacher(
            uid = "dummy-uid-123",
            name = "John Doe",
            email = "john.doe@example.com",
            photoUrl = null
        )

        // Dummy batches with creation dates
        val dummyBatches = listOf(
            Batch(
                id = "batch-1",
                name = "Class 10 Physics",
                standard = "10th",
                feeAmount = 1500.0,
                schedule = listOf(
                    Schedule(day = 1, time = "05:00 PM"), // Monday
                    Schedule(day = 3, time = "05:00 PM"), // Wednesday
                    Schedule(day = 5, time = "05:00 PM")  // Friday
                ),
                creationDate = getDateMonthsAgo(8)
            ),
            Batch(
                id = "batch-2",
                name = "Class 12 Chemistry",
                standard = "12th",
                feeAmount = 2000.0,
                schedule = listOf(
                    Schedule(day = 2, time = "04:00 PM"), // Tuesday
                    Schedule(day = 4, time = "04:00 PM")  // Thursday
                ),
                creationDate = getDateMonthsAgo(10)
            ),
            Batch(
                id = "batch-3",
                name = "Class 9 Mathematics",
                standard = "9th",
                feeAmount = 1200.0,
                schedule = listOf(
                    Schedule(day = 1, time = "03:00 PM"), // Monday
                    Schedule(day = 6, time = "10:00 AM")  // Saturday
                ),
                creationDate = getDateMonthsAgo(6)
            )
        )
        _batches.value = dummyBatches

        // Dummy students
        val currentMonthKey = Student.generateMonthKey()
        val lastMonthKey = run {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            Student.generateMonthKey(cal.time)
        }

        val dummyStudents = listOf(
            // Students for Class 10 Physics (batch-1)
            Student(
                id = "student-1",
                batchId = "batch-1",
                name = "Aarav Sharma",
                phone = "+91 98765 43210",
                joiningDate = getDateMonthsAgo(6),
                feesPaid = mapOf(
                    currentMonthKey to FeePayment(status = "PAID", paidAt = Date()),
                    lastMonthKey to FeePayment(status = "PAID", paidAt = getDateDaysAgo(30))
                )
            ),
            Student(
                id = "student-2",
                batchId = "batch-1",
                name = "Priya Patel",
                phone = "+91 87654 32109",
                joiningDate = getDateMonthsAgo(4),
                feesPaid = mapOf(
                    lastMonthKey to FeePayment(status = "PAID", paidAt = getDateDaysAgo(25))
                )
            ),
            Student(
                id = "student-3",
                batchId = "batch-1",
                name = "Rohan Verma",
                phone = "+91 76543 21098",
                joiningDate = getDateMonthsAgo(1), // Joined recently
                feesPaid = mapOf(
                    currentMonthKey to FeePayment(status = "PAID", paidAt = Date())
                )
            ),

            // Students for Class 12 Chemistry (batch-2)
            Student(
                id = "student-4",
                batchId = "batch-2",
                name = "Ananya Singh",
                phone = "+91 65432 10987",
                joiningDate = getDateMonthsAgo(8),
                feesPaid = mapOf(
                    currentMonthKey to FeePayment(status = "PAID", paidAt = Date()),
                    lastMonthKey to FeePayment(status = "PAID", paidAt = getDateDaysAgo(28))
                )
            ),
            Student(
                id = "student-5",
                batchId = "batch-2",
                name = "Vikram Reddy",
                phone = "+91 54321 09876",
                joiningDate = getDateMonthsAgo(2),
                feesPaid = emptyMap()
            ),

            // Students for Class 9 Mathematics (batch-3)
            Student(
                id = "student-6",
                batchId = "batch-3",
                name = "Ishaan Kumar",
                phone = "+91 43210 98765",
                joiningDate = getDateMonthsAgo(5),
                feesPaid = mapOf(
                    currentMonthKey to FeePayment(status = "PAID", paidAt = Date())
                )
            ),
            Student(
                id = "student-7",
                batchId = "batch-3",
                name = "Kavya Nair",
                phone = "+91 32109 87654",
                joiningDate = getDateMonthsAgo(1),
                feesPaid = emptyMap()
            )
        )
        _students.value = dummyStudents
    }

    private fun getDateMonthsAgo(months: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -months)
        return cal.time
    }

    private fun getDateDaysAgo(days: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return cal.time
    }

    // ==================== Batch Operations ====================

    /**
     * Add a new batch.
     */
    fun addBatch(batch: Batch) {
        _batches.value = _batches.value + batch
    }

    /**
     * Update an existing batch.
     */
    fun updateBatch(batch: Batch) {
        _batches.value = _batches.value.map {
            if (it.id == batch.id) batch else it
        }
    }

    /**
     * Delete a batch and all its students.
     */
    fun deleteBatch(batchId: String) {
        _batches.value = _batches.value.filter { it.id != batchId }
        _students.value = _students.value.filter { it.batchId != batchId }
    }

    /**
     * Get a batch by ID.
     */
    fun getBatchById(batchId: String): Batch? {
        return _batches.value.find { it.id == batchId }
    }

    /**
     * Get batches that have classes on a specific day.
     * @param dayOfWeek 1=Monday, 7=Sunday
     */
    fun getBatchesForDay(dayOfWeek: Int): List<Batch> {
        return _batches.value.filter { it.hasClassOnDay(dayOfWeek) }
    }

    // ==================== Student Operations ====================

    /**
     * Add a new student.
     */
    fun addStudent(student: Student) {
        _students.value = _students.value + student
    }

    /**
     * Update an existing student.
     */
    fun updateStudent(student: Student) {
        _students.value = _students.value.map {
            if (it.id == student.id) student else it
        }
    }

    /**
     * Delete a student.
     */
    fun deleteStudent(studentId: String) {
        _students.value = _students.value.filter { it.id != studentId }
    }

    /**
     * Get a student by ID.
     */
    fun getStudentById(studentId: String): Student? {
        return _students.value.find { it.id == studentId }
    }

    /**
     * Get all students in a batch.
     */
    fun getStudentsInBatch(batchId: String): List<Student> {
        return _students.value.filter { it.batchId == batchId }
    }

    /**
     * Get the count of students in a batch.
     */
    fun getStudentCountInBatch(batchId: String): Int {
        return _students.value.count { it.batchId == batchId }
    }

    // ==================== Fee Operations ====================

    /**
     * Mark fee as paid for a student for a specific month.
     * @param studentId The student's ID
     * @param monthKey Format: "MM-YYYY"
     */
    fun markFeePaid(studentId: String, monthKey: String) {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                val updatedFeesPaid = student.feesPaid.toMutableMap()
                updatedFeesPaid[monthKey] = FeePayment(status = "PAID", paidAt = Date())
                student.copy(feesPaid = updatedFeesPaid)
            } else {
                student
            }
        }
    }

    /**
     * Mark fee as unpaid (remove payment record) for a student for a specific month.
     * @param studentId The student's ID
     * @param monthKey Format: "MM-YYYY"
     */
    fun markFeeUnpaid(studentId: String, monthKey: String) {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                val updatedFeesPaid = student.feesPaid.toMutableMap()
                updatedFeesPaid.remove(monthKey)
                student.copy(feesPaid = updatedFeesPaid)
            } else {
                student
            }
        }
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _error.value = null
    }
}
