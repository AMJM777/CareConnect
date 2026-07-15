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

// unica Activity dell'app (single-activity architecture): ospita il
// NavHostFragment principale e la Toolbar delle schermate di autenticazione.
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    // registrato come proprietà (non dentro onCreate): l'API richiede che
    // la registrazione avvenga prima che l'Activity sia avviata.
    private val richiediPermessoNotifiche =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // se l'utente rifiuta l'app continua a funzionare senza notifiche
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        // Splash e Login sono "di primo livello": niente freccia indietro
        appBarConfiguration = AppBarConfiguration(setOf(R.id.splashFragment, R.id.loginFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // la Toolbar dell'Activity serve solo alle schermate di
        // autenticazione: dentro le sezioni di ruolo c'è una Toolbar per
        // ruolo, quindi qui va nascosta per non averne due
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.visibility = when (destination.id) {
                R.id.homeAnzianoFragment,
                R.id.homeVolontarioFragment,
                R.id.homeFamiliareFragment -> View.GONE
                else -> View.VISIBLE
            }
        }
    }

    // funzione che chiede il permesso POST_NOTIFICATIONS solo dove serve
    // (Android 13+, permesso implicito prima)
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

    // gestisce il tasto freccia della Toolbar
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}