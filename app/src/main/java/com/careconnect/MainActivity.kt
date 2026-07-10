package com.careconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.careconnect.util.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    // FASE 11/12 — Launcher per chiedere il permesso di mostrare notifiche.
    // Va registrato come proprietà (non dentro onCreate) perché l'API richiede
    // che la registrazione avvenga prima che l'Activity sia avviata.
    private val richiediPermessoNotifiche =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Non blocchiamo nulla se l'utente rifiuta: l'app continua a funzionare,
            // semplicemente non mostrerà notifiche finché il permesso non viene concesso.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // FASE 11/12 — Prepariamo il canale di notifica fin dall'avvio e chiediamo
        // il permesso (solo su Android 13+, e solo se non già concesso).
        NotificationHelper.creaCanali(this)
        chiediPermessoNotificheSeNecessario()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Splash e Login sono schermate "di primo livello": niente freccia
        // indietro. Le altre (Registrati, Completa profilo) restano di secondo
        // livello e mantengono la freccia per tornare al login.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.splashFragment, R.id.loginFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // La Toolbar dell'Activity serve solo alle schermate di autenticazione.
        // Dentro le sezioni di ruolo l'app bar la fornisce la schermata
        // contenitore (una Toolbar per ruolo): qui la nascondiamo per non averne due.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.visibility = when (destination.id) {
                R.id.homeAnzianoFragment,
                R.id.homeVolontarioFragment,
                R.id.homeFamiliareFragment -> View.GONE
                else -> View.VISIBLE
            }
        }
    }

    // FASE 11/12 — Chiede il permesso POST_NOTIFICATIONS solo dove serve davvero.
    // Prima di Android 13 il permesso non esiste (è implicito), quindi non facciamo nulla.
    private fun chiediPermessoNotificheSeNecessario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val giaConcesso = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!giaConcesso) {
                richiediPermessoNotifiche.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}