package com.example.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope,
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.surfaceContainerLow,
                        colorScheme.primaryContainer.copy(alpha = 0.35f),
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Park,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.auth_studio_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Botanical arch blueprint illustration drawn on Canvas
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .clip(RoundedCornerShape(topStart = 94.dp, topEnd = 94.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    colorScheme.surfaceContainer,
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = colorScheme.outlineVariant,
                            shape = RoundedCornerShape(topStart = 94.dp, topEnd = 94.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val leafPrimary = colorScheme.primary
                    val leafAccent = colorScheme.secondary
                    val pollen = colorScheme.tertiary
                    val gridLine = colorScheme.outlineVariant

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Subtle drafting grid
                        val step = w / 6f
                        for (i in 1..5) {
                            drawLine(
                                color = gridLine.copy(alpha = 0.45f),
                                start = Offset(step * i, 0f),
                                end = Offset(step * i, h),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawLine(
                                color = gridLine.copy(alpha = 0.45f),
                                start = Offset(0f, step * i),
                                end = Offset(w, step * i),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        // Sun orb
                        drawCircle(
                            color = pollen.copy(alpha = 0.85f),
                            radius = w * 0.11f,
                            center = Offset(w * 0.68f, h * 0.28f),
                        )
                        // Pathway arc
                        val path = Path().apply {
                            moveTo(w * 0.25f, h * 0.88f)
                            cubicTo(w * 0.35f, h * 0.65f, w * 0.62f, h * 0.68f, w * 0.72f, h * 0.45f)
                        }
                        drawPath(
                            path = path,
                            color = leafAccent.copy(alpha = 0.55f),
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                        )
                        // Botanical canopies
                        drawCircle(
                            color = leafPrimary.copy(alpha = 0.85f),
                            radius = w * 0.16f,
                            center = Offset(w * 0.36f, h * 0.44f),
                        )
                        drawCircle(
                            color = leafPrimary.copy(alpha = 0.55f),
                            radius = w * 0.12f,
                            center = Offset(w * 0.64f, h * 0.62f),
                        )
                        drawCircle(
                            color = leafAccent.copy(alpha = 0.7f),
                            radius = w * 0.08f,
                            center = Offset(w * 0.32f, h * 0.68f),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.auth_headline),
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FeatureHighlightRow(
                    icon = Icons.Rounded.GridView,
                    title = stringResource(R.string.auth_feature_layout_title),
                    subtitle = stringResource(R.string.auth_feature_layout_desc),
                )
                FeatureHighlightRow(
                    icon = Icons.Rounded.LocalFlorist,
                    title = stringResource(R.string.auth_feature_plants_title),
                    subtitle = stringResource(R.string.auth_feature_plants_desc),
                )
                FeatureHighlightRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.auth_feature_visuals_title),
                    subtitle = stringResource(R.string.auth_feature_visuals_desc),
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            },
                        )
                    },
                    enabled = !isLoading && !isCheckingAutoSignIn,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("google_sign_in_button"),
                ) {
                    if (isLoading || isCheckingAutoSignIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isCheckingAutoSignIn) stringResource(R.string.auth_checking) else stringResource(R.string.auth_signing_in),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.auth_sign_in_google),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainerLowest.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
