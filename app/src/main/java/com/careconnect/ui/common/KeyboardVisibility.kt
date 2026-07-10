package com.careconnect.ui.common

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Nasconde [bottomNav] mentre la tastiera è aperta e la rimostra quando si
 * chiude.
 *
 * Perché NON usiamo gli insets della tastiera: sulla nostra app la root usa
 * fitsSystemWindows (che "consuma" gli insets) e la finestra si ridimensiona
 * quando compare la tastiera (in quel caso l'altezza della tastiera misurata
 * dagli insets è 0). Quindi qui rileviamo la tastiera in modo diretto e
 * sempre affidabile: confrontiamo l'area visibile della finestra con
 * l'altezza totale dello schermo. Se in basso "manca" più del 15% dello
 * schermo, è la tastiera (valori piccoli sono solo la barra di navigazione).
 *
 * Il listener si rimuove da solo quando la vista del Fragment viene distrutta.
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

        // Cambio la visibilità solo se serve davvero: evita relayout inutili
        // (questo listener scatta a ogni passata di layout).
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