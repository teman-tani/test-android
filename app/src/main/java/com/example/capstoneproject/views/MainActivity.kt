package com.example.capstoneproject.views

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.raassh.dicodingstoryapp.R
import com.raassh.dicodingstoryapp.data.SessionPreferences
import com.raassh.dicodingstoryapp.databinding.ActivityMainBinding
import com.raassh.dicodingstoryapp.views.stories.StoriesFragmentDirections

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<SharedViewModel> {
        SharedViewModel.Factory(SessionPreferences.getInstance(dataStore))
    }

    private lateinit var binding: ActivityMainBinding

    var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.getToken().observe(this) {
            token = it
        }
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(binding.container.id)

        when (item.itemId) {
            R.id.logout -> {
                logout(navController)
            }
            R.id.setting -> {
                startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
            }
            R.id.map -> {
                val navigateAction =
                    StoriesFragmentDirections.actionStoriesFragmentToStoriesWithMapsFragment()
                navigateAction.token = token

                navController.navigate(navigateAction)
            }
            android.R.id.home -> {
                navController.navigateUp()
            }
        }

        return true
    }

    private fun logout(navController: NavController) {
        viewModel.saveToken("")

        // ref: https://github.com/android/architecture-components-samples/issues/767
        val navHostFragment =
            supportFragmentManager.findFragmentById(binding.container.id) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        graph.setStartDestination(R.id.loginFragment)
        navController.graph = graph
    }
}