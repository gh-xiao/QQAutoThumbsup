package com.xiao.qqautothumbsup

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastClickTime = 0L

    companion object {
        private val TAG: String = MainAccessibilityService::class.java.simpleName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (System.currentTimeMillis() - lastClickTime < 100) return
            lastClickTime = System.currentTimeMillis()
            /* 检查自动点赞是否已经开启 */
            val isAutoLikeEnable = SpUtil.instance.get(Constant.IS_AUTO_LIKE_ENABLE, false) == true
            if (!isAutoLikeEnable) return
            /* 检查无障碍服务是否正常启动 */
//            val am = getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return
//            if (!am.isEnabled) return
            /* 检查当前窗口的根节点是否为空 */
            val root = /*event?.source ?:*/ rootInActiveWindow ?: return
            /* 获取匹配的节点列表 */
            val nodes = root
                .findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/dyw")
                /* 过滤出可见和可点击的节点 */
                ?.filter { it.isVisibleToUser && it.isClickable }
                ?: return
            scope.launch {
                /* 清除空节点 并 模拟点击节点 */
                nodes.filterNotNull().forEach { node ->
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in scheduled task: " + e.message)
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}