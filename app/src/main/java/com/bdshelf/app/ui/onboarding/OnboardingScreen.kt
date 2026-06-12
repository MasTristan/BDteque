package com.bdshelf.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R

@Composable
fun OnboardingScreen(
    onImportComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.importComplete) {
        if (uiState.importComplete) onImportComplete()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.onboarding_name_label)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::onSubmit,
                enabled = uiState.name.isNotBlank() && !uiState.isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            ) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_submit_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
