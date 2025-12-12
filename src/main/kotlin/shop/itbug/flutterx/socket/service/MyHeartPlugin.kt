package shop.itbug.flutterx.socket.service

import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.extension.plugins.AbstractPlugin
import org.smartboot.socket.timer.HashedWheelTimer
import org.smartboot.socket.transport.AioSession
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HeartPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 */

/**
 * 心跳插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
abstract class MyHeartPlugin<T>(heartRate: Int, timeout: Int, timeUnit: TimeUnit, timeoutCallback: TimeoutCallback) :
    AbstractPlugin<T>() {
    private val sessionMap: MutableMap<AioSession, Long> = HashMap()

    /**
     * 心跳频率
     */
    private val heartRate: Long

    /**
     * 在超时时间内未收到消息,关闭连接。
     */
    private val timeout: Long
    private val timeoutCallback: TimeoutCallback

    /**
     * 心跳插件
     *
     * @param heartRate 心跳触发频率
     * @param timeUnit  heatRate单位
     */
    constructor(heartRate: Int, timeUnit: TimeUnit) : this(heartRate, 0, timeUnit)

    /**
     * 心跳插件
     *
     *
     * 心跳插件在断网场景可能会触发TCP Retransmission,导致无法感知到网络实际状态,可通过设置timeout关闭连接
     *
     *
     * @param heartRate 心跳触发频率
     * @param timeout   消息超时时间
     * @param unit      时间单位
     */
    constructor(heartRate: Int, timeout: Int, unit: TimeUnit) : this(
        heartRate,
        timeout,
        unit,
        DEFAULT_TIMEOUT_CALLBACK
    )

    /**
     * 心跳插件
     *
     *
     * 心跳插件在断网场景可能会触发TCP Retransmission,导致无法感知到网络实际状态,可通过设置timeout关闭连接
     *
     *
     * @param heartRate 心跳触发频率
     * @param timeout   消息超时时间
     */
    init {
        require(timeout !in 1..heartRate) { "heartRate must little then timeout" }
        this.heartRate = timeUnit.toMillis(heartRate.toLong())
        this.timeout = timeUnit.toMillis(timeout.toLong())
        this.timeoutCallback = timeoutCallback
    }

    override fun preProcess(session: AioSession, t: T?): Boolean {
        sessionMap[session] = System.currentTimeMillis()
        //是否心跳响应消息
        return !isHeartMessage(session, t)
    }

    override fun stateEvent(stateMachineEnum: StateMachineEnum?, session: AioSession?, throwable: Throwable?) {
        when (stateMachineEnum) {
            StateMachineEnum.NEW_SESSION -> {
                session?.let {
                    sessionMap[session] = System.currentTimeMillis()
                    registerHeart(session, heartRate)
                }
            }

            StateMachineEnum.SESSION_CLOSED ->                 //移除心跳监测
                sessionMap.remove(session)

            else -> {}
        }
    }

    /**
     * 自定义心跳消息并发送
     *
     * @param session
     * @throws IOException
     */
    @Throws(IOException::class)
    abstract fun sendHeartRequest(session: AioSession?)

    /**
     * 判断当前收到的消息是否为心跳消息。
     * 心跳请求消息与响应消息可能相同，也可能不同，因实际场景而异，故接口定义不做区分。
     *
     * @param session
     * @param msg
     * @return
     */
    abstract fun isHeartMessage(session: AioSession?, msg: T?): Boolean

    private fun registerHeart(session: AioSession, heartRate: Long) {
        if (heartRate <= 0) {
            return
        }
        HashedWheelTimer.DEFAULT_TIMER.schedule(object : TimerTask() {
            override fun run() {
                if (session.isInvalid) {
                    sessionMap.remove(session)
                    return
                }
                var lastTime = sessionMap[session]
                if (lastTime == null) {
                    lastTime = System.currentTimeMillis()
                    sessionMap[session] = lastTime
                }
                val current = System.currentTimeMillis()
                //超时未收到消息，关闭连接
                if (timeout > 0 && (current - lastTime) > timeout) {
                    timeoutCallback.callback(session, lastTime)
                } else if (current - lastTime > heartRate) {
                    try {
                        sendHeartRequest(session)
                        session.writeBuffer().flush()
                    } catch (e: IOException) {
                        session.close(true)
                    }
                }
                registerHeart(session, heartRate)
            }
        }, heartRate, TimeUnit.MILLISECONDS)
    }

    interface TimeoutCallback {
        fun callback(session: AioSession?, lastTime: Long)
    }

    companion object {
        private val DEFAULT_TIMEOUT_CALLBACK: TimeoutCallback = object : TimeoutCallback {

            override fun callback(session: AioSession?, lastTime: Long) {
                session?.close(true)
            }
        }
    }
}