package com.cau.medicheck.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * 로그인한 뒤에 보이는 첫 화면.
 *
 * 지금은 자리만 잡아 둔 임시 화면이다 (인사말 + 로그아웃 버튼).
 * 약 목록·오늘의 복약은 다음 작업에서 이 자리에 들어간다.
 */
@Composable
fun HomeScreen(user: User, onLogout: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.home_greeting, user.displayName),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onLogout) {
                Text(stringResource(R.string.action_logout))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MediCheckTheme(dynamicColor = false) {
        HomeScreen(
            user = User(id = 1, username = "suhwan", passwordHash = "", displayName = "Suhwan", createdAt = 0),
            onLogout = {}
        )
    }
}
