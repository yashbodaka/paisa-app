package com.paisa.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.TransactionType
import com.paisa.app.domain.PersonBalance
import com.paisa.app.domain.formatInr
import com.paisa.app.domain.formatSignedInr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class PaisaTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    People("People", Icons.Default.Person),
    History("History", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisaScreen(
    state: PaisaUiState,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDelete: (MoneyTransaction) -> Unit,
    onVoiceClick: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(PaisaTab.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Paisa", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Log money in seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                PaisaTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            PaisaTab.Home -> HomeContent(
                modifier = Modifier.padding(padding),
                state = state,
                onDraftChange = onDraftChange,
                onSubmit = onSubmit,
                onSuggestionClick = onSuggestionClick,
                onDelete = onDelete,
                onVoiceClick = onVoiceClick
            )

            PaisaTab.People -> PeopleContent(
                modifier = Modifier.padding(padding),
                people = state.people
            )

            PaisaTab.History -> HistoryContent(
                modifier = Modifier.padding(padding),
                transactions = state.transactions,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    state: PaisaUiState,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDelete: (MoneyTransaction) -> Unit,
    onVoiceClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            QuickEntry(
                draft = state.draft,
                onDraftChange = onDraftChange,
                onSubmit = onSubmit,
                onVoiceClick = onVoiceClick
            )
        }

        if (state.suggestions.isNotEmpty()) {
            item {
                SuggestionRow(
                    suggestions = state.suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }

        item {
            Dashboard(summary = state.summary)
        }

        item {
            SectionHeader(title = "Recent", trailing = "${state.transactions.size} entries")
        }

        if (state.transactions.isEmpty()) {
            item {
                EmptyState("Try `200 food` or tap the mic.")
            }
        } else {
            items(state.transactions.take(8), key = { it.id }) { transaction ->
                TransactionRow(transaction = transaction, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun QuickEntry(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text("200 food, 300 to Rahul, +2000 freelance") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon = {
                Row {
                    IconButton(onClick = onVoiceClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Speak entry")
                    }
                    IconButton(onClick = onSubmit) {
                        Icon(Icons.Default.Send, contentDescription = "Log entry")
                    }
                }
            }
        )
        Text(
            text = "No forms. Start with amount, add a word or person.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuggestionRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(suggestions) { suggestion ->
            AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun Dashboard(summary: com.paisa.app.domain.MoneySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Dashboard", trailing = summary.topCategory.takeIf { it != "none" } ?: "No spending yet")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Today",
                value = summary.todaySpendingPaise.formatInr()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "This week",
                value = summary.weekSpendingPaise.formatInr()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Income",
                value = summary.monthIncomePaise.formatInr()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Expense",
                value = summary.monthExpensePaise.formatInr()
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PeopleContent(
    modifier: Modifier,
    people: List<PersonBalance>
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(title = "People", trailing = "${people.size} active")
        }
        if (people.isEmpty()) {
            item {
                EmptyState("Entries like `500 to Rahul` appear here.")
            }
        } else {
            items(people, key = { it.name }) { person ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(person.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                person.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = person.netPaise.formatSignedInr(),
                            fontWeight = FontWeight.Bold,
                            color = if (person.netPaise >= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(
    modifier: Modifier,
    transactions: List<MoneyTransaction>,
    onDelete: (MoneyTransaction) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(title = "History", trailing = "${transactions.size} entries")
        }
        if (transactions.isEmpty()) {
            item {
                EmptyState("Your logged money activity will appear here.")
            }
        } else {
            items(transactions, key = { it.id }) { transaction ->
                TransactionRow(transaction = transaction, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: MoneyTransaction,
    onDelete: (MoneyTransaction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = when (transaction.type) {
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.LENT -> MaterialTheme.colorScheme.tertiary
                        TransactionType.BORROWED -> MaterialTheme.colorScheme.secondary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = transaction.rawText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = transaction.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.signedAmount(),
                    fontWeight = FontWeight.Bold,
                    color = transaction.amountColor()
                )
                IconButton(onClick = { onDelete(transaction) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            trailing,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun MoneyTransaction.subtitle(): String {
    val time = DateTimeFormatter.ofPattern("dd MMM, h:mm a")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(createdAt))
    val target = personName?.let { " · $it" }.orEmpty()
    return "${type.name.lowercase()} · $category$target · $time"
}

@Composable
private fun MoneyTransaction.signedAmount(): String {
    return when (type) {
        TransactionType.INCOME -> amountPaise.formatSignedInr()
        TransactionType.BORROWED -> amountPaise.formatSignedInr()
        TransactionType.EXPENSE -> "-" + amountPaise.formatInr()
        TransactionType.LENT -> "-" + amountPaise.formatInr()
    }
}

@Composable
private fun MoneyTransaction.amountColor() = when (type) {
    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
    TransactionType.BORROWED -> MaterialTheme.colorScheme.secondary
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    TransactionType.LENT -> MaterialTheme.colorScheme.tertiary
}

