/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.fstu.pinestyle.data.PostureStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

// 统计页面ViewModel
// 管理坐姿统计数据
class StatisticsViewModel : ViewModel() {
    // 当前会话统计数据
    private val _currentSession = MutableStateFlow<PostureStatistics?>(null)
    val currentSession : StateFlow<PostureStatistics?> = _currentSession
    
    init {
        // 初始化时创建新的会话
        startNewSession()
    }
    
    // 开始新的检测会话
    fun startNewSession() {
        viewModelScope.launch {
            val newSession = PostureStatistics(
                sessionId = UUID.randomUUID().toString(),
                startTime = Date()
                                              )
            _currentSession.value = newSession
        }
    }
    
    // 从MainViewModel接收实时统计数据
    fun updateFromMainViewModel(
        goodDuration : Long,
        totalDetectionDuration : Long,
        crossLegDuration : Long = 0,
        hunchbackDuration : Long = 0,
        noPersonDuration : Long = 0,
                               ) {
        viewModelScope.launch {
            val currentSession = _currentSession.value
            if (currentSession == null) {
                android.util.Log.w("StatisticsViewModel", "当前会话为null")
                return@launch
            }
            // 检查数据是否真正变化，避免不必要的UI更新
            if (currentSession.goodPostureTime != goodDuration ||
                currentSession.totalDetectionTime != totalDetectionDuration ||
                currentSession.crossLegTime != crossLegDuration ||
                currentSession.hunchbackTime != hunchbackDuration ||
                currentSession.noPersonTime != noPersonDuration) {
                // 创建新的会话对象（避免直接修改）
                val updatedSession = currentSession.copy(
                    goodPostureTime = goodDuration,
                    totalDetectionTime = totalDetectionDuration,
                    crossLegTime = crossLegDuration,
                    hunchbackTime = hunchbackDuration,
                    noPersonTime = noPersonDuration
                                                        )
                _currentSession.value = updatedSession
            }
        }
    }
    
    // 手动触发数据更新
    fun triggerDataUpdate(
        goodDuration : Long,
        totalDetectionDuration : Long,
        crossLegDuration : Long = 0,
        hunchbackDuration : Long = 0,
        noPersonDuration : Long = 0,
                         ) {
        updateFromMainViewModel(
            goodDuration,
            totalDetectionDuration,
            crossLegDuration,
            hunchbackDuration,
            noPersonDuration
                               )
    }
}
