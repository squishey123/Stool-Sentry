package com.squishey.stoolsentry.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.ThroneTopBar
import com.squishey.stoolsentry.ui.theme.Throne

@Composable
fun LicenseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val license = remember(context) {
        runCatching {
            context.assets.open("LICENSE.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
    val sqlCipherLicense = remember(context) {
        runCatching {
            context.assets.open("SQLCIPHER_LICENSE.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { ThroneTopBar(title = stringResource(R.string.settings_license), onBack = onBack) },
    ) { padding ->
        ScreenBody(Modifier.padding(padding)) {
            SelectionContainer {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.license_copyright),
                        style = MaterialTheme.typography.titleMedium,
                        color = Throne.Porcelain,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.license_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Throne.TextMuted,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = license ?: stringResource(R.string.license_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = Throne.Porcelain.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = stringResource(R.string.license_sqlcipher_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Throne.TextMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = sqlCipherLicense ?: stringResource(R.string.license_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = Throne.Porcelain.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}
