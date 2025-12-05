package com.example.tuitionmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tuitionmanager.model.Repo
import com.example.tuitionmanager.model.ResultState
import com.example.tuitionmanager.model.data.Batch
import com.example.tuitionmanager.model.data.Schedule
import com.example.tuitionmanager.model.data.Student
import com.example.tuitionmanager.model.data.Teacher
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the Tuition Manager app.
 * Acts as the bridge between UI (Screens) and Data (Repository).
 * Handles Loading/Error states and business logic.
 */
@HiltViewModel
class TuitionViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {

    // ==================== UI State ====================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ==================== Teacher State ====================
    
    private val _currentTeacher = MutableStateFlow<Teacher?>(null)
    val currentTeacher: StateFlow<Teacher?> = _currentTeacher.asStateFlow()

    // ==================== Batches State ====================
    
    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

    // ==================== Students State ====================
    
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    // ==================== Fee Management State ====================
    
    // Currently selected month for fee management (format: "MM-yyyy")
    private val _selectedMonthKey = MutableStateFlow(Student.generateMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    // ==================== Dashboard State ====================

    /**
     * Get batches scheduled for today.
     * Derived from batches flow, filtered by current day of week.
     */
    val todaysBatches: StateFlow<List<Batch>> = _batches
        .map { batchList ->
            val todayDayOfWeek = getTodayDayOfWeek()
            batchList
                .filter { it.hasClassOnDay(todayDayOfWeek) }
                .sortedBy { batch ->
                    val scheduleForToday = batch.getScheduleForDay(todayDayOfWeek).firstOrNull()
                    scheduleForToday?.time?.let { Schedule.getSortableTimeKey(it) } ?: Int.MAX_VALUE
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== Initialization ====================

    init {
        loadTeacher()
        loadBatches()
        loadStudents()
    }

    private fun loadTeacher() {
        viewModelScope.launch {
            repo.getCurrentTeacher().collect { result ->
                when (result) {
                    is ResultState.Loading -> { /* Teacher loading handled by isLoading */ }
                    is ResultState.Success -> {
                        _currentTeacher.value = result.data
                    }
                    is ResultState.Error -> {
                        _error.value = result.error
                    }
                }
            }
        }
    }

    private fun loadBatches() {
        viewModelScope.launch {
            repo.getAllBatches().collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _isLoading.value = true
                    }
                    is ResultState.Success -> {
                        _isLoading.value = false
                        _batches.value = result.data
                    }
                    is ResultState.Error -> {
                        _isLoading.value = false
                        _error.value = result.error
                    }
                }
            }
        }
    }

    private fun loadStudents() {
        viewModelScope.launch {
            repo.getAllStudents().collect { result ->
                when (result) {
                    is ResultState.Loading -> { /* Handled by batches loading */ }
                    is ResultState.Success -> {
                        _students.value = result.data
                    }
                    is ResultState.Error -> {
                        _error.value = result.error
                    }
                }
            }
        }
    }

    // ==================== Day of Week Helper ====================

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
     * Get a batch by ID from cached list.
     */
    fun getBatchById(batchId: String): Batch? {
        return _batches.value.find { it.id == batchId }
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
        viewModelScope.launch {
            _isLoading.value = true
            val batch = Batch(
                name = name,
                standard = standard?.takeIf { it.isNotBlank() },
                feeAmount = feeAmount,
                schedule = schedule
                // creationDate will be set by ServerTimestamp
            )
            when (val result = repo.addBatch(batch)) {
                is ResultState.Success -> {
                    _isLoading.value = false
                    // Data will be updated via the snapshot listener
                }
                is ResultState.Error -> {
                    _isLoading.value = false
                    _error.value = result.error
                }
                is ResultState.Loading -> { /* Already set */ }
            }
        }
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
        viewModelScope.launch {
            _isLoading.value = true
            val existingBatch = getBatchById(batchId)
            if (existingBatch == null) {
                _isLoading.value = false
                _error.value = "Batch not found"
                return@launch
            }
            
            val updatedBatch = existingBatch.copy(
                name = name,
                standard = standard?.takeIf { it.isNotBlank() },
                feeAmount = feeAmount,
                schedule = schedule
            )
            
            when (val result = repo.updateBatch(updatedBatch)) {
                is ResultState.Success -> {
                    _isLoading.value = false
                }
                is ResultState.Error -> {
                    _isLoading.value = false
                    _error.value = result.error
                }
                is ResultState.Loading -> { /* Already set */ }
            }
        }
    }

    /**
     * Delete a batch (cascading delete handled in Repo).
     */
    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repo.deleteBatch(batchId)) {
                is ResultState.Success -> {
                    _isLoading.value = false
                }
                is ResultState.Error -> {
                    _isLoading.value = false
                    _error.value = result.error
                }
                is ResultState.Loading -> { /* Already set */ }
            }
        }
    }

    /**
     * Get student count for a batch from cached list.
     */
    fun getStudentCountInBatch(batchId: String): Int {
        return _students.value.count { it.batchId == batchId }
    }

    // ==================== Student Operations ====================

    /**
     * Get a student by ID from cached list.
     */
    fun getStudentById(studentId: String): Student? {
        return _students.value.find { it.id == studentId }
    }

    /**
     * Get all students in a batch from cached list.
     */
    fun getStudentsInBatch(batchId: String): List<Student> {
        return _students.value.filter { it.batchId == batchId }
    }

    /**
     * Get students in a batch who had joined by a specific month.
     * Used for fee management - only show students who existed in that month.
     * Logic: IF (student.joiningDate > EndOf(SelectedMonth)) THEN HideStudent()
     * 
     * @param batchId The batch ID
     * @param monthKey Format: "MM-yyyy"
     */
    fun getStudentsForFeeMonth(batchId: String, monthKey: String): List<Student> {
        val allStudents = getStudentsInBatch(batchId)
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
            val joiningDate = student.getJoiningDateAsDate()
            !joiningDate.after(monthEndCal.time)
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
        viewModelScope.launch {
            _isLoading.value = true
            val student = Student(
                name = name,
                phone = phone,
                batchId = batchId,
                joiningDate = Timestamp(joiningDate)
            )
            when (val result = repo.addStudent(student)) {
                is ResultState.Success -> {
                    _isLoading.value = false
                }
                is ResultState.Error -> {
                    _isLoading.value = false
                    _error.value = result.error
                }
                is ResultState.Loading -> { /* Already set */ }
            }
        }
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
        viewModelScope.launch {
            _isLoading.value = true
            val existingStudent = getStudentById(studentId)
            if (existingStudent == null) {
                _isLoading.value = false
                _error.value = "Student not found"
                return@launch
            }
            
            val updatedStudent = existingStudent.copy(
                name = name,
                phone = phone,
                batchId = batchId,
                joiningDate = Timestamp(joiningDate)
            )
            
            when (val result = repo.updateStudent(updatedStudent)) {
                is ResultState.Success -> {
                    _isLoading.value = false
                }
                is ResultState.Error -> {
                    _isLoading.value = false
                    _error.value = result.error
                }
                is ResultState.Loading -> { /* Already set */ }
            }
        }
    }

    /**
     * Delete a student.
     */
    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repo.deleteStudent(studentId)) {
                is ResultState.Success -> {
                    _isLoading.value = false
                }
                is ResultState.Error -> {
                    _isLoading.value = false
                    _error.value = result.error
                }
                is ResultState.Loading -> { /* Already set */ }
            }
        }
    }

    // ==================== Fee Operations ====================

    /**
     * Set the selected month for fee management.
     */
    fun setSelectedMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    /**
     * Generate list of valid months for fee selection.
     * Range: from batch.creationDate to Current Month.
     * Constraint: Users cannot select future months.
     */
    fun getValidMonthsForBatch(batchCreationDate: Date): List<String> {
        val months = mutableListOf<String>()
        val currentCal = Calendar.getInstance()
        
        // Start from creation month
        val cal = Calendar.getInstance().apply { time = batchCreationDate }
        
        while (cal.time <= currentCal.time || 
               (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && 
                cal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH))) {
            months.add(Student.generateMonthKey(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)))
            cal.add(Calendar.MONTH, 1)
            
            // Safety check to avoid infinite loop (max 10 years)
            if (months.size > 120) break
        }
        
        return months
    }

    /**
     * Check if a month is within valid range (batch creation to current month).
     */
    fun isMonthInValidRange(monthKey: String, batchCreationDate: Date): Boolean {
        val parsed = Student.parseMonthKey(monthKey) ?: return false
        val currentMonthParsed = Student.parseMonthKey(Student.generateMonthKey()) ?: return false
        
        val creationCal = Calendar.getInstance().apply { time = batchCreationDate }
        val creationMonthKey = Student.generateMonthKey(
            creationCal.get(Calendar.MONTH) + 1, 
            creationCal.get(Calendar.YEAR)
        )
        val creationParsed = Student.parseMonthKey(creationMonthKey) ?: return false

        // Compare as month values (year * 12 + month)
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
     * Toggle ON = Write "PAID" to Firestore.
     * Toggle OFF = Delete Payment Key.
     */
    fun toggleFeeStatus(studentId: String, monthKey: String = _selectedMonthKey.value) {
        viewModelScope.launch {
            val student = getStudentById(studentId)
            if (student == null) {
                _error.value = "Student not found"
                return@launch
            }
            
            val result = if (student.isFeePaidFor(monthKey)) {
                repo.markFeeUnpaid(studentId, monthKey)
            } else {
                repo.markFeePaid(studentId, monthKey)
            }
            
            when (result) {
                is ResultState.Error -> {
                    _error.value = result.error
                }
                else -> { /* Success or Loading - data updated via listener */ }
            }
        }
    }

    /**
     * Mark fee as paid for a student.
     */
    fun markFeePaid(studentId: String, monthKey: String = _selectedMonthKey.value) {
        viewModelScope.launch {
            when (val result = repo.markFeePaid(studentId, monthKey)) {
                is ResultState.Error -> {
                    _error.value = result.error
                }
                else -> { /* Success or Loading */ }
            }
        }
    }

    /**
     * Mark fee as unpaid for a student.
     */
    fun markFeeUnpaid(studentId: String, monthKey: String = _selectedMonthKey.value) {
        viewModelScope.launch {
            when (val result = repo.markFeeUnpaid(studentId, monthKey)) {
                is ResultState.Error -> {
                    _error.value = result.error
                }
                else -> { /* Success or Loading */ }
            }
        }
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
        _error.value = null
    }

    /**
     * Get the batch name for a student.
     */
    fun getBatchNameForStudent(student: Student): String {
        return getBatchById(student.batchId)?.name ?: "Unknown Batch"
    }
}
