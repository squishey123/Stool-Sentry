package com.squishey.stoolsentry.ui.lock

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.ui.components.BrandMark
import com.squishey.stoolsentry.ui.components.GoldButton
import com.squishey.stoolsentry.ui.components.VersionTag
import com.squishey.stoolsentry.ui.components.throneBackground
import com.squishey.stoolsentry.ui.theme.Throne

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .throneBackground()
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(width = 96.dp, glow = true)
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Throne.Porcelain,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lock_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Throne.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            GoldButton(
                text = stringResource(R.string.lock_unlock),
                onClick = onUnlock,
                icon = R.drawable.ic_lock,
            )
        }
        VersionTag(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding(),
        )
    }
}
