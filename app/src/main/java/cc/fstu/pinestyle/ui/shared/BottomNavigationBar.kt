package cc.fstu.pinestyle.ui.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cc.fstu.pinestyle.R

// 底部导航栏组件
@Composable
fun BottomNavigationBar(
    currentRoute : String,
    onHomeClick : () -> Unit,
    onStatsClick : () -> Unit,
    onSettingsClick : () -> Unit,
    onHelpClick : () -> Unit,
                       ) {
    androidx.compose.material3.BottomAppBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onHomeClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Camera,
                    contentDescription = stringResource(R.string.icon_home)
                    )
            },
            label = { Text(stringResource(R.string.nav_home)) }
                         )
        NavigationBarItem(
            selected = currentRoute == "stats",
            onClick = onStatsClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.InsertChart,
                    contentDescription = stringResource(R.string.icon_stats)
                    )
            },
            label = { Text(stringResource(R.string.nav_stats)) }
                         )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = onSettingsClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.icon_settings)
                    )
            },
            label = { Text(stringResource(R.string.nav_settings)) }
                         )
        NavigationBarItem(
            selected = currentRoute == "help",
            onClick = onHelpClick,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = stringResource(R.string.icon_help)
                    )
            },
            label = { Text(stringResource(R.string.nav_help)) }
                         )
    }
}
