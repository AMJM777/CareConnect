package com.careconnect

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    // Necessaria per gestire correttamente la freccia "indietro" nella
    // Toolbar quando si torna alla Activity da onSupportNavigateUp().
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recupera il NavController dal NavHostFragment (stesso approccio
        // visto a lezione: il NavHostFragment "vive" dentro il FragmentManager
        // dell'Activity, lo troviamo cercandolo per id).
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // AppBarConfiguration definisce quali destinazioni sono "di primo
        // livello" (nessuna freccia indietro, es. le 3 home) rispetto a
        // quelle raggiunte navigando in avanti (freccia indietro automatica).
        // Senza argomenti, usa il grafo intero: Navigation Component decide
        // da solo in base allo startDestination di ogni sotto-grafo.
        appBarConfiguration = AppBarConfiguration(navController.graph)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)
    }

    // Permette alla freccia "indietro" della Toolbar di funzionare come
    // il tasto Indietro di sistema, delegando a Navigation Component.
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}