package com.example.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassStyle
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.ChromeSilver
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "WallxAuth"

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }
    val coroutineScope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cancelledMessage = stringResource(R.string.auth_error_cancelled)
    val noAccountMessage = stringResource(R.string.auth_error_no_account)
    val genericErrorMessage = stringResource(R.string.auth_error_generic)

    // Silent auto-sign-in for returning authorized Google users only (separate request)
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) return@LaunchedEffect
        try {
            val credentialManager = CredentialManager.create(activityContext)
            val silentOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build()
            val silentRequest = GetCredentialRequest.Builder()
                .addCredentialOption(silentOption)
                .build()

            val result = credentialManager.getCredential(
                request = silentRequest,
                context = activityContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(firebaseCredential).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            // Expected for first-time users; interactive Google Sign-In button stays ready
        } catch (e: Exception) {
            Log.d(AUTH_TAG, "Silent Google sign-in skipped: ${e.message}")
        }
    }

    fun startGoogleSignIn() {
        if (isSigningIn) return
        coroutineScope.launch {
            isSigningIn = true
            errorMessage = null
            try {
                val credentialManager = CredentialManager.create(activityContext)
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = context.getString(R.string.default_web_client_id)
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(firebaseCredential).await()
                } else {
                    errorMessage = genericErrorMessage
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: GetCredentialCancellationException) {
                errorMessage = cancelledMessage
            } catch (_: NoCredentialException) {
                errorMessage = noAccountMessage
            } catch (e: GetCredentialException) {
                Log.e(AUTH_TAG, "Credential Manager sign-in failed", e)
                errorMessage = e.localizedMessage?.takeIf { it.isNotBlank() } ?: genericErrorMessage
            } catch (e: Exception) {
                Log.e(AUTH_TAG, "Firebase Google sign-in failed", e)
                errorMessage = e.localizedMessage?.takeIf { it.isNotBlank() } ?: genericErrorMessage
            } finally {
                isSigningIn = false
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val inkColor = MaterialTheme.colorScheme.onBackground
    val discTopColor = MaterialTheme.colorScheme.surfaceContainer
    val discBottomColor = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // GPU-cached White, Silver & Black WX Crest Emblem
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val rimStroke = Stroke(width = 2.8.dp.toPx())
                        val lineStrokeWidth = 5.dp.toPx()
                        val silverBlackRim = Brush.linearGradient(
                            colors = listOf(primaryColor, tertiaryColor, primaryColor),
                            start = Offset.Zero,
                            end = Offset(w, h)
                        )
                        val discBrush = Brush.linearGradient(
                            colors = listOf(discBottomColor, discTopColor),
                            start = Offset.Zero,
                            end = Offset(w, h)
                        )
                        val p1 = Offset(w * 0.26f, h * 0.34f)
                        val p2 = Offset(w * 0.38f, h * 0.67f)
                        val p3 = Offset(w * 0.50f, h * 0.44f)
                        val p4 = Offset(w * 0.62f, h * 0.67f)
                        val p5 = Offset(w * 0.74f, h * 0.34f)

                        onDrawBehind {
                            drawCircle(brush = discBrush, radius = w * 0.44f)
                            drawCircle(brush = silverBlackRim, radius = w * 0.46f, style = rimStroke)
                            drawLine(inkColor, p1, p2, lineStrokeWidth, StrokeCap.Round)
                            drawLine(tertiaryColor, p2, p3, lineStrokeWidth, StrokeCap.Round)
                            drawLine(tertiaryColor, p3, p4, lineStrokeWidth, StrokeCap.Round)
                            drawLine(inkColor, p4, p5, lineStrokeWidth, StrokeCap.Round)
                        }
                    }
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {
                        append("WALL")
                        withStyle(SpanStyle(color = ChromeSilver)) {
                            append("X")
                        }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.vault_logo_subtitle),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidGlassSurface(
                style = LiquidGlassStyle.FROST_WHITE,
                shape = RoundedCornerShape(28.dp),
                cornerRadius = 28.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    LiquidGlassButton(
                        onClick = { startGoogleSignIn() },
                        enabled = !isSigningIn,
                        style = LiquidGlassStyle.MINT_PRIMARY,
                        shape = CircleShape,
                        cornerRadius = 28.dp,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auth_signing_in),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.action_sign_in_google),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
