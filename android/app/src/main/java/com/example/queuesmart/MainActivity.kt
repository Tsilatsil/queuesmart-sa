package com.example.queuesmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.queuesmart.ui.theme.QueueSmartTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QueueSmartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// South Africa's time zone. The backend works in this zone too.
private val SA_ZONE: ZoneId = ZoneId.of("Africa/Johannesburg")
private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

// 480 minutes after midnight -> "08:00"
fun formatMinutes(total: Int): String = "%02d:%02d".format(total / 60, total % 60)

// The next 14 weekdays, starting today (branches are closed on weekends)
fun upcomingWeekdays(count: Int = 14): List<LocalDate> {
    val today = LocalDate.now(SA_ZONE)
    return generateSequence(today) { it.plusDays(1) }
        .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
        .take(count)
        .toList()
}

// Decides which screen to show, based on what the person has chosen so far
@Composable
fun App(modifier: Modifier = Modifier) {
    var session by remember { mutableStateOf<AuthResponse?>(null) }
    var selectedBranch by remember { mutableStateOf<Branch?>(null) }
    var selectedService by remember { mutableStateOf<ServiceType?>(null) }
    var booking by remember { mutableStateOf(false) }

    val current = session
    val branch = selectedBranch
    val service = selectedService

    // Makes the phone's own Back button step back one screen
    BackHandler(enabled = branch != null) {
        when {
            booking -> booking = false
            service != null -> selectedService = null
            else -> selectedBranch = null
        }
    }

    when {
        current == null -> AuthScreen(
            modifier,
            onAuthenticated = { session = it }
        )

        branch == null -> BranchesScreen(
            modifier,
            userName = current.user.name,
            onBranchClick = { selectedBranch = it },
            onLogout = {
                session = null
                selectedBranch = null
                selectedService = null
                booking = false
            }
        )

        service == null -> ServicesScreen(
            modifier,
            branch = branch,
            onBack = { selectedBranch = null },
            onServiceClick = { selectedService = it }
        )

        !booking -> ServiceDetailScreen(
            modifier,
            branch = branch,
            service = service,
            onBack = { selectedService = null },
            onBook = { booking = true }
        )

        else -> BookingScreen(
            modifier,
            token = current.token,
            branch = branch,
            service = service,
            onBack = { booking = false },
            onDone = {
                booking = false
                selectedService = null
                selectedBranch = null
            }
        )
    }
}

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    onAuthenticated: (AuthResponse) -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("QueueSmart SA", style = MaterialTheme.typography.headlineMedium)
        Text(if (isRegister) "Create your account" else "Log in to book an appointment")

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (at least 8 characters)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                error = null
                loading = true
                scope.launch {
                    try {
                        val result = if (isRegister) {
                            ApiClient.api.register(
                                RegisterRequest(name.trim(), email.trim(), password)
                            )
                        } else {
                            ApiClient.api.login(LoginRequest(email.trim(), password))
                        }
                        onAuthenticated(result)
                    } catch (e: Exception) {
                        error = errorMessage(e)
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (loading) "Please wait..."
                else if (isRegister) "Sign up"
                else "Log in"
            )
        }

        TextButton(onClick = { isRegister = !isRegister; error = null }) {
            Text(
                if (isRegister) "Already have an account? Log in"
                else "New here? Create an account"
            )
        }
    }
}

@Composable
fun BranchesScreen(
    modifier: Modifier = Modifier,
    userName: String,
    onBranchClick: (Branch) -> Unit,
    onLogout: () -> Unit
) {
    var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            branches = ApiClient.api.getBranches()
        } catch (e: Exception) {
            error = errorMessage(e)
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hi, $userName", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onLogout) { Text("Log out") }
        }

        Text("Choose a branch", style = MaterialTheme.typography.headlineSmall)

        when {
            loading -> CircularProgressIndicator(Modifier.padding(top = 16.dp))
            error != null -> Text("Error: $error", Modifier.padding(top = 16.dp))
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                items(branches) { branch ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onBranchClick(branch) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(branch.name, style = MaterialTheme.typography.titleMedium)
                            Text("${branch.city}, ${branch.province}")
                            Text(branch.address)
                            Text(
                                "Open ${formatMinutes(branch.opensAtMin)} to ${formatMinutes(branch.closesAtMin)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServicesScreen(
    modifier: Modifier = Modifier,
    branch: Branch,
    onBack: () -> Unit,
    onServiceClick: (ServiceType) -> Unit
) {
    var services by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            services = ApiClient.api.getServices()
        } catch (e: Exception) {
            error = errorMessage(e)
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back to branches") }

        Text(branch.name, style = MaterialTheme.typography.headlineSmall)
        Text("Open ${formatMinutes(branch.opensAtMin)} to ${formatMinutes(branch.closesAtMin)}")

        Text(
            "Choose a service",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        when {
            loading -> CircularProgressIndicator(Modifier.padding(top = 16.dp))
            error != null -> Text("Error: $error", Modifier.padding(top = 16.dp))
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(services) { service ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onServiceClick(service) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(service.name, style = MaterialTheme.typography.titleMedium)
                            service.description?.let { Text(it) }
                            Text(
                                "About ${service.durationMin} minutes",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceDetailScreen(
    modifier: Modifier = Modifier,
    branch: Branch,
    service: ServiceType,
    onBack: () -> Unit,
    onBook: () -> Unit
) {
    // Remembers which documents have been ticked on this screen
    val ticked = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back to services") }

        Text(service.name, style = MaterialTheme.typography.headlineSmall)
        Text(branch.name, style = MaterialTheme.typography.bodyMedium)
        service.description?.let { Text(it, Modifier.padding(top = 8.dp)) }
        Text(
            "About ${service.durationMin} minutes",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            "Documents to bring",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        service.requiredDocuments.forEach { doc ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = ticked[doc.id] == true,
                    onCheckedChange = { ticked[doc.id] = it }
                )
                Column {
                    Text(doc.name)
                    doc.note?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        val total = service.requiredDocuments.size
        val done = ticked.values.count { it }
        Text(
            "$done of $total ready",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = onBook,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("Book an appointment")
        }
    }
}

@Composable
fun BookingScreen(
    modifier: Modifier = Modifier,
    token: String,
    branch: Branch,
    service: ServiceType,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val dates = remember { upcomingWeekdays() }
    var selectedDate by remember { mutableStateOf(dates.first()) }
    var slotsResponse by remember { mutableStateOf<SlotsResponse?>(null) }
    var loadingSlots by remember { mutableStateOf(true) }
    var slotsError by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var booking by remember { mutableStateOf(false) }
    var bookingError by remember { mutableStateOf<String?>(null) }
    var booked by remember { mutableStateOf<BookedAppointment?>(null) }
    val scope = rememberCoroutineScope()

    // Loads the time slots again whenever the day changes (or after a failed booking)
    LaunchedEffect(selectedDate, refresh) {
        loadingSlots = true
        slotsError = null
        slotsResponse = null
        try {
            slotsResponse = ApiClient.api.getSlots(branch.id, selectedDate.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            slotsError = errorMessage(e)
        } finally {
            loadingSlots = false
        }
    }

    val confirmed = booked
    if (confirmed != null) {
        BookingConfirmed(modifier, confirmed, service, onDone)
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("← Back") }

            Text("Book an appointment", style = MaterialTheme.typography.headlineSmall)
            Text("${service.name} at ${branch.name}")

            Text(
                "Choose a day",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { date ->
                    val label = date.format(DAY_FORMAT)
                    if (date == selectedDate) {
                        Button(onClick = {}) { Text(label) }
                    } else {
                        OutlinedButton(onClick = {
                            selectedDate = date
                            selectedSlot = null
                            bookingError = null
                        }) { Text(label) }
                    }
                }
            }

            Text(
                "Choose a time",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )

            val response = slotsResponse
            when {
                loadingSlots -> CircularProgressIndicator()
                slotsError != null -> Text("Error: $slotsError")
                response == null -> {}
                response.closed -> Text(response.message ?: "This branch is closed.")
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    response.slots.chunked(3).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                SlotButton(
                                    slot = slot,
                                    selected = selectedSlot?.startsAt == slot.startsAt,
                                    onClick = { selectedSlot = slot },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Keeps the last row's buttons the same width as the others
                            repeat(3 - rowSlots.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            bookingError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            val chosen = selectedSlot
            Button(
                onClick = {
                    if (chosen != null) {
                        bookingError = null
                        booking = true
                        scope.launch {
                            try {
                                val result = ApiClient.api.book(
                                    "Bearer $token",
                                    BookingRequest(branch.id, service.id, chosen.startsAt)
                                )
                                booked = result.appointment
                            } catch (e: Exception) {
                                bookingError = errorMessage(e)
                                selectedSlot = null
                                refresh++
                            } finally {
                                booking = false
                            }
                        }
                    }
                },
                enabled = chosen != null && !booking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    when {
                        booking -> "Booking..."
                        chosen != null -> "Confirm ${chosen.time} on ${selectedDate.format(DAY_FORMAT)}"
                        else -> "Choose a time"
                    }
                )
            }
        }
    }
}

@Composable
fun SlotButton(
    slot: Slot,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    if (selected) {
        Button(
            onClick = onClick,
            enabled = slot.available,
            contentPadding = padding,
            modifier = modifier
        ) { SlotLabel(slot) }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = slot.available,
            contentPadding = padding,
            modifier = modifier
        ) { SlotLabel(slot) }
    }
}

@Composable
fun SlotLabel(slot: Slot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(slot.time)
        Text(
            when {
                slot.remaining == 0 -> "Full"
                !slot.available -> "Passed"
                else -> "${slot.remaining} left"
            },
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun BookingConfirmed(
    modifier: Modifier = Modifier,
    appointment: BookedAppointment,
    service: ServiceType,
    onDone: () -> Unit
) {
    val whenText = ZonedDateTime
        .ofInstant(Instant.parse(appointment.startsAt), SA_ZONE)
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Booking confirmed", style = MaterialTheme.typography.headlineSmall)

        Card(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Your reference", style = MaterialTheme.typography.labelMedium)
                Text(appointment.reference, style = MaterialTheme.typography.headlineMedium)
                Text(appointment.service, Modifier.padding(top = 12.dp))
                Text(appointment.branch)
                Text(whenText)
            }
        }

        Text(
            "Documents to bring",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        service.requiredDocuments.forEach { doc ->
            Text("• ${doc.name}" + (doc.note?.let { " ($it)" } ?: ""))
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("Done")
        }
    }
}