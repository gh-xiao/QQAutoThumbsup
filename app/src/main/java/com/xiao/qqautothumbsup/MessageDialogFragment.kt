package com.xiao.qqautothumbsup

import android.view.View
import androidx.fragment.app.FragmentManager
import com.xiao.qqautothumbsup.databinding.DialogMessageBinding

class MessageDialogFragment private constructor() : BaseDialogFragment<DialogMessageBinding>(
    DialogMessageBinding::inflate
) {
    companion object {
        const val TAG = "MessageDialogFragment"
        fun newInstance() = MessageDialogFragment()

        /**
         * kt 拓展, 显示消息弹窗
         */
        fun MessageDialogFragment.showMsgDialog(
            fragmentManager: FragmentManager,
            title: String = "Notice",
            content: String,
            tag: String = TAG
        ): MessageDialogFragment {
            setDialogData(title, content)
            show(fragmentManager, tag)
            return this
        }
    }

    interface OnDialogClick {
        fun onConfirm() {}
        fun onCancel()
        fun onConfirm(content: String?) = onConfirm()
    }

    private var onDialogClick: OnDialogClick = object : OnDialogClick {
        override fun onConfirm() {}
        override fun onCancel() {}
    }
    private lateinit var title: String
    private lateinit var message: String

    /**
     * Java 调用
     */
    fun setOnDialogClick(listener: OnDialogClick) {
        this.onDialogClick = listener
    }

    /**
     * Kotlin 拓展
     */
    fun setOnDialogClick(onConfirm: () -> Unit, onCancel: () -> Unit) {
        this.onDialogClick = object : OnDialogClick {
            override fun onConfirm() = onConfirm()
            override fun onCancel() = onCancel()
        }
    }

    fun setDialogData(title: String, message: String) {
        this.title = title
        this.message = message
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun setContent(content: String) {
        this.message = content
    }

    override fun initView() {
        binding.apply {
            ivState.visibility = View.GONE
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = title
            tvContent.text = message
            tvConfirm.setOnClickListener {
                dismiss()
                onDialogClick.onConfirm()
            }
            tvCancel.setOnClickListener {
                dismiss()
                onDialogClick.onCancel()
            }
        }
    }
}