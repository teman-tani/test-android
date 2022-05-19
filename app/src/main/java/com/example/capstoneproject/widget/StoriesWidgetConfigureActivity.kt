package com.example.capstoneproject.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.capstoneproject.R
import com.raassh.dicodingstoryapp.customviews.EditTextWithValidation
import com.raassh.dicodingstoryapp.data.SessionPreferences
import com.example.capstoneproject.data.api.ApiConfig
import com.example.capstoneproject.data.repository.AuthRepository
import com.example.capstoneproject.databinding.StoriesWidgetConfigureBinding
import com.raassh.dicodingstoryapp.misc.showSnackbar
import com.raassh.dicodingstoryapp.misc.visibility
import com.example.capstoneproject.views.SharedViewModel
import com.example.capstoneproject.views.dataStore
import com.example.capstoneproject.views.login.LoginViewModel

class StoriesWidgetConfigureActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: StoriesWidgetConfigureBinding
    private val sharedViewModel by viewModels<SharedViewModel> {
        SharedViewModel.Factory(SessionPreferences.getInstance(dataStore))
    }
    private val loginViewModel by viewModels<LoginViewModel> {
        LoginViewModel.Factory(
            AuthRepository(
                ApiConfig.getApiService()
            )
        )
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)

        binding = StoriesWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading(false)

        sharedViewModel.getToken().observe(this) {
            if (it.isNotEmpty()) {
                showWidget()
            }
        }

        loginViewModel.apply {
            isLoading.observe(this@StoriesWidgetConfigureActivity) {
                showLoading(it)
            }

            token.observe(this@StoriesWidgetConfigureActivity) {
                it.getContentIfNotHandled()?.let { token ->
                    Toast.makeText(
                        this@StoriesWidgetConfigureActivity,
                        getString(R.string.login_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    sharedViewModel.saveToken(token)
                }
            }

            error.observe(this@StoriesWidgetConfigureActivity) {
                it.getContentIfNotHandled()?.let { message ->
                    showSnackbar(binding.root, message)
                }
            }
        }

        binding.apply {
            emailInput.setValidationCallback(object : EditTextWithValidation.InputValidation {
                override val errorMessage: String
                    get() = getString(R.string.email_validation_message)

                override fun validate(input: String) = input.isNotEmpty()
                        && Patterns.EMAIL_ADDRESS.matcher(input).matches()
            })

            passwordInput.setValidationCallback(object : EditTextWithValidation.InputValidation {
                override val errorMessage: String
                    get() = getString(R.string.password_validation_message)

                override fun validate(input: String) = input.length >= 6
            })

            login.setOnClickListener {
                val isEmailValid = emailInput.validateInput()
                val isPasswordValid = passwordInput.validateInput()

                if (!isEmailValid || !isPasswordValid) {
                    showSnackbar(root, getString(R.string.validation_error))
                    return@setOnClickListener
                }

                loginViewModel.login(emailInput.text.toString(), passwordInput.text.toString())
            }
        }

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            loginGroup.visibility = visibility(!isLoading)
            loginLoadingGroup.visibility = visibility(isLoading)
        }
    }

    private fun showWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        StoriesWidget.updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}