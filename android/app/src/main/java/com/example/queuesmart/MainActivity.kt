package com.example.queuesmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.queuesmart.ui.theme.QueueSmartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QueueSmartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BranchesScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BranchesScreen(modifier: Modifier = Modifier) {
    // These three variables hold what the screen is showing right now
    var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Runs once when the screen opens: ask the API for the branches
    LaunchedEffect(Unit) {
        try {
            branches = ApiClient.api.getBranches()
        } catch (e: Exception) {
            error = e.message ?: "Something went wrong"
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
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