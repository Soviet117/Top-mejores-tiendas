package com.example.topmejorestiendas.feature.auth.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.topmejorestiendas.core.network.EmailVerificationState
import com.example.topmejorestiendas.core.network.RucVerificationState

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailVerificationState by viewModel.emailVerificationState.collectAsState()
    val rucVerificationState by viewModel.rucVerificationState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isOwner by remember { mutableStateOf(false) }
    var ruc by remember { mutableStateOf("") }

    // Estado del diálogo de verificación de email
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }

    // Rastrear el email previo para invalidar verificación si cambia
    var lastVerifiedEmail by remember { mutableStateOf("") }

    // Navigation logic based on login/register success
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            if (user.isOwner) {
                onNavigateToDashboard()
            } else {
                onNavigateToHome()
            }
        }
    }

    // Cuando el código se envía exitosamente, mostrar el diálogo OTP
    LaunchedEffect(emailVerificationState) {
        if (emailVerificationState is EmailVerificationState.CodeSent) {
            showOtpDialog = true
        }
    }

    // Verificación automática de RUC cuando tiene 11 dígitos
    LaunchedEffect(ruc) {
        if (ruc.length == 11 && ruc.all { it.isDigit() }) {
            viewModel.verifyRuc(ruc)
        } else if (ruc.length < 11) {
            viewModel.resetRucVerification()
        }
    }

    // ── Diálogo de verificación OTP ──────────────────────────────────────
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Verificar Correo",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Ingresa el código de 6 dígitos enviado a:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        email,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                otpInput = it
                            }
                        },
                        label = { Text("Código OTP") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.verifyEmailCode(otpInput, email)
                                if (viewModel.isEmailVerified(email)) {
                                    showOtpDialog = false
                                    lastVerifiedEmail = email
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Estado de error en verificación
                    val currentEmailState = emailVerificationState
                    if (currentEmailState is EmailVerificationState.Error) {
                        Text(
                            text = currentEmailState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        viewModel.sendVerificationEmail(email)
                        otpInput = ""
                    }) {
                        Text("¿No recibiste el código? Reenviar")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyEmailCode(otpInput, email)
                    },
                    enabled = otpInput.length == 6
                ) {
                    Text("Verificar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Cerrar diálogo cuando se verifica exitosamente
    LaunchedEffect(emailVerificationState) {
        if (emailVerificationState is EmailVerificationState.Verified) {
            showOtpDialog = false
            lastVerifiedEmail = email
        }
    }

    // ── Contenido Principal ──────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Únete a Top Mejores Tiendas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // ── Nombre ──
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; viewModel.clearError() },
            label = { Text("Nombre Completo") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Email con botón de verificación ──
        val isEmailCurrentlyVerified = viewModel.isEmailVerified(email)

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                viewModel.clearError()
                // Si el email cambia respecto al verificado, resetear verificación
                if (it != lastVerifiedEmail && isEmailCurrentlyVerified) {
                    viewModel.resetEmailVerification()
                }
            },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            trailingIcon = {
                when {
                    isEmailCurrentlyVerified -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    emailVerificationState is EmailVerificationState.Sending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = emailVerificationState is EmailVerificationState.Error && !showOtpDialog,
            enabled = !isEmailCurrentlyVerified
        )

        // Botón de enviar código / estado de verificación
        AnimatedVisibility(visible = email.isNotBlank() && !isEmailCurrentlyVerified) {
            Button(
                onClick = { viewModel.sendVerificationEmail(email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = emailVerificationState !is EmailVerificationState.Sending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                when (emailVerificationState) {
                    is EmailVerificationState.Sending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviando código...")
                    }
                    else -> {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar código de verificación")
                    }
                }
            }
        }

        // Badge de email verificado
        AnimatedVisibility(visible = isEmailCurrentlyVerified) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Correo electrónico verificado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error de email (fuera del diálogo)
        val emailErr = emailVerificationState
        if (emailErr is EmailVerificationState.Error && !showOtpDialog) {
            Text(
                text = emailErr.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Teléfono ──
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; viewModel.clearError() },
            label = { Text("Teléfono") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Contraseña ──
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Switch "Soy Dueño" ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Soy Dueño de un Local", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Activa esto para registrar y gestionar tu propio negocio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isOwner,
                    onCheckedChange = {
                        isOwner = it
                        if (!it) {
                            ruc = ""
                            viewModel.resetRucVerification()
                        }
                    }
                )
            }
        }

        // ── Sección RUC (solo si es dueño) ──
        AnimatedVisibility(
            visible = isOwner,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Verificación SUNAT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val isRucCurrentlyVerified = viewModel.isRucVerified(ruc)

                OutlinedTextField(
                    value = ruc,
                    onValueChange = {
                        if (it.length <= 11 && it.all { c -> c.isDigit() }) {
                            ruc = it
                            viewModel.clearError()
                        }
                    },
                    label = { Text("RUC del Negocio (11 dígitos)") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    trailingIcon = {
                        when (rucVerificationState) {
                            is RucVerificationState.Verifying -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is RucVerificationState.Verified -> {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "RUC Verificado",
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                            is RucVerificationState.Invalid -> {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "RUC Inválido",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            is RucVerificationState.Error -> {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = rucVerificationState is RucVerificationState.Invalid ||
                            rucVerificationState is RucVerificationState.Error,
                    enabled = !isRucCurrentlyVerified,
                    supportingText = {
                        if (ruc.isNotEmpty() && ruc.length < 11) {
                            Text("${ruc.length}/11 dígitos")
                        }
                    }
                )

                // ── Card de resultado SUNAT ──
                AnimatedVisibility(
                    visible = rucVerificationState is RucVerificationState.Verified ||
                            rucVerificationState is RucVerificationState.Invalid,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    when (val state = rucVerificationState) {
                        is RucVerificationState.Verified -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "RUC Verificado en SUNAT",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    SunatDetailRow("Razón Social", state.response.razonSocial)
                                    SunatDetailRow("RUC", state.response.ruc)
                                    SunatDetailRow("Estado", state.response.estado)
                                    SunatDetailRow("Condición", state.response.condicion)
                                    if (state.response.direccionCompleta.isNotBlank()) {
                                        SunatDetailRow("Dirección Fiscal", state.response.direccionCompleta)
                                    }
                                }
                            }
                        }
                        is RucVerificationState.Invalid -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "RUC No Válido",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = state.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    if (state.response != null) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        SunatDetailRow("Razón Social", state.response.razonSocial)
                                        SunatDetailRow("Estado", state.response.estado)
                                        SunatDetailRow("Condición", state.response.condicion)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }

                // Error de RUC
                val rucErr = rucVerificationState
                if (rucErr is RucVerificationState.Error) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rucErr.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.verifyRuc(ruc) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                // Consultando SUNAT
                AnimatedVisibility(visible = rucVerificationState is RucVerificationState.Verifying) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Consultando base de datos SUNAT...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Botón de Registro ──
        val canRegister = remember(name, email, password, isOwner, ruc, emailVerificationState, rucVerificationState) {
            val basicFieldsFilled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
            val emailVerified = emailVerificationState is EmailVerificationState.Verified
            val rucOk = if (isOwner) rucVerificationState is RucVerificationState.Verified else true
            basicFieldsFilled && emailVerified && rucOk
        }

        Button(
            onClick = { viewModel.register(name, email, phone, password, isOwner, ruc) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = canRegister && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Registrarse")
            }
        }

        // Info de requisitos faltantes
        if (!canRegister && name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (emailVerificationState !is EmailVerificationState.Verified) {
                    RequirementRow(text = "Verificar correo electrónico", isMet = false)
                }
                if (isOwner && rucVerificationState !is RucVerificationState.Verified) {
                    RequirementRow(text = "Verificar RUC en SUNAT", isMet = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Componentes auxiliares
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SunatDetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.width(110.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RequirementRow(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
