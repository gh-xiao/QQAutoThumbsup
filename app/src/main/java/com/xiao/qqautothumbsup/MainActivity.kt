package com.xiao.qqautothumbsup

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.interfaces.OnPermissionResult
import com.lzf.easyfloat.permission.PermissionUtils.checkPermission
import com.lzf.easyfloat.permission.PermissionUtils.requestPermission
import com.xiao.qqautothumbsup.databinding.ActivityMainBinding
import com.xiao.qqautothumbsup.databinding.FloatWindowsBinding
import kotlin.concurrent.Volatile

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fwBinding: FloatWindowsBinding
    private var asUnAvailable = true

    @Volatile
    private var state = ""

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val FLOAT_TAG = "likeFloat"
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
        private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

        @JvmStatic
        var isLike = false
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        fwBinding = FloatWindowsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        /* 数据恢复 */
        reSetData(savedInstanceState)
        /* 控件初始化 */
        init()
    }

    override fun onResume() {
        super.onResume()
        /* 隐藏返回软件控件 */
        fwBinding.returnBtn.visibility = View.GONE
        state = "onResume"
        /* 权限检查 */
        request()
    }

    override fun onPause() {
        super.onPause()
        /* 显示返回软件控件 */
        fwBinding.returnBtn.visibility = View.VISIBLE
        state = "onPause"
    }

    override fun onDestroy() {
        super.onDestroy()
        isLike = false
        MainAccessibilityService.cancelTask()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("state", state)
        outState.putString("unAvailable", asUnAvailable.toString())
        outState.putString("like", isLike.toString())
    }

    /**
     * 控件初始化
     */
    private fun init() {
        /* 前往无障碍设置 */
        binding.gotoSettingBtn.setOnClickListener {
            val bundle = Bundle()
            /* 设置要启用的组件名 */
            val componentName = ComponentName("com.xiao.qqautothumbsup", MainAccessibilityService::class.java.name).flattenToString()
            bundle.putString(EXTRA_FRAGMENT_ARG_KEY, componentName)
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, componentName)
            intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
            startActivity(intent)
        }
        /* 创建浮窗 */
        binding.buildWinBtn.setOnClickListener {
            /* 如果Android版本大于6.0且未授予系统悬浮窗权限 */
            if (!checkPermission(this)) AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("创建悬浮窗需要允许显示在应用上层权限!")
                    .setPositiveButton("确定") { _, _ ->
                        requestPermission(this, object : OnPermissionResult {
                            override fun permissionResult(isOpen: Boolean) {
                                buildEasyFloatWin()
                                binding.buildWinBtn.isEnabled = !isOpen
                            }
                        })
                    }
                    .setNegativeButton("取消") { _, _ -> }
                    .show()
            else {
                buildEasyFloatWin()
                it.isEnabled = false
            }
        }
        /* 显示悬浮窗 */
        binding.floatWinBtn.setOnClickListener {
            EasyFloat.show(FLOAT_TAG)
            binding.buildWinBtn.isEnabled = !EasyFloat.isShow(FLOAT_TAG)
        }
        /* 跳转到QQ */
        binding.jumpBtn.setOnClickListener {
            try {
                val intent = Intent()
                val cmp = ComponentName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.SplashActivity")
                intent.setAction(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setComponent(cmp)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "ERROR:　" + e.message)
                Toast.makeText(this, "未能拉起QQ!", Toast.LENGTH_LONG).show()
            }
        }
        /* 重置悬浮窗控件 */
        fwBinding.resetBtn.setOnClickListener {
            fwBinding.apply {
                visOffBtn.visibility = View.VISIBLE
                returnBtn.visibility = if (state == "onPause") View.VISIBLE else View.GONE
                finishBtn.visibility = View.VISIBLE
                resetBtn.visibility = View.GONE
            }
        }
        /* 刷新权限状态 */
        binding.freshBtn.setOnClickListener { request() }
    }

    /**
     * 权限检查
     */
    private fun request() {
        /* 检查系统悬浮窗权限 */
        val easyFloatPermission = !checkPermission(this)
        /* 检查无障碍权限 */
        isAccessibilityServiceEnabled(this, MainAccessibilityService::class.java)
        /* 设置是否需要跳转申请权限 */
        binding.gotoSettingBtn.isEnabled = asUnAvailable
        binding.buildWinBtn.isEnabled = EasyFloat.getFloatView(FLOAT_TAG) == null
        /* 主页显示1 */
        binding.state1.text = if (asUnAvailable) "未授予" else "已授予"
        binding.state1.setTextColor(if (asUnAvailable) Color.RED else Color.GREEN)
        /* 主页显示2 */
        binding.state2.text = if (easyFloatPermission) "未授予" else "已授予"
        binding.state2.setTextColor(if (easyFloatPermission) Color.RED else Color.GREEN)
    }

    private fun buildEasyFloatWin() {
        EasyFloat.with(this)
                .setTag(FLOAT_TAG)
                .setGravity(Gravity.END or Gravity.BOTTOM)
                .setLayoutChangedGravity(Gravity.END or Gravity.BOTTOM)
                /* 设置浮窗布局 */
                .setLayout(fwBinding.getRoot()) {
                    /* 自动点赞 */
                    fwBinding.likeBtn.setOnClickListener {
                        /* 自动点赞 */
                        if (!isLike) {
                            isLike = true
                            fwBinding.apply {
                                likeBtn.text = "关闭点赞"
                                collapseBtn.contentDescription = "关闭点赞"
                                likeBtn.icon = AppCompatResources.getDrawable(this@MainActivity, R.drawable.thumb_down_off)
                            }
                        } else {
                            isLike = false
                            fwBinding.apply {
                                likeBtn.text = "开启点赞"
                                collapseBtn.contentDescription = "开启点赞"
                                likeBtn.icon = AppCompatResources.getDrawable(this@MainActivity, R.drawable.thumb_up)
                            }
                        }
                    }
                    /* 隐藏悬浮窗 */
                    fwBinding.visOffBtn.setOnClickListener { EasyFloat.hide(FLOAT_TAG) }
                    /* 隐藏控件 */
                    fwBinding.visOffBtn.setOnLongClickListener {
                        fwBinding.visOffBtn.visibility = View.GONE
                        fwBinding.resetBtn.visibility = View.VISIBLE
                        true
                    }
                    /* 返回软件 */
                    fwBinding.returnBtn.setOnClickListener {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    /* 隐藏控件 */
                    fwBinding.returnBtn.setOnLongClickListener {
                        fwBinding.returnBtn.visibility = View.GONE
                        fwBinding.resetBtn.visibility = View.VISIBLE
                        true
                    }
                    /* 关闭软件 */
                    fwBinding.finishBtn.setOnClickListener {
                        /* 关闭悬浮窗 */
                        EasyFloat.dismiss(FLOAT_TAG)
                        /* 关闭activity */
                        finishAndRemoveTask()
                    }
                    /* 隐藏控件 */
                    fwBinding.finishBtn.setOnLongClickListener {
                        fwBinding.finishBtn.visibility = View.GONE
                        fwBinding.resetBtn.visibility = View.VISIBLE
                        true
                    }
                    /* 展开与收起 */
                    fwBinding.collapseBtn.setOnClickListener {
                        /* 获得当前浮窗状态 */
                        val isExpand = fwBinding.collapseBtn.contentDescription.toString() == "收起浮窗"
                        fwBinding.apply {
                            /* 设置菜单显示状态 */
                            expandedMenu.visibility = if (isExpand) View.GONE else View.VISIBLE
                            /* 设置展开与收起按键状态 */
                            collapseBtn.contentDescription = if (isExpand) "展开浮窗" else "收起浮窗"
                            collapseBtn.setBackgroundColor(getResources().getColor(if (isExpand) R.color.green else R.color.blue))
                            collapseBtn.icon = AppCompatResources.getDrawable(this@MainActivity, if (isExpand) R.drawable.expand else R.drawable.collapse)
                        }
                    }
                }
                /* 设置显示类型 */
                .setShowPattern(ShowPattern.ALL_TIME)
                .show()
    }

    /**
     * 数据恢复
     */
    private fun reSetData(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        state = savedInstanceState.getString("state").toString()
        asUnAvailable = savedInstanceState.getString("unAvailable").toBoolean()
        isLike = savedInstanceState.getString("like").toBoolean()
    }

    /**
     * 无障碍权限检查
     *
     * @param context Activity上下文
     * @param service 无障碍服务类
     */
    @SuppressLint("NewApi")
    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService?>) {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        asUnAvailable = !enabledServices.stream()
                .map { serviceInfo -> serviceInfo.resolveInfo.serviceInfo }
                .anyMatch { it.packageName == context.packageName && it.name == service.name }
    }
}