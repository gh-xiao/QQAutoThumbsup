package com.xiao.qqautothumbsup;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainAccessibilityService extends AccessibilityService {
    private static final String TAG = MainAccessibilityService.class.getSimpleName();
    // 创建一个带定时器固定大小的线程池
    private static ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> future;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "MainAccessibilityService onCreate: 无障碍服务已启动");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        future = threadPool.scheduleAtFixedRate(() -> {
            if (!MainActivity.isLike()) return;
            try {
                AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
                if (am != null && am.isEnabled() && MainActivity.isLike()) {
                    if (getRootInActiveWindow() == null) return;
                    /* 获取匹配的节点列表 */
                    List<AccessibilityNodeInfo> nodes = getRootInActiveWindow()
                            .findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/dyw");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) nodes.stream()
                            /* 过滤出可见和可点击的节点 */
                            .filter(node -> node.isVisibleToUser() && node.isClickable())
                            /* 模拟点击节点 */
                            .forEach(node -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK));
                        /* 遍历节点列表 */
                    else for (AccessibilityNodeInfo node : nodes) {
                        /* 判断节点是否可见和可点击 */
                        if (node.isVisibleToUser() && node.isClickable())
                            /* 模拟点击节点 */
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in scheduled task: " + e.getMessage());
            }
        }, 1, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 触发了需要监听的无障碍事件后回调
     *
     * @param event The new event. This event is owned by the caller and cannot be used after
     *              this method returns. Services wishing to use the event after this method returns should
     *              make a copy.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    /**
     * 无障碍服务断开后回调
     */
    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTask();
    }

    public static void cancelTask() {
        // 在服务销毁时取消并释放定时任务和线程池
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (threadPool != null) {
            threadPool.shutdown();
            threadPool = null;
        }
    }
}
