package com.example.mobilefinalproject

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mobilefinalproject.databinding.ActivityMainBinding
import com.example.mobilefinalproject.shell.AppSession
import com.example.mobilefinalproject.shell.LocalAccount
import com.example.mobilefinalproject.shell.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var openedFor: LocalAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedFor = AppSession.account
        if (openedFor == null) {
            openLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab?.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController
        val shellFab = binding.appBarMain.fab ?: binding.appBarMain.contentMain.fab
        shellFab?.let { fab ->
            navController.addOnDestinationChangedListener { _, destination, _ ->
                fab.isVisible = destination.id != R.id.nav_dashboard &&
                    destination.hierarchy.none { it.id == R.id.nav_strength_training }
            }
        }

        val navView: NavigationView? = findViewById(R.id.nav_view)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_dashboard), binding.drawerLayout.takeIf { navView != null })
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView?.setupWithNavController(navController)
        binding.appBarMain.contentMain.bottomNavView?.setupWithNavController(navController)
    }

    override fun onDestroy() {
        if (isFinishing && openedFor != null && AppSession.account === openedFor) AppSession.signOut()
        super.onDestroy()
    }

    internal fun signOut() {
        AppSession.signOut()
        openLogin()
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.overflow, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
