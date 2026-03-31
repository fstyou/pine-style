/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.fstu.pinestyle.R
import cc.fstu.pinestyle.ui.shared.BottomNavigationBar

// 帮助页面屏幕
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateToHome : () -> Unit,
    onNavigateToStats : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onNavigateToHelp : () -> Unit,
              ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) }
                     )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "help",
                onHomeClick = onNavigateToHome,
                onStatsClick = onNavigateToStats,
                onSettingsClick = onNavigateToSettings,
                onHelpClick = onNavigateToHelp
                               )
        }
            ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
            // 顶部间距
            Spacer(modifier = Modifier.height(8.dp))
            // 特点部分
            HelpSection(
                title = stringResource(R.string.help_features),
                icon = Icons.Filled.Star,
                iconDescription = stringResource(R.string.icon_features)
                       ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                    HelpItem(
                        title = stringResource(R.string.help_feature1_title),
                        description = stringResource(R.string.help_feature1_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_feature2_title),
                        description = stringResource(R.string.help_feature2_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_feature3_title),
                        description = stringResource(R.string.help_feature3_desc)
                            )
                }
            }
            // 工作逻辑部分
            HelpSection(
                title = stringResource(R.string.help_logic),
                icon = Icons.Filled.Info,
                iconDescription = stringResource(R.string.icon_logic)
                       ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                    HelpItem(
                        title = stringResource(R.string.help_logic1_title),
                        description = stringResource(R.string.help_logic1_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_logic2_title),
                        description = stringResource(R.string.help_logic2_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_logic3_title),
                        description = stringResource(R.string.help_logic3_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_logic4_title),
                        description = stringResource(R.string.help_logic4_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_logic5_title),
                        description = stringResource(R.string.help_logic5_desc)
                            )
                }
            }
            // 使用说明部分
            HelpSection(
                title = stringResource(R.string.help_usage),
                icon = Icons.AutoMirrored.Filled.Help,
                iconDescription = stringResource(R.string.icon_usage)
                       ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                    HelpItem(
                        title = stringResource(R.string.help_usage1_title),
                        description = stringResource(R.string.help_usage1_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_usage2_title),
                        description = stringResource(R.string.help_usage2_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_usage3_title),
                        description = stringResource(R.string.help_usage3_desc)
                            )
                    
                    HelpItem(
                        title = stringResource(R.string.help_usage4_title),
                        description = stringResource(R.string.help_usage4_desc)
                            )
                }
            }
            // 版权部分
            HelpSection(
                title = stringResource(R.string.help_copyright),
                icon = Icons.Filled.Copyright,
                iconDescription = stringResource(R.string.icon_copyright)
                       ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                    Text(
                        text = stringResource(R.string.license_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                        )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ai_source_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                        )
                    HelpItem(
                        title = stringResource(R.string.help_linyi_project),
                        description = stringResource(R.string.help_linyi_url)
                            )
                    HelpItem(
                        title = stringResource(R.string.help_tensorflow_project),
                        description = stringResource(R.string.help_tensorflow_url)
                            )
                    Text(
                        text = stringResource(R.string.license_apache_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                        )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// 帮助部分容器
@Composable
fun HelpSection(
    title : String,
    icon : androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription : String,
    content : @Composable () -> Unit,
               ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
              ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
               ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = MaterialTheme.colorScheme.primary
                    )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                                                                     ),
                    color = MaterialTheme.colorScheme.onSurface
                    )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// 帮助项目
@Composable
fun HelpItem(
    title : String,
    description : String,
            ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
                                                           ),
            color = MaterialTheme.colorScheme.primary
            )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}
