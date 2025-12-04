package com.example.tuitionmanager.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tuitionmanager.viewmodel.TuitionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    studentId: String,
    viewModel: TuitionViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val student = students.find { it.id == studentId }
    
    var studentName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedBatchId by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf(Date()) }
    
    var showBatchDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var batchError by remember { mutableStateOf<String?>(null) }

    // Pre-fill data from existing student
    LaunchedEffect(student) {
        student?.let {
            studentName = it.name
            phone = it.phone
            selectedBatchId = it.batchId
            joiningDate = it.joiningDate
        }
    }

    if (student == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Student not found")
        }
        return
    }

    val selectedBatch = batches.find { it.id == selectedBatchId }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Get the minimum date (batch creation date or earliest possible)
    val minDateMillis = selectedBatch?.creationDate?.time ?: 0L
    val maxDateMillis = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Student",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Student Name
            OutlinedTextField(
                value = studentName,
                onValueChange = {
                    studentName = it
                    nameError = null
                },
                label = { Text("Student Name *") },
                placeholder = { Text("Enter student's full name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                },
                label = { Text("Phone Number *") },
                placeholder = { Text("e.g., +91 98765 43210") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )
                },
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Batch Selection
            Column {
                Text(
                    text = "Select Batch *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBatchDropdown = true },
                        colors = CardDefaults.cardColors(
                            containerColor = if (batchError != null) 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                if (selectedBatch != null) {
                                    Column {
                                        Text(
                                            text = selectedBatch.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = selectedBatch.getScheduleSummary(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Choose a batch",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showBatchDropdown,
                        onDismissRequest = { showBatchDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        batches.forEach { batch ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = batch.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = batch.getScheduleSummary() + " • ₹${batch.feeAmount.toInt()}/month",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedBatchId = batch.id
                                    batchError = null
                                    // Adjust joining date if needed
                                    if (joiningDate.before(batch.creationDate)) {
                                        joiningDate = batch.creationDate
                                    }
                                    showBatchDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
                
                if (batchError != null) {
                    Text(
                        text = batchError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Joining Date
            Column {
                Text(
                    text = "Joining Date",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (selectedBatch != null) {
                                showDatePicker = true 
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = dateFormat.format(joiningDate),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (selectedBatch != null) {
                    Text(
                        text = "Date must be between batch creation (${dateFormat.format(selectedBatch.creationDate)}) and today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    // Validate
                    var isValid = true
                    if (studentName.isBlank()) {
                        nameError = "Student name is required"
                        isValid = false
                    }
                    if (phone.isBlank()) {
                        phoneError = "Phone number is required"
                        isValid = false
                    }
                    if (selectedBatchId.isBlank()) {
                        batchError = "Please select a batch"
                        isValid = false
                    }

                    if (isValid) {
                        viewModel.updateStudent(
                            studentId = studentId,
                            name = studentName.trim(),
                            phone = phone.trim(),
                            batchId = selectedBatchId,
                            joiningDate = joiningDate
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Update Student",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Date Picker Dialog with constraints
    if (showDatePicker && selectedBatch != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = joiningDate.time,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= minDateMillis && utcTimeMillis <= maxDateMillis
                }
                
                override fun isSelectableYear(year: Int): Boolean {
                    return true
                }
            }
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            joiningDate = Date(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Joining Date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }
}

