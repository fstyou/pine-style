package cc.fstu.pinestyle.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.fstu.pinestyle.R
import cc.fstu.pinestyle.data.PostureStatistics
import cc.fstu.pinestyle.ui.shared.BottomNavigationBar

// 统计页面屏幕
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    statistics : PostureStatistics?,
    onNavigateToHome : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onNavigateToHelp : () -> Unit,
    onUpdateData : () -> Unit = {},
                    ) {
    // 从统计数据获取数据并格式化
    val totalTimeMs = statistics?.totalDetectionTime ?: 0L
    val goodTimeMs = statistics?.goodPostureTime ?: 0L
    val crossLegTimeMs = statistics?.crossLegTime ?: 0L
    val hunchbackTimeMs = statistics?.hunchbackTime ?: 0L
    val noPersonTimeMs = statistics?.noPersonTime ?: 0L
    val scoreValue = statistics?.calculateScore() ?: 0f
    val evaluationText = when {
        scoreValue >= 90 -> stringResource(R.string.excellent_evaluation)
        scoreValue >= 70 -> stringResource(R.string.good_evaluation)
        scoreValue >= 50 -> stringResource(R.string.average_evaluation)
        else -> stringResource(R.string.poor_evaluation)
    }
    val hasData = statistics != null
    // 计算总时长
    val totalSessionTimeMs = totalTimeMs + noPersonTimeMs
    // 计算良好坐姿比例
    val goodRatio = if (totalTimeMs > 0) {
        (goodTimeMs.toFloat() / totalTimeMs.toFloat() * 100).toInt()
    }
    else {
        0
    }
    // 格式化时间
    val totalTime = if (hasData) {
        formatTime(totalTimeMs)
    }
    else {
        stringResource(R.string.time_zero_seconds)
    }
    val goodTime = if (hasData) {
        formatTime(goodTimeMs)
    }
    else {
        stringResource(R.string.time_zero_seconds)
    }
    val crossLegTime = if (hasData) {
        formatTime(crossLegTimeMs)
    }
    else {
        stringResource(R.string.time_zero_seconds)
    }
    val hunchbackTime = if (hasData) {
        formatTime(hunchbackTimeMs)
    }
    else {
        stringResource(R.string.time_zero_seconds)
    }
    val noPersonTime = if (hasData) {
        formatTime(noPersonTimeMs)
    }
    else {
        stringResource(R.string.time_zero_seconds)
    }
    val totalSessionTime = if (hasData) {
        formatTime(totalSessionTimeMs)
    }
    else {
        stringResource(R.string.time_zero_seconds)
    }
    val score = if (hasData) {
        String.format(stringResource(R.string.score_format_decimal), scoreValue)
    }
    else {
        stringResource(R.string.score_zero)
    }
    val evaluation = if (hasData && evaluationText.isNotEmpty()) {
        evaluationText
    }
    else {
        stringResource(R.string.no_data)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) }
                     )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "stats",
                onHomeClick = onNavigateToHome,
                onStatsClick = { },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
            Spacer(modifier = Modifier.height(8.dp))
            // 统计概览卡片
            StatisticsOverviewCard(
                goodTime = goodTime,
                totalTime = totalTime,
                goodRatio = goodRatio,
                score = score
                                  )
            // 详细统计数据卡片
            DetailedStatisticsCard(
                crossLegTime = crossLegTime,
                hunchbackTime = hunchbackTime,
                noPersonTime = noPersonTime,
                totalSessionTime = totalSessionTime
                                  )
            // 坐姿评价卡片
            EvaluationCard(evaluation = evaluation)
            // 更新数据按钮
            Button(
                onClick = {
                    android.util.Log.d("StatisticsScreen", "点击更新数据按钮")
                    onUpdateData()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                  ) {
                Text(stringResource(R.string.update_data))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 统计概览卡片
@Composable
fun StatisticsOverviewCard(
    goodTime : String,
    totalTime : String,
    goodRatio : Int,
    score : String,
                          ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
              ) {
            // 标题
            Text(
                text = stringResource(R.string.stats_overview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            Spacer(modifier = Modifier.height(16.dp))
            // 良好坐姿比例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
               ) {
                Column {
                    Text(
                        text = stringResource(R.string.good_posture_ratio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = "$goodRatio%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                                                                            ),
                        color = MaterialTheme.colorScheme.primary
                        )
                }
                Column {
                    Text(
                        text = stringResource(R.string.posture_score),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = score,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                                                                            ),
                        color = MaterialTheme.colorScheme.secondary
                        )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // 进度条
            LinearProgressIndicator(
                progress = { goodRatio.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                   )
            Spacer(modifier = Modifier.height(16.dp))
            // 时间统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
               ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                    Text(
                        text = stringResource(R.string.good_posture),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = goodTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                        )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                    Text(
                        text = stringResource(R.string.total_detection_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = totalTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                        )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stats_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                )
        }
    }
}

// 详细统计数据卡片
@Composable
fun DetailedStatisticsCard(
    crossLegTime : String,
    hunchbackTime : String,
    noPersonTime : String,
    totalSessionTime : String,
                          ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
              ) {
            // 标题
            Text(
                text = stringResource(R.string.detailed_stats),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            
            Spacer(modifier = Modifier.height(12.dp))
            // 不健康姿势统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
               ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.icon_cross_leg),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                        )
                    Text(
                        text = stringResource(R.string.cross_leg_posture),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = crossLegTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                        )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = stringResource(R.string.icon_hunchback),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                        )
                    Text(
                        text = stringResource(R.string.hunchback_posture),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = hunchbackTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                        )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                    Icon(
                        imageVector = Icons.Filled.PersonOff,
                        contentDescription = stringResource(R.string.icon_no_person),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                        )
                    Text(
                        text = stringResource(R.string.no_person_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    Text(
                        text = noPersonTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                        )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // 总会话时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
               ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = stringResource(R.string.icon_total_session),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                    )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.total_session_time) + ": ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                Text(
                    text = totalSessionTime,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                    )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detection_time_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                )
        }
    }
}

// 坐姿评价卡片
@Composable
fun EvaluationCard(evaluation : String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
              ) {
            Text(
                text = stringResource(R.string.posture_evaluation),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = evaluation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
                )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// 格式化时间
@Composable
private fun formatTime(milliseconds : Long) : String {
    val totalSeconds = milliseconds / 1000
    return if (totalSeconds >= 60) {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format(
            stringResource(R.string.time_format_minutes_seconds),
            minutes,
            seconds
                     )
    }
    else {
        String.format(
            stringResource(R.string.time_format_seconds_only),
            totalSeconds
                     )
    }
}
