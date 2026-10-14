package com.squishey.stoolsentry.ui.welcome

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.ui.components.BrandMark
import com.squishey.stoolsentry.ui.components.CardDivider
import com.squishey.stoolsentry.ui.components.GoldButton
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.SectionLabel
import com.squishey.stoolsentry.ui.components.ThroneCard
import com.squishey.stoolsentry.ui.theme.QuoteStyle
import com.squishey.stoolsentry.ui.theme.Throne

@Composable
fun WelcomeScreen(onAccept: () -> Unit) {
    ScreenBody(
        Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            BrandMark(width = 84.dp, glow = true)
            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.welcome_eyebrow))
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = Throne.Porcelain,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = QuoteStyle,
                color = Throne.TextMuted,
            )
            Spacer(Modifier.height(28.dp))
            ThroneCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Promise(R.drawable.ic_shield, R.string.welcome_private_title, R.string.welcome_private_body)
                CardDivider()
                Promise(R.drawable.ic_info, R.string.welcome_doctor_title, R.string.welcome_doctor_body)
                CardDivider()
                Promise(R.drawable.ic_download, R.string.welcome_backup_title, R.string.welcome_backup_body)
            }
            Spacer(Modifier.height(28.dp))
            GoldButton(
                text = stringResource(R.string.welcome_accept),
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Promise(icon: Int, title: Int, body: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Throne.CardRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Throne.Gold,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = Throne.Porcelain)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Throne.TextMuted)
        }
    }
}
