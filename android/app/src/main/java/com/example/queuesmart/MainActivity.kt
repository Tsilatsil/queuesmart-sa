package com.example.queuesmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

// Decides which screen to show: login if nobody is logged in, branches if someone is
@Composable
fun App(modifier: Modifier = Modifier) {
    var session by remember { mutableStateOf<AuthResponse?>(null) }
    val current = session

    if (current == null) {
        AuthScreen(modifier, onAuthenticated = { session = it })
    } else {
        BranchesScreen(
            modifier,
            userName = current.user.name,
            onLogout = { session = null }
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
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(branch.name, style = MaterialTheme.typography.titleMedium)
                            Text("${branch.city}, ${branch.province}")
                            Text(branch.address)
                        }
                    }
                }
            }
        }
    }
}