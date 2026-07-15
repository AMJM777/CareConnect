package com.careconnect.ui.common

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * funzione per nascondere bottomNav mentre la tastiera è aperta e rimostrarla quando si chiude.
 * in questa app la root usa fitsSystemWindows e la finestra si ridimensiona quando compare
 * la tastiera
 */
fun nascondiBottomNavQuandoTastieraAperta(
    root: View,
    bottomNav: View,
    lifecycleOwner: LifecycleOwner
) {
    val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val areaVisibile = Rect()
        root.getWindowVisibleDisplayFrame(areaVisibile)
        val altezzaSchermo = root.rootView.height
        val spazioMancanteInBasso = altezzaSchermo - areaVisibile.bottom

        val tastieraAperta = spazioMancanteInBasso > altezzaSchermo * 0.15
        val nuovaVisibilita = if (tastieraAperta) View.GONE else View.VISIBLE

        // Cambio la visibilità solo se serve : evita relayout inutili
        if (bottomNav.visibility != nuovaVisibilita) {
            bottomNav.visibility = nuovaVisibilita
        }
    }
    root.viewTreeObserver.addOnGlobalLayoutListener(listener)

    // Rimozione automatica quando la vista del Fragment viene distrutta.
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            if (root.viewTreeObserver.isAlive) {
                root.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    })
}