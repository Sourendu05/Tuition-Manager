package com.example.tuitionmanager.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tuitionmanager.model.data.Batch
import com.example.tuitionmanager.model.data.Schedule
import com.example.tuitionmanager.viewmodel.TuitionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBatchScreen(
    viewModel: TuitionViewModel,
    onNavigateBack: () -> Unit
) {
    var batchName by remember { mutableStateOf("") }
    var standard by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("") }
    val scheduleList = remember { mutableStateListOf<Schedule>() }
    
    var showScheduleDialog by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var feeError by remember { mutableStateOf<String?>(null) }
    var scheduleError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create New Batch",
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
            // Batch Name
            OutlinedTextField(
                value = batchName,
                onValueChange = {
                    batchName = it
                    nameError = null
                },
                label = { Text("Batch Name *") },
                placeholder = { Text("e.g., Class 10 Physics") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Standard (Optional)
            OutlinedTextField(
                value = standard,
                onValueChange = { standard = it },
                label = { Text("Standard (Optional)") },
                placeholder = { Text("e.g., 10th, 12th") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Fee Amount
            OutlinedTextField(
                value = feeAmount,
                onValueChange = {
                    if (it.all { char -> char.isDigit() || char == '.' }) {
                        feeAmount = it
                        feeError = null
                    }
                },
                label = { Text("Monthly Fee *") },
                placeholder = { Text("e.g., 1500") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                isError = feeError != null,
                supportingText = feeError?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Schedule Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (scheduleError != null) 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Class Schedule *",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Add at least one class day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { 
                                showScheduleDialog = true
                                scheduleError = null
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Day")
                        }
                    }

                    if (scheduleError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scheduleError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scheduleList.isEmpty()) {
                        Text(
                            text = "No schedule added yet. Tap 'Add Day' to add class timings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scheduleList.forEachIndexed { index, schedule ->
                                ScheduleItem(
                                    schedule = schedule,
                                    onRemove = { scheduleList.removeAt(index) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    // Validate
                    var isValid = true
                    if (batchName.isBlank()) {
                        nameError = "Batch name is required"
                        isValid = false
                    }
                    if (feeAmount.isBlank()) {
                        feeError = "Fee amount is required"
                        isValid = false
                    } else {
                        val fee = feeAmount.toDoubleOrNull()
                        if (fee == null || fee <= 0) {
                            feeError = "Enter a valid fee amount"
                            isValid = false
                        }
                    }
                    if (scheduleList.isEmpty()) {
                        scheduleError = "Add at least one class schedule"
                        isValid = false
                    }

                    if (isValid) {
                        viewModel.addBatch(
                            name = batchName.trim(),
                            standard = standard.trim().takeIf { it.isNotBlank() },
                            feeAmount = feeAmount.toDouble(),
                            schedule = scheduleList.toList()
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
                    text = "Create Batch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { schedule ->
                scheduleList.add(schedule)
                showScheduleDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleItem(
    schedule: Schedule,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = schedule.day?.let { Schedule.getDayName(it) } ?: "?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = schedule.day?.let { Schedule.getFullDayName(it) } ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = schedule.time ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (Schedule) -> Unit
) {
    var selectedDay by remember { mutableStateOf(1) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf("05:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Class Schedule",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Day Selection
                Text(
                    text = "Select Day",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(Schedule.getDayName(day)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Time Selection
                Text(
                    text = "Class Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedTime,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Tap to change time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Schedule(
                            day = selectedDay,
                            time = selectedTime
                        )
                    )
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                selectedTime = time
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parsed = Schedule.parseTime12Hour(initialTime)
    val initialHour = parsed?.first ?: 17
    val initialMinute = parsed?.second ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false // Use 12-hour format
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedTime = Schedule.formatTime12Hour(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onConfirm(formattedTime)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Reusable Batch Form component for both Add and Edit screens.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchFormContent(
    batchName: String,
    onBatchNameChange: (String) -> Unit,
    standard: String,
    onStandardChange: (String) -> Unit,
    feeAmount: String,
    onFeeAmountChange: (String) -> Unit,
    scheduleList: List<Schedule>,
    onAddSchedule: (Schedule) -> Unit,
    onRemoveSchedule: (Int) -> Unit,
    nameError: String?,
    feeError: String?,
    scheduleError: String?,
    buttonText: String,
    onSubmit: () -> Unit
) {
    var showScheduleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Batch Name
        OutlinedTextField(
            value = batchName,
            onValueChange = onBatchNameChange,
            label = { Text("Batch Name *") },
            placeholder = { Text("e.g., Class 10 Physics") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp)
        )

        // Standard (Optional)
        OutlinedTextField(
            value = standard,
            onValueChange = onStandardChange,
            label = { Text("Standard (Optional)") },
            placeholder = { Text("e.g., 10th, 12th") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Fee Amount
        OutlinedTextField(
            value = feeAmount,
            onValueChange = { newValue ->
                if (newValue.all { char -> char.isDigit() || char == '.' }) {
                    onFeeAmountChange(newValue)
                }
            },
            label = { Text("Monthly Fee *") },
            placeholder = { Text("e.g., 1500") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
            isError = feeError != null,
            supportingText = feeError?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp)
        )

        // Schedule Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (scheduleError != null) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Class Schedule *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add at least one class day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showScheduleDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Day")
                    }
                }

                if (scheduleError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scheduleError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (scheduleList.isEmpty()) {
                    Text(
                        text = "No schedule added yet. Tap 'Add Day' to add class timings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scheduleList.forEachIndexed { index, schedule ->
                            ScheduleItem(
                                schedule = schedule,
                                onRemove = { onRemoveSchedule(index) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { schedule ->
                onAddSchedule(schedule)
                showScheduleDialog = false
            }
        )
    }
}
