/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import cc.fstu.pinestyle.data.Device
import cc.fstu.pinestyle.data.Person

// 安全的MoveNet包装器
// 用于捕获MoveNet运行时异常，避免应用崩溃
class SafeMoveNet private constructor(
    private val moveNet : MoveNet,
    private val context : Context,
    private val originalDevice : Device,
    private val onFallbackToCpu : ((Device) -> Unit)? = null,
                                     ) : PoseDetector {
    
    companion object {
        private const val TAG = "SafeMoveNet"
        
        fun create(
            context : Context,
            device : Device,
            onFallbackToCpu : ((Device) -> Unit)? = null,
                  ) : SafeMoveNet {
            // 尝试创建指定设备的检测器
            val moveNet = try {
                MoveNet.create(context, device)
            }
            catch (e : Throwable) {
                // 如果创建失败（例如GPU/NNAPI不支持），尝试回退到CPU
                if (device != Device.CPU) {
                    try {
                        val cpuDetector = MoveNet.create(context, Device.CPU)
                        // 通知上层已回退到CPU
                        onFallbackToCpu?.invoke(device)
                        cpuDetector
                    }
                    catch (cpuE : Throwable) {
                        throw cpuE
                    }
                }
                else {
                    throw e
                }
            }
            return SafeMoveNet(moveNet, context, device, onFallbackToCpu)
        }
    }
    
    // 是否已经回退到CPU模式
    private var hasFallenBackToCpu = false
    
    // 回退后的CPU检测器
    private var fallbackDetector : MoveNet? = null
    
    // 估计姿态
    // 捕获所有异常，避免崩溃
    override fun estimatePoses(bitmap : Bitmap) : List<Person> {
        return try {
            // 如果已经回退到CPU，使用回退检测器
            if (hasFallenBackToCpu && fallbackDetector != null) {
                fallbackDetector !!.estimatePoses(bitmap)
            }
            else {
                moveNet.estimatePoses(bitmap)
            }
        }
        catch (e : Throwable) {
            // 捕获所有异常，避免应用崩溃
            // 如果是GPU或NNAPI不支持的操作异常，尝试自动回退到CPU
            val isGpuError = e is IllegalArgumentException && e.message?.contains("GPU delegate") == true
            val isNnapiError = e is IllegalArgumentException && (
                    e.message?.contains("NNAPI") == true ||
                    e.message?.contains("Failed to apply delegate") == true ||
                    e.message?.contains("NN API") == true
                                                                )
            if ((isGpuError || isNnapiError) && ! hasFallenBackToCpu && originalDevice != Device.CPU) {
                try {
                    fallbackDetector = MoveNet.create(context, Device.CPU)
                    hasFallenBackToCpu = true
                    // 通知上层已回退到CPU
                    onFallbackToCpu?.invoke(originalDevice)
                    // 使用CPU检测器重新尝试
                    return fallbackDetector !!.estimatePoses(bitmap)
                }
                catch (fallbackE : Throwable) {
                    Log.e(TAG, "回退到CPU也失败", fallbackE)
                }
            }
            // 其他异常返回空列表
            emptyList()
        }
    }
    
    // 获取最后一次推理时间（纳秒）
    override fun lastInferenceTimeNanos() : Long {
        return if (hasFallenBackToCpu && fallbackDetector != null) {
            fallbackDetector !!.lastInferenceTimeNanos()
        }
        else {
            moveNet.lastInferenceTimeNanos()
        }
    }
    
    // 关闭资源
    override fun close() {
        moveNet.close()
        fallbackDetector?.close()
    }
    
    // 检查是否已经回退到CPU模式
    fun hasFallenBackToCpu() : Boolean = hasFallenBackToCpu
    
}
