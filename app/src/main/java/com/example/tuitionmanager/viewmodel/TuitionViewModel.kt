package com.example.tuitionmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tuitionmanager.model.Repo
import com.example.tuitionmanager.model.data.Batch
import com.example.tuitionmanager.model.data.Schedule
import com.example.tuitionmanager.model.data.Student
import com.example.tuitionmanager.model.data.Teacher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the Tuition Manager app.
 * Acts as the bridge between UI (Screens) and Data (Repository).
 */
@HiltViewModel
class TuitionViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {

    // ==================== Exposed State Flows ====================

    // Current teacher
    val currentTeacher: StateFlow<Teacher?> = repo.currentTeacher

    // All batches
    val batches: StateFlow<List<Batch>> = repo.batches

    // All students
    val students: StateFlow<List<Student>> = repo.students

    // Loading state
    val isLoading: StateFlow<Boolean> = repo.isLoading

    // Error state
    val error: StateFlow<String?> = repo.error

    // Currently selected month for fee management (format: "MM-YYYY")
    private val _selectedMonthKey = MutableStateFlow(Student.generateMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    // ==================== Dashboard State ====================

    /**
     * Get batches scheduled for today.
     */
    val todaysBatches: StateFlow<List<Batch>> = batches
        .combine(MutableStateFlow(Unit)) { batchList, _ ->
            val todayDayOfWeek = getTodayDayOfWeek()
            batchList
                .filter { it.hasClassOnDay(todayDayOfWeek) }
                .sortedBy { batch ->
                    val scheduleForToday = batch.getScheduleForDay(todayDayOfWeek).firstOrNull()
                    scheduleForToday?.let { Schedule.getSortableTimeKey(it.time) } ?: Int.MAX_VALUE
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Get the day of week (1=Monday, 7=Sunday) for today.
     */
    private fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert from Calendar's Sunday=1 to our Monday=1 format
        return when (day) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    // ==================== Batch Operations ====================

    /**
     * Get a batch by ID.
     */
    fun getBatchById(batchId: String): Batch? {
        return repo.getBatchById(batchId)
    }

    /**
     * Add a new batch.
     */
    fun addBatch(
        name: String,
        standard: String?,
        feeAmount: Double,
        schedule: List<Schedule>
    ) {
        val batch = Batch(
            name = name,
            standard = standard?.takeIf { it.isNotBlank() },
            feeAmount = feeAmount,
            schedule = schedule,
            creationDate = Date()
        )
        repo.addBatch(batch)
    }

    /**
     * Update an existing batch (preserving creation date).
     */
    fun updateBatch(
        batchId: String,
        name: String,
        standard: String?,
        feeAmount: Double,
        schedule: List<Schedule>
    ) {
        val existingBatch = repo.getBatchById(batchId) ?: return
        val updatedBatch = existingBatch.copy(
            name = name,
            standard = standard?.takeIf { it.isNotBlank() },
            feeAmount = feeAmount,
            schedule = schedule
        )
        repo.updateBatch(updatedBatch)
    }

    /**
     * Delete a batch.
     */
    fun deleteBatch(batchId: String) {
        repo.deleteBatch(batchId)
    }

    /**
     * Get student count for a batch.
     */
    fun getStudentCountInBatch(batchId: String): Int {
        return repo.getStudentCountInBatch(batchId)
    }

    // ==================== Student Operations ====================

    /**
     * Get a student by ID.
     */
    fun getStudentById(studentId: String): Student? {
        return repo.getStudentById(studentId)
    }

    /**
     * Get all students in a batch.
     */
    fun getStudentsInBatch(batchId: String): List<Student> {
        return repo.getStudentsInBatch(batchId)
    }

    /**
     * Get students in a batch who had joined by a specific month.
     * Used for fee management - only show students who existed in that month.
     * @param batchId The batch ID
     * @param monthKey Format: "MM-yyyy"
     */
    fun getStudentsForFeeMonth(batchId: String, monthKey: String): List<Student> {
        val allStudents = repo.getStudentsInBatch(batchId)
        return allStudents.filter { student ->
            // Check if student had joined by the end of the selected month
            val parsed = Student.parseMonthKey(monthKey) ?: return@filter true
            val (month, year) = parsed
            
            // Create a date representing the end of the selected month
            val monthEndCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1) // Calendar months are 0-indexed
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            
            // Student should have joined before or during this month
            !student.joiningDate.after(monthEndCal.time)
        }
    }

    /**
     * Add a new student.
     */
    fun addStudent(
        name: String,
        phone: String,
        batchId: String,
        joiningDate: Date = Date()
    ) {
        val student = Student(
            name = name,
            phone = phone,
            batchId = batchId,
            joiningDate = joiningDate
        )
        repo.addStudent(student)
    }

    /**
     * Update an existing student.
     */
    fun updateStudent(
        studentId: String,
        name: String,
        phone: String,
        batchId: String,
        joiningDate: Date
    ) {
        val existingStudent = repo.getStudentById(studentId) ?: return
        val updatedStudent = existingStudent.copy(
            name = name,
            phone = phone,
            batchId = batchId,
            joiningDate = joiningDate
        )
        repo.updateStudent(updatedStudent)
    }

    /**
     * Delete a student.
     */
    fun deleteStudent(studentId: String) {
        repo.deleteStudent(studentId)
    }

    // ==================== Fee Operations ====================

    /**
     * Set the selected month for fee management.
     */
    fun setSelectedMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    /**
     * Check if a month is within valid range (batch creation to current month).
     */
    fun isMonthInValidRange(monthKey: String, batchCreationDate: Date): Boolean {
        val parsed = Student.parseMonthKey(monthKey) ?: return false
        val currentMonthParsed = Student.parseMonthKey(Student.generateMonthKey()) ?: return false
        val creationMonthKey = run {
            val cal = Calendar.getInstance()
            cal.time = batchCreationDate
            Student.generateMonthKey(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        }
        val creationParsed = Student.parseMonthKey(creationMonthKey) ?: return false

        // Check if month is not in the future
        val monthValue = parsed.second * 12 + parsed.first
        val currentMonthValue = currentMonthParsed.second * 12 + currentMonthParsed.first
        val creationMonthValue = creationParsed.second * 12 + creationParsed.first

        return monthValue in creationMonthValue..currentMonthValue
    }

    /**
     * Move to the previous month if valid.
     */
    fun goToPreviousMonth(batchCreationDate: Date): Boolean {
        val parsed = Student.parseMonthKey(_selectedMonthKey.value) ?: return false
        var (month, year) = parsed
        month -= 1
        if (month < 1) {
            month = 12
            year -= 1
        }
        val newMonthKey = Student.generateMonthKey(month, year)
        
        if (isMonthInValidRange(newMonthKey, batchCreationDate)) {
            _selectedMonthKey.value = newMonthKey
            return true
        }
        return false
    }

    /**
     * Move to the next month if valid (not beyond current month).
     */
    fun goToNextMonth(): Boolean {
        val parsed = Student.parseMonthKey(_selectedMonthKey.value) ?: return false
        val currentMonthParsed = Student.parseMonthKey(Student.generateMonthKey()) ?: return false
        
        var (month, year) = parsed
        month += 1
        if (month > 12) {
            month = 1
            year += 1
        }
        
        val newMonthValue = year * 12 + month
        val currentMonthValue = currentMonthParsed.second * 12 + currentMonthParsed.first
        
        if (newMonthValue <= currentMonthValue) {
            _selectedMonthKey.value = Student.generateMonthKey(month, year)
            return true
        }
        return false
    }

    /**
     * Check if we can go to next month.
     */
    fun canGoToNextMonth(): Boolean {
        val parsed = Student.parseMonthKey(_selectedMonthKey.value) ?: return false
        val currentMonthParsed = Student.parseMonthKey(Student.generateMonthKey()) ?: return false
        
        var (month, year) = parsed
        month += 1
        if (month > 12) {
            month = 1
            year += 1
        }
        
        val newMonthValue = year * 12 + month
        val currentMonthValue = currentMonthParsed.second * 12 + currentMonthParsed.first
        
        return newMonthValue <= currentMonthValue
    }

    /**
     * Check if we can go to previous month.
     */
    fun canGoToPreviousMonth(batchCreationDate: Date): Boolean {
        val parsed = Student.parseMonthKey(_selectedMonthKey.value) ?: return false
        var (month, year) = parsed
        month -= 1
        if (month < 1) {
            month = 12
            year -= 1
        }
        val newMonthKey = Student.generateMonthKey(month, year)
        return isMonthInValidRange(newMonthKey, batchCreationDate)
    }

    /**
     * Toggle fee status for a student for the selected month.
     */
    fun toggleFeeStatus(studentId: String, monthKey: String = _selectedMonthKey.value) {
        val student = repo.getStudentById(studentId) ?: return
        if (student.isFeePaidFor(monthKey)) {
            repo.markFeeUnpaid(studentId, monthKey)
        } else {
            repo.markFeePaid(studentId, monthKey)
        }
    }

    /**
     * Mark fee as paid for a student.
     */
    fun markFeePaid(studentId: String, monthKey: String = _selectedMonthKey.value) {
        repo.markFeePaid(studentId, monthKey)
    }

    /**
     * Mark fee as unpaid for a student.
     */
    fun markFeeUnpaid(studentId: String, monthKey: String = _selectedMonthKey.value) {
        repo.markFeeUnpaid(studentId, monthKey)
    }

    /**
     * Reset selected month to current month.
     */
    fun resetToCurrentMonth() {
        _selectedMonthKey.value = Student.generateMonthKey()
    }

    // ==================== Utility ====================

    /**
     * Clear any error state.
     */
    fun clearError() {
        repo.clearError()
    }

    /**
     * Get the batch name for a student.
     */
    fun getBatchNameForStudent(student: Student): String {
        return repo.getBatchById(student.batchId)?.name ?: "Unknown Batch"
    }

    /**
     * Generate list of valid months for fee selection (from batch creation to current month).
     */
    fun getValidMonthsForBatch(batchCreationDate: Date): List<String> {
        val months = mutableListOf<String>()
        val currentCal = Calendar.getInstance()
        val creationCal = Calendar.getInstance().apply { time = batchCreationDate }
        
        // Start from creation month
        val cal = Calendar.getInstance().apply { time = batchCreationDate }
        
        while (cal.time <= currentCal.time || 
               (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && 
                cal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH))) {
            months.add(Student.generateMonthKey(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)))
            cal.add(Calendar.MONTH, 1)
            
            // Safety check to avoid infinite loop
            if (months.size > 120) break // Max 10 years
        }
        
        return months
    }
}
