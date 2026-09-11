package com.autotrack.app

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OverlayManager {

    private var currentOverlayView: View? = null

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun show(context: Context, txn: ParsedTransaction) {
        if (!canDrawOverlays(context)) return
        if (!DataSyncManager.isServiceEnabled()) return

        Handler(Looper.getMainLooper()).post {
            dismissCurrent(context)

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val card = LayoutInflater.from(context).inflate(R.layout.overlay_confirm_card, null)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
            }

            // Bind Views
            val tvSource = card.findViewById<TextView>(R.id.tvOverlaySource)
            val etAmount = card.findViewById<EditText>(R.id.etOverlayAmount)
            val rgType = card.findViewById<RadioGroup>(R.id.rgOverlayType)
            val rbExpense = card.findViewById<RadioButton>(R.id.rbOverlayExpense)
            val rbIncome = card.findViewById<RadioButton>(R.id.rbOverlayIncome)
            val etVendor = card.findViewById<EditText>(R.id.etOverlayVendor)
            val spCategory = card.findViewById<Spinner>(R.id.spOverlayCategory)
            val etNote = card.findViewById<EditText>(R.id.etOverlayNote)
            val btnConfirm = card.findViewById<Button>(R.id.btnOverlayConfirm)
            val btnDismiss = card.findViewById<Button>(R.id.btnOverlayDismiss)
            val btnSnooze = card.findViewById<Button>(R.id.btnOverlaySnooze)

            // Populate fields
            tvSource.text = txn.sourceApp
            etAmount.setText(String.format(java.util.Locale.US, "%.2f", txn.amount))

            if (txn.type == "income") {
                rbIncome.isChecked = true
            } else {
                rbExpense.isChecked = true
            }

            if (!txn.vendor.isNullOrBlank()) {
                etVendor.setText(txn.vendor)
            }

            // Populate Category Spinner
            val categories = DataSyncManager.getCachedCategories()
            val categoryLabels = categories.map { it.second }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categoryLabels)
            spCategory.adapter = adapter

            // Actions
            btnConfirm.setOnClickListener {
                val finalAmount = etAmount.text.toString().toDoubleOrNull() ?: txn.amount
                val finalType = if (rbIncome.isChecked) "income" else "expense"
                val finalVendor = etVendor.text.toString().trim()
                val finalNote = etNote.text.toString().trim()

                val selectedIndex = spCategory.selectedItemPosition
                val selectedCatId = if (selectedIndex in categories.indices) categories[selectedIndex].first else null

                CoroutineScope(Dispatchers.IO).launch {
                    DataSyncManager.saveTransaction(
                        context = context,
                        amount = finalAmount,
                        type = finalType,
                        vendor = finalVendor,
                        categoryId = selectedCatId,
                        sourceApp = txn.sourceApp,
                        note = finalNote,
                        rawNotification = txn.rawText
                    )
                }

                dismissCurrent(context)
            }

            btnDismiss.setOnClickListener {
                dismissCurrent(context)
            }

            btnSnooze.setOnClickListener {
                dismissCurrent(context)
                // Re-show in 5 minutes (300,000 ms)
                Handler(Looper.getMainLooper()).postDelayed({
                    show(context, txn)
                }, 300000L)
            }

            try {
                wm.addView(card, params)
                currentOverlayView = card
            } catch (e: Exception) {
                // WindowManager addView failure fallback
            }
        }
    }

    fun dismissCurrent(context: Context) {
        currentOverlayView?.let { view ->
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(view)
            } catch (e: Exception) {
                // ignore
            }
            currentOverlayView = null
        }
    }
}
