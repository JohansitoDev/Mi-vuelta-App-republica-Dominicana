package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.ui.theme.AppThemeMode

private enum class AuthFormMode {
  LOGIN,
  REGISTER,
}

@Composable
fun AuthScreen(
  themeMode: AppThemeMode,
  onToggleTheme: () -> Unit,
  onAuthSuccess: () -> Unit,
  onGuestMode: () -> Unit,
  onLocalSignIn: (email: String, password: String) -> Result<Unit>,
  onLocalRegister: (name: String, email: String, password: String) -> Result<Unit>,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var formMode by remember { mutableStateOf(AuthFormMode.LOGIN) }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isGoogleLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding()
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 500.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
          // Top Brand + Light/Black Mode Toggle (no card behind form)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (themeMode == AppThemeMode.BLACK) {
              Image(
                painter = painterResource(id = R.drawable.img_splash_brand),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(42.dp),
              )
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "MV",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onBackground,
                )
              }
            }

            Surface(
              onClick = onToggleTheme,
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(50),
              modifier = Modifier.testTag("auth_theme_toggle"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = if (themeMode == AppThemeMode.BLACK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                  contentDescription = "Cambiar tema",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = if (themeMode == AppThemeMode.BLACK) "Modo claro" else "Modo negro",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          // Heading
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = if (formMode == AuthFormMode.LOGIN) "Bienvenido de vuelta" else "Crea tu cuenta",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "No pagues más moras. Organiza tus pagos, recibos y avisos.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Clean underline mode tabs ("Iniciar sesión" | "Crear cuenta") with NO card behind
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
          ) {
            AuthTabItem(
              label = "Iniciar sesión",
              selected = formMode == AuthFormMode.LOGIN,
              onClick = {
                formMode = AuthFormMode.LOGIN
                errorMessage = null
              },
              testTag = "tab_login",
            )
            AuthTabItem(
              label = "Crear cuenta",
              selected = formMode == AuthFormMode.REGISTER,
              onClick = {
                formMode = AuthFormMode.REGISTER
                errorMessage = null
              },
              testTag = "tab_register",
            )
          }

          // Bare fields directly on background (NO Card/Surface behind fields)
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val fieldColors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.background,
              unfocusedContainerColor = MaterialTheme.colorScheme.background,
            )

            AnimatedVisibility(visible = formMode == AuthFormMode.REGISTER) {
              OutlinedTextField(
                value = fullName,
                onValueChange = {
                  fullName = it
                  errorMessage = null
                },
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_auth_name"),
              )
            }

            OutlinedTextField(
              value = email,
              onValueChange = {
                email = it
                errorMessage = null
              },
              label = { Text("Correo electrónico") },
              leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
              singleLine = true,
              shape = MaterialTheme.shapes.medium,
              colors = fieldColors,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_auth_email"),
            )

            OutlinedTextField(
              value = password,
              onValueChange = {
                password = it
                errorMessage = null
              },
              label = { Text("Contraseña") },
              leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Ver contraseña",
                  )
                }
              },
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = MaterialTheme.shapes.medium,
              colors = fieldColors,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_auth_password"),
            )

            if (errorMessage != null) {
              Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
              )
            }

            Button(
              onClick = {
                errorMessage = null
                val result = if (formMode == AuthFormMode.LOGIN) {
                  onLocalSignIn(email, password)
                } else {
                  onLocalRegister(fullName, email, password)
                }
                result.onSuccess {
                  onAuthSuccess()
                }.onFailure { err ->
                  errorMessage = err.localizedMessage ?: "No se pudo completar la operación."
                }
              },
              shape = MaterialTheme.shapes.medium,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_email_auth_submit"),
            ) {
              Text(
                text = if (formMode == AuthFormMode.LOGIN) "Iniciar sesión" else "Crear cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          // Divider
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
              text = "o continúa con",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
          }

          // Google Sign-In & Guest Mode
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = {
                errorMessage = null
                isGoogleLoading = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isGoogleLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isGoogleLoading = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = { isGoogleLoading = false },
                )
              },
              enabled = !isGoogleLoading,
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
            ) {
              if (isGoogleLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                )
              } else {
                Text(
                  text = stringResource(R.string.btn_google_signin),
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onBackground,
                )
              }
            }

            TextButton(
              onClick = onGuestMode,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_guest_mode"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PersonOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Entrar como invitado (sin Google)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun AuthTabItem(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp)
      .testTag(testTag),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box(
      modifier = Modifier
        .width(42.dp)
        .height(3.dp)
        .clip(RoundedCornerShape(50))
        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
    )
  }
}
