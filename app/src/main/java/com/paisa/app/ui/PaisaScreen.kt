package com.paisa.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paisa.app.R
import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.TransactionType
import com.paisa.app.domain.PersonBalance
import com.paisa.app.domain.formatInr
import com.paisa.app.domain.formatSignedInr
import com.paisa.app.ui.theme.bouncyClickable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Random

private enum class PaisaTab(val label: String, val iconRes: Int) {
    Home("Home", R.drawable.ic_home_handdrawn),
    History("History", R.drawable.ic_history_handdrawn),
    Insights("Insights", R.drawable.ic_insights_handdrawn),
    Savings("Savings", R.drawable.ic_savings_handdrawn),
    People("People", R.drawable.ic_people_handdrawn)
}

val HandDrawnShapeChip = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp)

private fun buildWobblyRectPath(
    w: Float,
    h: Float,
    random: Random,
    jitter: Float = 3f,
    segments: Int = 6
): Path {
    return Path().apply {
        val margin = 4f
        val corners = listOf(
            floatArrayOf(margin + random.nextFloat() * jitter, margin + random.nextFloat() * jitter),
            floatArrayOf(w - margin - random.nextFloat() * jitter, margin + random.nextFloat() * jitter),
            floatArrayOf(w - margin - random.nextFloat() * jitter, h - margin - random.nextFloat() * jitter),
            floatArrayOf(margin + random.nextFloat() * jitter, h - margin - random.nextFloat() * jitter)
        )

        moveTo(corners[0][0], corners[0][1])

        for (edge in 0 until 4) {
            val start = corners[edge]
            val end = corners[(edge + 1) % 4]

            for (seg in 1..segments) {
                val t = seg.toFloat() / segments
                val tPrev = (seg - 0.5f) / segments

                val cx = start[0] + (end[0] - start[0]) * tPrev + (random.nextFloat() - 0.5f) * jitter * 2f
                val cy = start[1] + (end[1] - start[1]) * tPrev + (random.nextFloat() - 0.5f) * jitter * 2f

                val ex = start[0] + (end[0] - start[0]) * t + if (seg < segments) (random.nextFloat() - 0.5f) * jitter else 0f
                val ey = start[1] + (end[1] - start[1]) * t + if (seg < segments) (random.nextFloat() - 0.5f) * jitter else 0f

                quadraticBezierTo(cx, cy, ex, ey)
            }
        }
        close()
    }
}

@Composable
fun HandDrawnBox(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    rotation: Float = 0f,
    seed: Int = 0,
    content: @Composable () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "hand_drawn")
    val wobble by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500 + (seed % 500),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    Box(
        modifier = modifier
            .graphicsLayer { 
                rotationZ = rotation + wobble 
                translationY = wobble * 10f 
            }
            .drawBehind {
                val w = size.width
                val h = size.height

                val fillPath = buildWobblyRectPath(w, h, Random(seed.toLong()), jitter = 3f)
                drawPath(fillPath, containerColor, style = Fill)

                val strokePasses = 3
                for (pass in 0 until strokePasses) {
                    val passRandom = Random(seed.toLong() + pass + 100L)
                    val offsetX = (passRandom.nextFloat() - 0.5f) * 2f
                    val offsetY = (passRandom.nextFloat() - 0.5f) * 2f
                    val strokeWidth = 1.5f + passRandom.nextFloat() * 1.5f 

                    val strokePath = buildWobblyRectPath(
                        w, h,
                        passRandom,
                        jitter = 2.5f + pass * 0.5f
                    )

                    drawContext.canvas.save()
                    drawContext.canvas.translate(offsetX, offsetY)
                    drawPath(
                        strokePath,
                        outlineColor.copy(alpha = 0.6f + pass * 0.15f),
                        style = Stroke(
                            width = strokeWidth.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    drawContext.canvas.restore()
                }
            }
    ) {
        content()
    }
}

@Composable
private fun CustomHeader(
    summary: com.paisa.app.domain.MoneySummary,
    totalSavingsPaise: Long = 0
) {
    HandDrawnBox(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        rotation = 0f,
        seed = 0
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Paisa",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Magic logbook",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val disposableBalance = summary.totalBalancePaise - totalSavingsPaise
                Text(
                    text = disposableBalance.formatSignedInr(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (disposableBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                if (totalSavingsPaise > 0L) {
                    Text(
                        text = "Saved: ${totalSavingsPaise.formatInr()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisaScreen(
    state: PaisaUiState,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDelete: (MoneyTransaction) -> Unit,
    onUpdate: (MoneyTransaction) -> Unit,
    onVoiceClick: () -> Unit,
    editTransactionId: Long? = null,
    onEditTransactionHandled: () -> Unit = {},
    onSavingsPercentageChange: (Int) -> Unit = {},
    onSavingsDeposit: (Long, String) -> Unit = { _, _ -> },
    onSavingsWithdraw: (Long, String) -> Unit = { _, _ -> }
) {
    var selectedTab by rememberSaveable { mutableStateOf(PaisaTab.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    var transactionToEdit by remember { mutableStateOf<MoneyTransaction?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    if (transactionToEdit != null) {
        EditTransactionSheet(
            transaction = transactionToEdit!!,
            onDismiss = { transactionToEdit = null },
            onSave = { updated -> 
                onUpdate(updated)
                transactionToEdit = null
            },
            onDelete = { toDelete ->
                onDelete(toDelete)
                transactionToEdit = null
            }
        )
    }

    AnimatedContent(
        targetState = state.isLoading,
        transitionSpec = {
            if (targetState) {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            } else {
                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                 scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.96f, animationSpec = tween(300)))
            }
        },
        label = "screen_transition"
    ) { isLoading ->
        if (isLoading) {
            LoadingScreen()
        } else {
            Scaffold(
                topBar = {
                    CustomHeader(summary = state.summary, totalSavingsPaise = state.totalSavingsPaise)
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar {
                        PaisaTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(painterResource(tab.iconRes), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text(tab.label, style = MaterialTheme.typography.bodyMedium) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Image(
                        painter = painterResource(R.drawable.bg_doodles),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.06f
                    )
                    
                    when (selectedTab) {
                        PaisaTab.Home -> HomeContent(
                            modifier = Modifier,
                            state = state,
                            onDraftChange = onDraftChange,
                            onSubmit = onSubmit,
                            onSuggestionClick = onSuggestionClick,
                            onDelete = onDelete,
                            onEdit = { transactionToEdit = it },
                            onVoiceClick = onVoiceClick,
                            isListening = state.isListening,
                            isTranscribing = state.isTranscribing
                        )
 
                        PaisaTab.History -> HistoryContent(
                            modifier = Modifier,
                            transactions = state.transactions,
                            onDelete = onDelete,
                            onEdit = { transactionToEdit = it }
                        )
 
                        PaisaTab.Insights -> InsightsContent(
                            modifier = Modifier,
                            transactions = state.transactions
                        )
 
                        PaisaTab.People -> PeopleContent(
                            modifier = Modifier,
                            people = state.people
                        )

                        PaisaTab.Savings -> SavingsContent(
                            modifier = Modifier,
                            state = state,
                            onSavingsPercentageChange = onSavingsPercentageChange,
                            onSavingsDeposit = onSavingsDeposit,
                            onSavingsWithdraw = onSavingsWithdraw
                        )
                    }
                }
            }
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
    onEdit: (MoneyTransaction) -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    isTranscribing: Boolean
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Log your day,",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "what's the damage?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                    Box(modifier = Modifier.animateItemPlacement()) {
                        TransactionRow(transaction = transaction, onDelete = onDelete, onEdit = onEdit)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = 60f
                    )
                )
                .padding(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.suggestions.isNotEmpty()) {
                SuggestionRow(
                    suggestions = state.suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
            QuickEntry(
                draft = state.draft,
                onDraftChange = onDraftChange,
                onSubmit = onSubmit,
                onVoiceClick = onVoiceClick,
                isListening = isListening,
                isTranscribing = isTranscribing
            )
        }
    }
}

@Composable
private fun QuickEntry(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    isTranscribing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HandDrawnBox(
            modifier = Modifier.fillMaxWidth(),
            rotation = 0.5f,
            seed = 42
        ) {
            AnimatedContent(
                targetState = (isListening || isTranscribing),
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "input_mode"
            ) { voiceModeActive ->
                if (voiceModeActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = dotAlpha }
                                .drawBehind {
                                    drawCircle(
                                        color = if (isTranscribing) Color(0xFFFF8F00) else Color(0xFFE53935)
                                    )
                                }
                        )

                        Text(
                            text = if (isTranscribing) "Processing…" else "Hearing…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isTranscribing) Color(0xFFFF8F00) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.widthIn(max = 95.dp)
                        )

                        AudioWaveformIndicator(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp),
                            color = if (isTranscribing) Color(0xFFFF8F00) else MaterialTheme.colorScheme.primary,
                            isProcessing = isTranscribing
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                val ringColor = if (isTranscribing) Color(0xFFFF8F00) else Color(0xFF5D4037)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                            alpha = 1f - (pulseScale - 1f) * 2f
                                        }
                                        .drawBehind {
                                            drawCircle(color = ringColor, alpha = 0.25f)
                                        }
                                )
                                IconButton(
                                    onClick = {},
                                    enabled = !isTranscribing,
                                    modifier = Modifier.bouncyClickable(enabled = !isTranscribing) {
                                        if (!isTranscribing) onVoiceClick()
                                    }
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_mic_handdrawn),
                                        contentDescription = if (isTranscribing) "Transcribing" else "Stop recording",
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isTranscribing) Color(0xFFFF8F00) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft,
                        onValueChange = onDraftChange,
                        placeholder = { Text("200 food, 300 to Rahul", style = MaterialTheme.typography.bodyLarge) },
                        singleLine = true,
                        enabled = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.bouncyClickable { onVoiceClick() }
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_mic_handdrawn),
                                            contentDescription = "Speak entry",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.bouncyClickable { onSubmit() }
                                ) {
                                    Icon(painterResource(R.drawable.ic_send_handdrawn), contentDescription = "Log entry", modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
        Text(
            text = when {
                isTranscribing -> "Whisper is processing your voice…"
                isListening -> "Listening — tap mic again to stop"
                else -> "No forms. Start with amount, add a word or person."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isListening || isTranscribing)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
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
                onClick = {},
                label = { Text(suggestion, style = MaterialTheme.typography.bodyMedium) },
                shape = HandDrawnShapeChip,
                modifier = Modifier.bouncyClickable { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
private fun Dashboard(summary: com.paisa.app.domain.MoneySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(title = "Your Money", trailing = summary.topCategory.takeIf { it != "none" }?.let { "Top: $it" } ?: "No spending")
        
        Row(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                label = "Today",
                value = summary.todayNetPaise.formatSignedInr(),
                subValue = "Spent ${summary.todaySpendingPaise.formatInr()}",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                rotation = -1f,
                seed = 1,
                isLarge = true
            )
            
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1.2f).fillMaxWidth(),
                    label = "This week",
                    value = summary.weekNetPaise.formatSignedInr(),
                    subValue = "Spent ${summary.weekSpendingPaise.formatInr()}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    rotation = 1.5f,
                    seed = 2
                )
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "Expense",
                    value = summary.monthExpensePaise.formatInr(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    rotation = -1.5f,
                    seed = 4
                )
            }
        }
        
        StatCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            label = "Total Income this month",
            value = summary.monthIncomePaise.formatInr(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            rotation = 0.5f,
            seed = 3,
            horizontal = true
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    subValue: String? = null,
    containerColor: Color,
    rotation: Float = 0f,
    seed: Int = 0,
    isLarge: Boolean = false,
    horizontal: Boolean = false
) {
    HandDrawnBox(
        modifier = modifier,
        containerColor = containerColor,
        rotation = rotation,
        seed = seed
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = if (isLarge) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                subValue?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsightsContent(
    modifier: Modifier,
    transactions: List<MoneyTransaction>
) {
    val summary = com.paisa.app.domain.SummaryCalculator.buildSummary(transactions)

    var trendFilter by rememberSaveable { mutableStateOf("7D") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    var activeDrillDownCategory by remember { mutableStateOf<String?>(null) }
    var activeDrillDownDate by remember { mutableStateOf<String?>(null) }

    val categories = remember(transactions) {
        listOf("All") + transactions
            .filter { it.type == TransactionType.EXPENSE }
            .map { it.category }
            .distinct()
            .sorted()
    }
    val filteredExpenses = remember(transactions, trendFilter, selectedCategory) {
        filteredInsightExpenses(transactions, trendFilter, selectedCategory)
    }
    val periodTotal = filteredExpenses.sumOf { it.amountPaise }

    val priorPeriodExpenses = remember(transactions, trendFilter, selectedCategory) {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        
        transactions.filter { transaction ->
            val isSpend = transaction.type == TransactionType.EXPENSE
            val categoryMatches = selectedCategory == "All" || transaction.category == selectedCategory
            val date = Instant.ofEpochMilli(transaction.createdAt).atZone(zone).toLocalDate()
            val periodMatches = when (trendFilter) {
                "7D" -> {
                    date.isAfter(today.minusDays(15)) && date.isBefore(today.minusDays(6))
                }
                "30D" -> {
                    date.isAfter(today.minusDays(61)) && date.isBefore(today.minusDays(29))
                }
                else -> false
            }
            isSpend && categoryMatches && periodMatches
        }
    }
    val priorTotal = priorPeriodExpenses.sumOf { it.amountPaise }
    val comparisonText = remember(periodTotal, priorTotal, trendFilter) {
        if (trendFilter == "All") "" else {
            if (priorTotal == 0L) {
                if (periodTotal == 0L) "No change" else "New activity"
            } else {
                val percentShift = ((periodTotal - priorTotal).toFloat() / priorTotal.toFloat() * 100f).roundToInt()
                val direction = if (percentShift >= 0) "↑" else "↓"
                val windowLabel = if (trendFilter == "7D") "vs last week" else "vs last month"
                "$direction ${Math.abs(percentShift)}% $windowLabel"
            }
        }
    }

    val topCategory = filteredExpenses
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amountPaise } }
        .maxByOrNull { it.value }
    val averageSpend = if (filteredExpenses.isEmpty()) 0L else periodTotal / filterWindowDays(trendFilter, transactions)
    val insightLine = when {
        filteredExpenses.isEmpty() -> "No spending signal yet for this filter."
        topCategory != null -> "${topCategory.key.toTitleCase()} is leading this view at ${topCategory.value.formatInr()}."
        else -> "Your spending is spread evenly in this view."
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFEDE4D3))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            InsightsHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                periodLabel = trendFilter,
                amount = periodTotal.formatInr(),
                insight = insightLine,
                topCategory = topCategory?.key?.toTitleCase() ?: "No category yet",
                comparison = comparisonText
            )

            InsightFilterDeck(
                trendFilter = trendFilter,
                selectedCategory = selectedCategory,
                categories = categories,
                onTrendSelected = { trendFilter = it },
                onCategorySelected = { selectedCategory = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniInsightCard(
                    modifier = Modifier.weight(1f).height(116.dp),
                    label = "In Filter",
                    value = periodTotal.formatInr(),
                    caption = "${filteredExpenses.size} entries",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    seed = 21
                )
                MiniInsightCard(
                    modifier = Modifier.weight(1f).height(116.dp),
                    label = "Daily Pace",
                    value = averageSpend.formatInr(),
                    caption = when (trendFilter) {
                        "All" -> "active days"
                        else -> "avg per day"
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    seed = 22
                )
            }

            HandDrawnBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                rotation = 0.5f,
                seed = 13
            ) {
                DailySpendingLineChart(
                    transactions = transactions,
                    filter = trendFilter,
                    category = selectedCategory,
                    onPointClick = { dateKey ->
                        activeDrillDownDate = dateKey
                    }
                )
            }

            HandDrawnBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                rotation = -0.5f,
                seed = 12
            ) {
                SpendingBarChart(
                    transactions = transactions,
                    onCategoryClick = { catName ->
                        activeDrillDownCategory = catName
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniInsightCard(
                    modifier = Modifier.weight(1f).height(110.dp),
                    label = "This Month",
                    value = summary.monthExpensePaise.formatInr(),
                    caption = "spent so far",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    seed = 23
                )
                MiniInsightCard(
                    modifier = Modifier.weight(1f).height(110.dp),
                    label = "Top Category",
                    value = summary.topCategory.toTitleCase(),
                    caption = if (summary.topCategory == "none") "not enough data" else "highest category",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    seed = 24
                )
            }
        }

        val drillDownTransactions = remember(transactions, activeDrillDownCategory, activeDrillDownDate) {
            val zone = ZoneId.systemDefault()
            transactions.filter {
                val catMatch = activeDrillDownCategory == null || it.category == activeDrillDownCategory
                val dateMatch = activeDrillDownDate == null || {
                    val dt = Instant.ofEpochMilli(it.createdAt).atZone(zone)
                    val dateStr = dt.toLocalDate().toString()
                    if (trendFilter == "7D") {
                        val hour = dt.hour
                        val period = when {
                            hour < 12 -> "Morning"
                            hour < 17 -> "Afternoon"
                            hour < 21 -> "Evening"
                            else -> "Night"
                        }
                        "$dateStr $period" == activeDrillDownDate
                    } else {
                        dateStr == activeDrillDownDate
                    }
                }()
                catMatch && dateMatch && (it.type == TransactionType.EXPENSE)
            }.sortedByDescending { it.createdAt }
        }

        AnimatedVisibility(
            visible = activeDrillDownCategory != null || activeDrillDownDate != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable {
                        activeDrillDownCategory = null
                        activeDrillDownDate = null
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                HandDrawnBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .clickable(enabled = false) {},
                    containerColor = MaterialTheme.colorScheme.surface,
                    rotation = 0f,
                    seed = 77
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    activeDrillDownCategory != null -> "Drill-down: ${activeDrillDownCategory?.toTitleCase()}"
                                    activeDrillDownDate != null -> "Drill-down: $activeDrillDownDate"
                                    else -> "Drill-down"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {},
                                modifier = Modifier.bouncyClickable {
                                    activeDrillDownCategory = null
                                    activeDrillDownDate = null
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_close_handdrawn),
                                    contentDescription = "Close",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (drillDownTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transactions found.", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(drillDownTransactions) { transaction ->
                                    HandDrawnBox(
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        rotation = if (transaction.id.hashCode() % 2 == 0) 0.3f else -0.3f,
                                        seed = transaction.id.hashCode()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = transaction.note.ifBlank { transaction.category.toTitleCase() },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = Instant.ofEpochMilli(transaction.createdAt)
                                                        .atZone(ZoneId.systemDefault())
                                                        .toLocalDate()
                                                        .toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = transaction.amountPaise.formatInr(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsHeroCard(
    modifier: Modifier,
    periodLabel: String,
    amount: String,
    insight: String,
    topCategory: String,
    comparison: String = ""
) {
    HandDrawnBox(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        rotation = -0.6f,
        seed = 42
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.9f, size.height * 0.08f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.width * 0.25f,
                        center = Offset(size.width * 0.08f, size.height * 0.95f)
                    )
                }
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Insight Brief",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.78f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (comparison.isNotBlank()) {
                            val isNegative = comparison.contains("↓")
                            Surface(
                                color = if (isNegative) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                shape = HandDrawnShapeChip
                            ) {
                                Text(
                                    comparison,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isNegative) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.14f),
                            shape = HandDrawnShapeChip
                        ) {
                            Text(
                                periodLabel,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    }
                }

                Text(
                    amount,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Black
                )

                Text(
                    insight,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )

                Text(
                    "Focus area: $topCategory",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun InsightFilterDeck(
    trendFilter: String,
    selectedCategory: String,
    categories: List<String>,
    onTrendSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Slice the story",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("7D", "30D", "All")) { filter ->
                FilterChip(
                    selected = trendFilter == filter,
                    onClick = { onTrendSelected(filter) },
                    label = { Text(filter) },
                    shape = HandDrawnShapeChip
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.toTitleCase()) },
                    shape = HandDrawnShapeChip
                )
            }
        }
    }
}

@Composable
private fun MiniInsightCard(
    modifier: Modifier,
    label: String,
    value: String,
    caption: String,
    containerColor: Color,
    seed: Int
) {
    HandDrawnBox(
        modifier = modifier,
        containerColor = containerColor,
        rotation = if (seed % 2 == 0) 0.8f else -0.8f,
        seed = seed
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpendingBarChart(
    transactions: List<MoneyTransaction>,
    onCategoryClick: (String) -> Unit
) {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    if (expenses.isEmpty()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Category Mix", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EmptyState("No spending data yet.")
        }
        return
    }

    val grouped = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amountPaise } }
    val sortedEntries = grouped.entries.sortedByDescending { it.value }
    
    val displayList = if (sortedEntries.size <= 5) {
        sortedEntries
    } else {
        val top4 = sortedEntries.take(4)
        val remainingSum = sortedEntries.drop(4).sumOf { it.value }
        top4 + java.util.AbstractMap.SimpleEntry("Others", remainingSum)
    }

    val maxAmount = displayList.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Category Mix", style = MaterialTheme.typography.titleMedium)
        Text(
            "Where your money clusters most often. Tap a bar to view entries.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp, bottom = 16.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val baseColor = MaterialTheme.colorScheme.primary
            val colors = List(displayList.size) { index ->
                baseColor.copy(alpha = 1f - (index * 0.18f).coerceAtMost(0.7f))
            }

            displayList.forEachIndexed { index, entry ->
                val ratio = entry.value.toFloat() / maxAmount
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entry.key.take(12).toTitleCase(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(70.dp),
                        maxLines = 1
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .bouncyClickable {
                                    onCategoryClick(entry.key)
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            
                            if (w > 0f) {
                                val r = Random(entry.key.hashCode().toLong())
                                val path = buildWobblyRectPath(w, h, r, jitter = 1.5f)
                                drawPath(path, color = colors[index % colors.size], style = Fill)
                                
                                for (pass in 0 until 2) {
                                    val passRandom = Random(entry.key.hashCode().toLong() + pass + 100L)
                                    val offsetX = (passRandom.nextFloat() - 0.5f) * 1.5f
                                    val offsetY = (passRandom.nextFloat() - 0.5f) * 1.5f
                                    val strokeWidth = 1f + passRandom.nextFloat() * 1f
                                    val strokePath = buildWobblyRectPath(w, h, passRandom, jitter = 1.5f + pass * 0.5f)
                                    
                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(offsetX, offsetY)
                                    drawPath(
                                        strokePath,
                                        color = outlineColor.copy(alpha = 0.5f + pass * 0.2f),
                                        style = Stroke(
                                            width = strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                    drawContext.canvas.restore()
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = entry.value.formatInr(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(70.dp).padding(start = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun DailySpendingLineChart(
    transactions: List<MoneyTransaction>,
    filter: String,
    category: String,
    onPointClick: (String) -> Unit
) {
    val expenses = transactions.filter { 
        (it.type == TransactionType.EXPENSE) &&
        (category == "All" || it.category == category)
    }

    if (expenses.isEmpty()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Trend", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EmptyState("No data for these filters.")
        }
        return
    }

    val now = Instant.now()
    val zone = ZoneId.systemDefault()
    val today = now.atZone(zone).toLocalDate()

    val filteredExpenses = when (filter) {
        "7D" -> expenses.filter { 
            val date = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            date.isAfter(today.minusDays(7)) || date.isEqual(today)
        }
        "30D" -> expenses.filter { 
            val date = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            date.isAfter(today.minusDays(30)) || date.isEqual(today)
        }
        else -> expenses
    }

    if (filteredExpenses.isEmpty()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Trend", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EmptyState("No recent data.")
        }
        return
    }

    val groupedData: Map<String, Long> = if (filter == "7D") {
        filteredExpenses.groupBy { 
            val dt = Instant.ofEpochMilli(it.createdAt).atZone(zone)
            val date = dt.toLocalDate()
            val hour = dt.hour
            val period = when {
                hour < 12 -> "Morning"
                hour < 17 -> "Afternoon"
                hour < 21 -> "Evening"
                else -> "Night"
            }
            "$date $period"
        }.mapValues { it.value.sumOf { t -> t.amountPaise } }
    } else {
        filteredExpenses.groupBy { 
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().toString()
        }.mapValues { it.value.sumOf { t -> t.amountPaise } }
    }

    val sortedKeys = groupedData.keys.sortedWith(Comparator { k1, k2 ->
        if (filter == "7D") {
            val s1 = k1.split(" ")
            val s2 = k2.split(" ")
            if (s1.size == 2 && s2.size == 2) {
                val dateCompare = s1[0].compareTo(s2[0])
                if (dateCompare != 0) {
                    dateCompare
                } else {
                    val p1Rank = when (s1[1]) {
                        "Morning" -> 0
                        "Afternoon" -> 1
                        "Evening" -> 2
                        "Night" -> 3
                        else -> 4
                    }
                    val p2Rank = when (s2[1]) {
                        "Morning" -> 0
                        "Afternoon" -> 1
                        "Evening" -> 2
                        "Night" -> 3
                        else -> 4
                    }
                    p1Rank.compareTo(p2Rank)
                }
            } else {
                k1.compareTo(k2)
            }
        } else {
            k1.compareTo(k2)
        }
    })

    val maxAmount = groupedData.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    
    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    val lineColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Spending Trend", style = MaterialTheme.typography.titleMedium)
            selectedPoint?.let { idx ->
                val key = sortedKeys[idx]
                val amount = groupedData[key] ?: 0L
                Text(
                    text = amount.formatInr(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            trendCaption(groupedData, filter, category),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Column(
                modifier = Modifier.fillMaxHeight().width(50.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(maxAmount.formatInr(), style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f), maxLines = 1)
                Text((maxAmount / 2).formatInr(), style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f), maxLines = 1)
                Text("0", style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f), maxLines = 1)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 50.dp, bottom = 20.dp)
                    .pointerInput(sortedKeys) {
                        detectTapGestures { offset ->
                            val w = size.width
                            val stepX = w / (sortedKeys.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / stepX).roundToInt().coerceIn(0, sortedKeys.size - 1)
                            selectedPoint = idx
                            onPointClick(sortedKeys[idx])
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val stepX = w / (sortedKeys.size - 1).coerceAtLeast(1)
                
                drawLine(color = outlineColor.copy(alpha = 0.1f), start = Offset(0f, h), end = Offset(w, h), strokeWidth = 1f)
                drawLine(color = outlineColor.copy(alpha = 0.1f), start = Offset(0f, h/2f), end = Offset(w, h/2f), strokeWidth = 1f)
                drawLine(color = outlineColor.copy(alpha = 0.1f), start = Offset(0f, 0f), end = Offset(w, 0f), strokeWidth = 1f)
                
                val points = sortedKeys.mapIndexed { index, key ->
                    val amount = groupedData[key] ?: 0L
                    val ratio = amount.toFloat() / maxAmount.toFloat()
                    Offset(index * stepX, h - (ratio * h))
                }
                
                if (points.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val midX = (p1.x + p2.x) / 2f
                            quadraticBezierTo(midX, p1.y, p2.x, p2.y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    
                    points.forEachIndexed { idx: Int, pt: Offset ->
                        val isSelected = selectedPoint == idx
                        drawCircle(
                            color = if (isSelected) lineColor else outlineColor,
                            radius = if (isSelected) 10f else 6f,
                            center = pt
                        )
                        if (isSelected) {
                            drawCircle(color = Color.White, radius = 4f, center = pt)
                            
                            val key = sortedKeys[idx]
                            val amount = groupedData[key] ?: 0L
                            val amountText = amount.formatInr()
                            
                            drawContext.canvas.nativeCanvas.apply {
                                val tooltipW = 120f
                                val tooltipH = 50f
                                val rectLeft = (pt.x - tooltipW / 2f).coerceIn(0f, w - tooltipW)
                                val rectTop = pt.y - tooltipH - 20f
                                
                                val r = Random(idx.toLong())
                                val toolTipPath = buildWobblyRectPath(tooltipW, tooltipH, r, jitter = 1.5f)
                                
                                save()
                                translate(rectLeft, rectTop)
                                
                                val fillPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#EDE4D3")
                                    style = android.graphics.Paint.Style.FILL
                                }
                                drawPath(toolTipPath.asAndroidPath(), fillPaint)
                                
                                val strokePaint = android.graphics.Paint().apply {
                                    color = outlineColor.copy(alpha = 0.8f).toArgb()
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 3f
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                }
                                drawPath(toolTipPath.asAndroidPath(), strokePaint)
                                
                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 20f
                                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                drawText(amountText, tooltipW / 2f, tooltipH / 2f + 8f, textPaint)
                                restore()
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 50.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val startLabel = sortedKeys.firstOrNull()?.toString()?.takeLast(5) ?: ""
            val endLabel = sortedKeys.lastOrNull()?.toString()?.takeLast(5) ?: ""
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun filteredInsightExpenses(
    transactions: List<MoneyTransaction>,
    filter: String,
    category: String
): List<MoneyTransaction> {
    val zone = ZoneId.systemDefault()
    val today = Instant.now().atZone(zone).toLocalDate()

    return transactions.filter { transaction ->
        val isSpend = transaction.type == TransactionType.EXPENSE
        val categoryMatches = category == "All" || transaction.category == category
        val date = Instant.ofEpochMilli(transaction.createdAt).atZone(zone).toLocalDate()
        val periodMatches = when (filter) {
            "7D" -> date.isAfter(today.minusDays(7)) || date.isEqual(today)
            "30D" -> date.isAfter(today.minusDays(30)) || date.isEqual(today)
            else -> true
        }

        isSpend && categoryMatches && periodMatches
    }
}

private fun filterWindowDays(
    filter: String,
    allTransactions: List<MoneyTransaction>
): Long {
    val zone = ZoneId.systemDefault()
    val today = Instant.now().atZone(zone).toLocalDate()
    
    val oldestTransactionDate = allTransactions.minOfOrNull { it.createdAt }
        ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        ?: today

    val accountAgeDays = java.time.temporal.ChronoUnit.DAYS.between(oldestTransactionDate, today) + 1L

    return when (filter) {
        "7D" -> minOf(7L, accountAgeDays).coerceAtLeast(1L)
        "30D" -> minOf(30L, accountAgeDays).coerceAtLeast(1L)
        else -> accountAgeDays.coerceAtLeast(1L)
    }
}

private fun trendCaption(groupedData: Map<String, Long>, filter: String, category: String): String {
    val peak = groupedData.maxByOrNull { it.value } ?: return "No clear pattern yet."
    val categoryText = if (category == "All") "all spending" else category.toTitleCase()
    val windowText = when (filter) {
        "7D" -> "this week"
        "30D" -> "this month"
        else -> "overall"
    }

    return "Peak for $categoryText is ${peak.value.formatInr()} $windowText."
}

private fun String.toTitleCase(): String {
    if (isBlank()) return this
    if (this == "All") return this
    return split(Regex("""\s+"""))
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(title = "Network", trailing = "${people.size} people")
        }

        if (people.isNotEmpty()) {
            item {
                val netTotal = people.sumOf { it.netPaise }
                StatCard(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    label = "Total Net Balance",
                    value = netTotal.formatSignedInr(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    rotation = -0.5f,
                    seed = 101,
                    horizontal = true
                )
            }
        }
        if (people.isEmpty()) {
            item {
                EmptyState("Entries like `500 to Rahul` appear here.")
            }
        } else {
            items(people, key = { it.name }) { person ->
                HandDrawnBox(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    rotation = (person.name.hashCode() % 3 - 1).toFloat(),
                    seed = person.name.hashCode()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                person.name, 
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                person.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = person.netPaise.formatSignedInr(),
                            style = MaterialTheme.typography.bodyLarge,
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
    onDelete: (MoneyTransaction) -> Unit,
    onEdit: (MoneyTransaction) -> Unit
) {
    val grouped = remember(transactions) {
        transactions.groupBy { 
            val instant = Instant.ofEpochMilli(it.createdAt)
            val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            date
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(title = "Timeline", trailing = "${transactions.size} logs")
        }

        if (transactions.isEmpty()) {
            item {
                EmptyState("Your logged money activity will appear here.")
            }
        } else {
            grouped.forEach { (date, dailyTransactions) ->
                item {
                    Text(
                        text = when (date) {
                            java.time.LocalDate.now() -> "Today"
                            java.time.LocalDate.now().minusDays(1) -> "Yesterday"
                            else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM"))
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(dailyTransactions, key = { it.id }) { transaction ->
                    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                    Box(modifier = Modifier.animateItemPlacement()) {
                        TransactionRow(transaction = transaction, onDelete = onDelete, onEdit = onEdit)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: MoneyTransaction,
    onDelete: (MoneyTransaction) -> Unit,
    onEdit: (MoneyTransaction) -> Unit
) {
    HandDrawnBox(
        modifier = Modifier.fillMaxWidth().bouncyClickable { onEdit(transaction) },
        containerColor = MaterialTheme.colorScheme.surface,
        rotation = (transaction.id.hashCode() % 3 - 1).toFloat(),
        seed = transaction.id.hashCode()
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
                    painter = painterResource(
                        when (transaction.type) {
                            TransactionType.SAVINGS_DEPOSIT,
                            TransactionType.SAVINGS_WITHDRAW -> R.drawable.ic_savings_handdrawn
                            TransactionType.LENT,
                            TransactionType.BORROWED -> R.drawable.ic_people_handdrawn
                            else -> R.drawable.ic_home_handdrawn
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = when (transaction.type) {
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.LENT -> MaterialTheme.colorScheme.tertiary
                        TransactionType.BORROWED -> MaterialTheme.colorScheme.secondary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurfaceVariant
                        TransactionType.SAVINGS_DEPOSIT -> MaterialTheme.colorScheme.primary
                        TransactionType.SAVINGS_WITHDRAW -> MaterialTheme.colorScheme.secondary
                    }
                )
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = transaction.displayTitle(),
                        style = MaterialTheme.typography.titleMedium,
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = transaction.amountColor()
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier.bouncyClickable { onDelete(transaction) }
                ) {
                    Icon(painterResource(R.drawable.ic_delete_handdrawn), contentDescription = "Delete entry", modifier = Modifier.size(24.dp))
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
        Text(
            title, 
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            trailing,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    HandDrawnBox(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        rotation = -1f,
        seed = text.hashCode()
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge,
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

private fun MoneyTransaction.displayTitle(): String {
    return note
        .takeIf { it.isNotBlank() }
        ?: rawText
}

@Composable
private fun MoneyTransaction.signedAmount(): String {
    return when (type) {
        TransactionType.INCOME -> amountPaise.formatSignedInr()
        TransactionType.BORROWED -> amountPaise.formatSignedInr()
        TransactionType.EXPENSE -> "-" + amountPaise.formatInr()
        TransactionType.LENT -> "-" + amountPaise.formatInr()
        TransactionType.SAVINGS_DEPOSIT -> "-" + amountPaise.formatInr()
        TransactionType.SAVINGS_WITHDRAW -> "+" + amountPaise.formatInr()
    }
}

@Composable
private fun MoneyTransaction.amountColor() = when (type) {
    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
    TransactionType.BORROWED -> MaterialTheme.colorScheme.secondary
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    TransactionType.LENT -> MaterialTheme.colorScheme.tertiary
    TransactionType.SAVINGS_DEPOSIT -> MaterialTheme.colorScheme.primary
    TransactionType.SAVINGS_WITHDRAW -> MaterialTheme.colorScheme.secondary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: MoneyTransaction,
    onDismiss: () -> Unit,
    onSave: (MoneyTransaction) -> Unit,
    onDelete: (MoneyTransaction) -> Unit
) {
    var rawText by remember { mutableStateOf(transaction.displayTitle()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Transaction", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                label = { Text("Entry") },
                modifier = Modifier.fillMaxWidth(),
                shape = HandDrawnShapeChip,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HandDrawnBox(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { showDeleteConfirm = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    rotation = -1f,
                    seed = 444
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                
                HandDrawnBox(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                        val parser = com.paisa.app.domain.MoneyParser()
                        val result = parser.parse(rawText)
                        if (result is com.paisa.app.domain.ParseResult.Success) {
                            onSave(transaction.copy(
                                rawText = result.entry.rawText,
                                amountPaise = result.entry.amountPaise,
                                type = result.entry.type,
                                category = result.entry.category,
                                personName = result.entry.personName,
                                note = result.entry.note
                            ))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    rotation = 1f,
                    seed = 555
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete entry?") },
            text = { Text("This will permanently remove this transaction.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(transaction)
                    onDismiss()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScribbleCircularLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: androidx.compose.ui.unit.Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scribble_loader")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    val seedFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seed"
    )
    val seed = seedFloat.toInt()
    val strokeWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = (minOf(w, h) - strokeWidthPx) / 2f

        if (radius <= 0f) return@Canvas

        val random = java.util.Random(seed.toLong())
        val path = Path()

        val totalAngles = (360f * progress)
        val step = 10f // degree steps

        var started = false
        var angle = 0f

        while (angle <= totalAngles) {
            val rad = Math.toRadians(angle.toDouble())
            val jitterRadius = radius + (random.nextFloat() - 0.5f) * 6f
            val x = cx + Math.cos(rad).toFloat() * jitterRadius
            val y = cy + Math.sin(rad).toFloat() * jitterRadius

            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
            angle += step
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun AudioWaveformIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: androidx.compose.ui.unit.Dp = 3.dp,
    gap: androidx.compose.ui.unit.Dp = 3.dp,
    isProcessing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val gapPx = with(density) { gap.toPx() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val maxBarHeight = h * 0.9f
        val minBarHeight = h * 0.2f

        val stepPx = barWidthPx + gapPx
        val barCount = (w / stepPx).toInt().coerceAtLeast(5)

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount.toFloat()
            val wave1 = Math.sin((phase + progress * 3 * Math.PI)).toFloat()
            val wave2 = Math.cos((phase * 1.8 + progress * 5f)).toFloat()
            val combinedWave = (wave1 + wave2) / 2f

            val amplitude = if (isProcessing) {
                0.3f + 0.7f * Math.abs(Math.sin((phase + progress * 2f * Math.PI)).toFloat())
            } else {
                0.2f + 0.8f * Math.abs(combinedWave)
            }

            val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * amplitude
            val x = i * stepPx + barWidthPx / 2f
            val startY = centerY - barHeight / 2f
            val endY = centerY + barHeight / 2f

            drawLine(
                color = color,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SavingsContent(
    modifier: Modifier,
    state: PaisaUiState,
    onSavingsPercentageChange: (Int) -> Unit,
    onSavingsDeposit: (Long, String) -> Unit,
    onSavingsWithdraw: (Long, String) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    
    var triggerWobble by remember { mutableStateOf(0) }
    val wobbleScale by animateFloatAsState(
        targetValue = if (triggerWobble % 2 == 1) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wobble_scale"
    )
    val wobbleRotation by animateFloatAsState(
        targetValue = if (triggerWobble == 0) 0f else if (triggerWobble % 2 == 1) -5f else 5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "wobble_rotation"
    )

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var depositAmountStr by remember { mutableStateOf("") }
    var depositNoteStr by remember { mutableStateOf("") }
    var withdrawAmountStr by remember { mutableStateOf("") }
    var withdrawNoteStr by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HandDrawnBox(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            rotation = -0.5f,
            seed = 88
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Locked Savings Pool",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    state.totalSavingsPaise.formatInr(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Safely hidden from your primary Wallet balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    scaleX = wobbleScale
                    scaleY = wobbleScale
                    rotationZ = wobbleRotation
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        triggerWobble++
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                
                // 🐸 "Gullak" the Coin Pet Frog sitting next to the jar
                val fx = cx - 94.dp.toPx()
                val fy = cy + 56.dp.toPx()
                val frogRadius = 16.dp.toPx()
                
                // Draw frog body fill (mocha/tan cream matching secondaryContainer)
                drawCircle(
                    color = Color(0xFFF5EEDC),
                    radius = frogRadius,
                    center = Offset(fx, fy)
                )
                
                // Draw wobbly frog outline (outlineColor)
                drawCircle(
                    color = outlineColor.copy(alpha = 0.8f),
                    radius = frogRadius,
                    center = Offset(fx, fy),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Draw eyes (bulging circles)
                val eyeL = Offset(fx - 7.dp.toPx(), fy - 14.dp.toPx())
                val eyeR = Offset(fx + 7.dp.toPx(), fy - 14.dp.toPx())
                val eyeRadius = 5.dp.toPx()
                
                // Eye fills
                drawCircle(color = Color(0xFFF5EEDC), radius = eyeRadius, center = eyeL)
                drawCircle(color = Color(0xFFF5EEDC), radius = eyeRadius, center = eyeR)
                // Eye outlines
                drawCircle(color = outlineColor.copy(alpha = 0.8f), radius = eyeRadius, center = eyeL, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = outlineColor.copy(alpha = 0.8f), radius = eyeRadius, center = eyeR, style = Stroke(width = 1.5.dp.toPx()))
                // Pupils (black dots)
                drawCircle(color = outlineColor, radius = 2.dp.toPx(), center = eyeL)
                drawCircle(color = outlineColor, radius = 2.dp.toPx(), center = eyeR)
                
                // Smiling mouth
                val mouthPath = Path().apply {
                    moveTo(fx - 7.dp.toPx(), fy + 2.dp.toPx())
                    quadraticBezierTo(fx, fy + 8.dp.toPx(), fx + 7.dp.toPx(), fy + 2.dp.toPx())
                }
                drawPath(
                    path = mouthPath,
                    color = outlineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Rosy cheeks
                drawCircle(color = Color(0xFFE57373).copy(alpha = 0.6f), radius = 2.5.dp.toPx(), center = Offset(fx - 10.dp.toPx(), fy + 1.dp.toPx()))
                drawCircle(color = Color(0xFFE57373).copy(alpha = 0.6f), radius = 2.5.dp.toPx(), center = Offset(fx + 10.dp.toPx(), fy + 1.dp.toPx()))
                
                // Little frog legs/feet at the base
                val legL = Path().apply {
                    moveTo(fx - 12.dp.toPx(), fy + 12.dp.toPx())
                    quadraticBezierTo(fx - 18.dp.toPx(), fy + 16.dp.toPx(), fx - 16.dp.toPx(), fy + 16.dp.toPx())
                }
                val legR = Path().apply {
                    moveTo(fx + 12.dp.toPx(), fy + 12.dp.toPx())
                    quadraticBezierTo(fx + 18.dp.toPx(), fy + 16.dp.toPx(), fx + 16.dp.toPx(), fy + 16.dp.toPx())
                }
                drawPath(path = legL, color = outlineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path = legR, color = outlineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                
                val jarPath = Path()
                
                val neckL = Offset(cx - 50.dp.toPx(), cy - 70.dp.toPx())
                val neckR = Offset(cx + 50.dp.toPx(), cy - 70.dp.toPx())
                val shoulderL = Offset(cx - 60.dp.toPx(), cy - 40.dp.toPx())
                val shoulderR = Offset(cx + 60.dp.toPx(), cy - 40.dp.toPx())
                val baseL = Offset(cx - 75.dp.toPx(), cy + 70.dp.toPx())
                val baseR = Offset(cx + 75.dp.toPx(), cy + 70.dp.toPx())

                jarPath.moveTo(neckL.x, neckL.y)
                jarPath.lineTo(neckR.x, neckR.y)
                jarPath.quadraticBezierTo(cx + 55.dp.toPx(), cy - 55.dp.toPx(), shoulderR.x, shoulderR.y)
                jarPath.lineTo(baseR.x, baseR.y)
                jarPath.lineTo(baseL.x, baseL.y)
                jarPath.lineTo(shoulderL.x, shoulderL.y)
                jarPath.quadraticBezierTo(cx - 55.dp.toPx(), cy - 55.dp.toPx(), neckL.x, neckL.y)
                
                val fillRatio = (state.totalSavingsPaise / 1000000f).coerceIn(0f, 0.95f)
                if (fillRatio > 0f) {
                    val fillHeight = (baseL.y - shoulderL.y) * fillRatio
                    val fillTopY = baseL.y - fillHeight
                    
                    val goldFillPath = Path()
                    goldFillPath.moveTo(baseL.x + 2.dp.toPx(), baseL.y - 2.dp.toPx())
                    goldFillPath.lineTo(baseR.x - 2.dp.toPx(), baseR.y - 2.dp.toPx())
                    
                    goldFillPath.lineTo(
                        cx + (baseR.x - cx) * (1f - fillRatio * 0.15f) - 2.dp.toPx(),
                        fillTopY
                    )
                    goldFillPath.quadraticBezierTo(
                        cx,
                        fillTopY + (if (triggerWobble % 2 == 1) -5.dp.toPx() else 5.dp.toPx()),
                        cx - (cx - baseL.x) * (1f - fillRatio * 0.15f) + 2.dp.toPx(),
                        fillTopY
                    )
                    goldFillPath.close()
                    
                    drawPath(
                        path = goldFillPath,
                        color = Color(0xFFFFD54F).copy(alpha = 0.5f),
                        style = Fill
                    )
                    
                    val coinCount = (state.totalSavingsPaise / 50000L).toInt().coerceIn(0, 15)
                    val coinRandom = java.util.Random(99L)
                    for (i in 0 until coinCount) {
                        val coinX = cx + (coinRandom.nextFloat() - 0.5f) * 80.dp.toPx() * (1f - fillRatio * 0.2f)
                        val coinY = baseL.y - 12.dp.toPx() - coinRandom.nextFloat() * (fillHeight - 12.dp.toPx()).coerceAtLeast(0f)
                        
                        drawCircle(
                            color = Color(0xFFFFB300),
                            radius = 10.dp.toPx(),
                            center = Offset(coinX, coinY),
                            style = Fill
                        )
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.5f),
                            radius = 10.dp.toPx(),
                            center = Offset(coinX, coinY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawLine(
                            color = outlineColor.copy(alpha = 0.6f),
                            start = Offset(coinX - 4.dp.toPx(), coinY),
                            end = Offset(coinX + 4.dp.toPx(), coinY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                for (pass in 0 until 2) {
                    val passRandom = java.util.Random(pass + 42L)
                    val offsetX = (passRandom.nextFloat() - 0.5f) * 2f
                    val offsetY = (passRandom.nextFloat() - 0.5f) * 2f
                    
                    drawContext.canvas.save()
                    drawContext.canvas.translate(offsetX, offsetY)
                    
                    drawPath(
                        path = jarPath,
                        color = outlineColor.copy(alpha = 0.6f + pass * 0.2f),
                        style = Stroke(
                            width = (2f + passRandom.nextFloat() * 1.5f).dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    drawContext.canvas.restore()
                }
            }
            Text(
                "Tap to Shake",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
            )
        }

        HandDrawnBox(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            rotation = 0.4f,
            seed = 44
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Auto-Savings Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Set a percentage of incoming income to automatically pay yourself first. Deducted from disposable wallet balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0, 10, 20, 30, 50).forEach { pct ->
                        val isSelected = state.savingsPercentage == pct
                        Surface(
                            onClick = { onSavingsPercentageChange(pct) },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = HandDrawnShapeChip,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .bouncyClickable { onSavingsPercentageChange(pct) }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        HandDrawnBox(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            rotation = -0.4f,
            seed = 22
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Manual Adjustments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showDepositDialog = true },
                        modifier = Modifier.weight(1f).bouncyClickable { showDepositDialog = true },
                        shape = HandDrawnShapeChip,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Deposit Cash", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.weight(1f).bouncyClickable { showWithdrawDialog = true },
                        shape = HandDrawnShapeChip,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Withdraw Cash", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDepositDialog) {
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depositAmountStr.toLongOrNull()?.times(100L)
                        if (amt != null && amt > 0L) {
                            onSavingsDeposit(amt, depositNoteStr)
                            showDepositDialog = false
                            depositAmountStr = ""
                            depositNoteStr = ""
                        }
                    },
                    modifier = Modifier.bouncyClickable {
                        val amt = depositAmountStr.toLongOrNull()?.times(100L)
                        if (amt != null && amt > 0L) {
                            onSavingsDeposit(amt, depositNoteStr)
                            showDepositDialog = false
                            depositAmountStr = ""
                            depositNoteStr = ""
                        }
                    },
                    shape = HandDrawnShapeChip
                ) {
                    Text("Confirm Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Deposit to Savings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter amount to deduct from Wallet and move to Savings lockbox:")
                    OutlinedTextField(
                        value = depositAmountStr,
                        onValueChange = { depositAmountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = depositNoteStr,
                        onValueChange = { depositNoteStr = it },
                        label = { Text("Note (e.g. Monthly cut)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmountStr.toLongOrNull()?.times(100L)
                        if (amt != null && amt > 0L && amt <= state.totalSavingsPaise) {
                            onSavingsWithdraw(amt, withdrawNoteStr)
                            showWithdrawDialog = false
                            withdrawAmountStr = ""
                            withdrawNoteStr = ""
                        }
                    },
                    modifier = Modifier.bouncyClickable {
                        val amt = withdrawAmountStr.toLongOrNull()?.times(100L)
                        if (amt != null && amt > 0L && amt <= state.totalSavingsPaise) {
                            onSavingsWithdraw(amt, withdrawNoteStr)
                            showWithdrawDialog = false
                            withdrawAmountStr = ""
                            withdrawNoteStr = ""
                        }
                    },
                    shape = HandDrawnShapeChip
                ) {
                    Text("Confirm Withdrawal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Withdraw from Savings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter amount to move back to primary spending Wallet balance:")
                    OutlinedTextField(
                        value = withdrawAmountStr,
                        onValueChange = { withdrawAmountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = withdrawNoteStr,
                        onValueChange = { withdrawNoteStr = it },
                        label = { Text("Note (e.g. Emergency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
private fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_loading")
    val textScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Scribble Progress Circle spinning around the logo
                ScribbleCircularLoader(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )

                HandDrawnBox(
                    modifier = Modifier.size(100.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    rotation = 5f,
                    seed = 99
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.app_logo_concept_2),
                            contentDescription = "Paisa Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "Sketching your finances...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    scaleX = textScale
                    scaleY = textScale
                }
            )
        }
    }
}
