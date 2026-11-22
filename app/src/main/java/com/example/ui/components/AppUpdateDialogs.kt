package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.AppRelease

@Composable
fun UpdateAvailableDialog(
    release: AppRelease,
    currentVersionName: String,
    onUpdateNow: (AppRelease) -> Unit,
    onDismiss: () -> Unit
) {
    val canDismiss = !release.forceUpdate
    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss
        )
    ) {
        LiquidGlassSurface(
            style = LiquidGlassStyle.FROST_WHITE,
            shape = RoundedCornerShape(30.dp),
            cornerRadius = 30.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("update_available_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = stringResource(
                            R.string.update_dialog_version_jump,
                            currentVersionName,
                            release.versionName
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Release Notes Box
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_whats_new),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = release.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (release.forceUpdate) {
                    Text(
                        text = stringResource(R.string.update_dialog_required_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canDismiss) {
                        LiquidGlassButton(
                            onClick = onDismiss,
                            style = LiquidGlassStyle.FROST_WHITE,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("update_later_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_update_later),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    LiquidGlassButton(
                        onClick = { onUpdateNow(release) },
                        style = LiquidGlassStyle.MINT_PRIMARY,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(if (canDismiss) 1.45f else 1f)
                            .height(48.dp)
                            .testTag("update_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_update_now),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PublishUpdateDialog(
    currentVersionCode: Int,
    latestRelease: AppRelease?,
    isPublishing: Boolean,
    onDismiss: () -> Unit,
    onPublish: (
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String?,
        forceUpdate: Boolean
    ) -> Unit
) {
    val suggestedCode = remember(currentVersionCode, latestRelease?.versionCode) {
        maxOf(currentVersionCode, latestRelease?.versionCode ?: currentVersionCode) + 1
    }
    val suggestedName = remember(suggestedCode) {
        "1.${suggestedCode - 1}"
    }

    var versionNameText by rememberSaveable { mutableStateOf(suggestedName) }
    var versionCodeText by rememberSaveable { mutableStateOf(suggestedCode.toString()) }
    var releaseNotesText by rememberSaveable { mutableStateOf(latestRelease?.releaseNotes.orEmpty()) }
    var downloadUrlText by rememberSaveable { mutableStateOf(latestRelease?.downloadUrl.orEmpty()) }
    var forceUpdate by rememberSaveable { mutableStateOf(latestRelease?.forceUpdate ?: false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val errVersionName = stringResource(R.string.publish_validation_version_name)
    val errVersionCode = stringResource(R.string.publish_validation_version_code, currentVersionCode)
    val errNotes = stringResource(R.string.publish_validation_notes)
    val errUrl = stringResource(R.string.publish_validation_url)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary
    )

    Dialog(onDismissRequest = { if (!isPublishing) onDismiss() }) {
        LiquidGlassSurface(
            style = LiquidGlassStyle.FROST_WHITE,
            shape = RoundedCornerShape(30.dp),
            cornerRadius = 30.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_update_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Publish,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.publish_update_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.publish_update_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = versionNameText,
                        onValueChange = {
                            versionNameText = it.take(32)
                            validationError = null
                        },
                        label = { Text(stringResource(R.string.publish_version_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("publish_version_name_input")
                    )

                    OutlinedTextField(
                        value = versionCodeText,
                        onValueChange = { input ->
                            versionCodeText = input.filter { it.isDigit() }.take(6)
                            validationError = null
                        },
                        label = { Text(stringResource(R.string.publish_version_code_label, currentVersionCode)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("publish_version_code_input")
                    )
                }

                OutlinedTextField(
                    value = releaseNotesText,
                    onValueChange = {
                        releaseNotesText = it.take(500)
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.publish_notes_label)) },
                    placeholder = { Text(stringResource(R.string.publish_notes_placeholder)) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_notes_input")
                )

                OutlinedTextField(
                    value = downloadUrlText,
                    onValueChange = {
                        downloadUrlText = it.take(2048)
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.publish_url_label)) },
                    placeholder = { Text(stringResource(R.string.publish_url_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_url_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.publish_force_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = forceUpdate,
                        onCheckedChange = { forceUpdate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("publish_force_switch")
                    )
                }

                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassButton(
                        onClick = onDismiss,
                        enabled = !isPublishing,
                        style = LiquidGlassStyle.FROST_WHITE,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("publish_cancel_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    LiquidGlassButton(
                        onClick = {
                            val parsedCode = versionCodeText.toIntOrNull()
                            val cleanName = versionNameText.trim()
                            val cleanNotes = releaseNotesText.trim()
                            val cleanUrl = downloadUrlText.trim().takeIf { it.isNotEmpty() }

                            when {
                                cleanName.isEmpty() -> validationError = errVersionName
                                parsedCode == null || parsedCode <= currentVersionCode || parsedCode > 100000 -> {
                                    validationError = errVersionCode
                                }
                                cleanNotes.isEmpty() -> validationError = errNotes
                                cleanUrl != null && !cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://") -> {
                                    validationError = errUrl
                                }
                                else -> {
                                    validationError = null
                                    onPublish(parsedCode, cleanName, cleanNotes, cleanUrl, forceUpdate)
                                }
                            }
                        },
                        enabled = !isPublishing,
                        style = LiquidGlassStyle.MINT_PRIMARY,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("publish_submit_button")
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.publishing_update),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.action_publish_update),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
