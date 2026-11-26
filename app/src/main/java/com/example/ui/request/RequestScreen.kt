package com.example.ui.request

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.StoreInfo
import com.example.data.repository.StoreUiState
import com.example.ui.StoreViewModel
import com.example.ui.theme.AppButtonShape
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.onSurfaceMuted
import com.example.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val storeInfo = (uiState as? StoreUiState.Success)?.store ?: StoreInfo()
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    var appName by remember { mutableStateOf("") }
    var appDetails by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userContact by remember { mutableStateOf("") }

    var appNameError by remember { mutableStateOf<String?>(null) }
    var appDetailsError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (appName.isBlank()) {
            appNameError = context.getString(R.string.error_app_name_required)
            isValid = false
        } else {
            appNameError = null
        }

        if (appDetails.trim().length < 10) {
            appDetailsError = context.getString(R.string.error_app_desc_min)
            isValid = false
        } else {
            appDetailsError = null
        }
        return isValid
    }

    fun buildMessage(): String {
        val fromName = userName.ifBlank { "Anonymous" }
        val contact = userContact.ifBlank { "Not provided" }
        return """
            New APK request - RORAX App Store

            App name: ${appName.trim()}
            Details: ${appDetails.trim()}
            From: $fromName
            Contact: $contact
        """.trimIndent()
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("APK Request", text)
        clipboard.setPrimaryClip(clip)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.request_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("request_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.lg,
                    end = spacing.lg,
                    top = spacing.sm,
                    bottom = spacing.lg
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline),
                        AppCardShape
                    )
                    .padding(spacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = {
                            appName = it
                            if (appNameError != null) appNameError = null
                        },
                        label = { Text(stringResource(R.string.field_app_name)) },
                        singleLine = true,
                        isError = appNameError != null,
                        supportingText = appNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_app_name_input")
                    )

                    OutlinedTextField(
                        value = appDetails,
                        onValueChange = {
                            appDetails = it
                            if (appDetailsError != null) appDetailsError = null
                        },
                        label = { Text(stringResource(R.string.field_app_desc)) },
                        minLines = 4,
                        maxLines = 6,
                        isError = appDetailsError != null,
                        supportingText = appDetailsError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_app_desc_input")
                    )

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text(stringResource(R.string.field_user_name)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_user_name_input")
                    )

                    OutlinedTextField(
                        value = userContact,
                        onValueChange = { userContact = it },
                        label = { Text(stringResource(R.string.field_user_contact)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_contact_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            if (storeInfo.telegram.isNotBlank()) {
                Button(
                    onClick = {
                        if (validate()) {
                            val msg = buildMessage()
                            copyToClipboard(msg)
                            Toast.makeText(context, context.getString(R.string.msg_copied_telegram), Toast.LENGTH_SHORT).show()
                            openLink(context, storeInfo.telegram)
                        }
                    },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.buttonHeight)
                        .testTag("request_send_telegram")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(dimensions.iconAction)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(stringResource(R.string.btn_send_telegram), style = MaterialTheme.typography.labelLarge)
                }
            }

            if (storeInfo.email.isNotBlank()) {
                Button(
                    onClick = {
                        if (validate()) {
                            val msg = buildMessage()
                            sendRequestEmail(context, storeInfo.email, appName.trim(), msg)
                        }
                    },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.buttonHeight)
                        .testTag("request_send_email")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconAction)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(stringResource(R.string.btn_send_email), style = MaterialTheme.typography.labelLarge)
                }
            }

            if (storeInfo.facebook.isNotBlank()) {
                Button(
                    onClick = {
                        if (validate()) {
                            val msg = buildMessage()
                            copyToClipboard(msg)
                            Toast.makeText(context, context.getString(R.string.msg_copied_facebook), Toast.LENGTH_SHORT).show()
                            openLink(context, storeInfo.facebook)
                        }
                    },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.buttonHeight)
                        .testTag("request_send_facebook")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(dimensions.iconAction)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(stringResource(R.string.btn_send_facebook), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun openLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_to_handle), Toast.LENGTH_SHORT).show()
    }
}

private fun sendRequestEmail(context: Context, email: String, appName: String, body: String) {
    try {
        val uri = Uri.parse("mailto:$email?subject=" + Uri.encode("APK Request: $appName") + "&body=" + Uri.encode(body))
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_to_handle), Toast.LENGTH_SHORT).show()
    }
}
