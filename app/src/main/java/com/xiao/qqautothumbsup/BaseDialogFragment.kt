package com.xiao.qqautothumbsup

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding

abstract class BaseDialogFragment<VB : ViewBinding>(
    protected val bindingInflater: (LayoutInflater) -> VB,
    private val cancelable: Boolean = false
) : DialogFragment() {
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding
            ?: throw IllegalStateException("Attempt to access binding when view is destroyed or not yet created")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        _binding = bindingInflater(requireActivity().layoutInflater)
        builder.setView(binding.root)
        initView()
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setDimAmount(0.4f)
        dialog.setCanceledOnTouchOutside(false)
        isCancelable = cancelable
        return dialog
    }

    override fun onStart() {
        super.onStart()
        resetDialogSize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isAdded) return
        if (manager.findFragmentByTag(tag) != null) return
        super.show(manager, tag)
    }

    protected open fun resetDialogSize() = dialog?.let {
        val window = it.window ?: return@let
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.80f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    abstract fun initView()
}
