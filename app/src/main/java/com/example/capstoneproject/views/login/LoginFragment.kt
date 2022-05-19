package com.example.capstoneproject.views.login

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.fragment.app.*
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.raassh.dicodingstoryapp.R
import com.raassh.dicodingstoryapp.customviews.EditTextWithValidation
import com.raassh.dicodingstoryapp.data.SessionPreferences
import com.example.capstoneproject.data.api.ApiConfig
import com.example.capstoneproject.data.repository.AuthRepository
import com.raassh.dicodingstoryapp.databinding.LoginFragmentBinding
import com.raassh.dicodingstoryapp.misc.hideSoftKeyboard
import com.raassh.dicodingstoryapp.misc.showSnackbar
import com.raassh.dicodingstoryapp.misc.visibility
import com.example.capstoneproject.views.SharedViewModel
import com.example.capstoneproject.views.dataStore
import com.example.capstoneproject.views.register.RegisterFragment

class LoginFragment : Fragment() {
    private val viewModel by viewModels<LoginViewModel> {
        LoginViewModel.Factory(
            AuthRepository(
                ApiConfig.getApiService()
            )
        )
    }
    private val sharedViewModel by activityViewModels<SharedViewModel> {
        SharedViewModel.Factory(SessionPreferences.getInstance(context?.dataStore as DataStore))
    }

    private var binding: LoginFragmentBinding? = null

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LoginFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoading(false)

        setFragmentResultListener(RegisterFragment.REGISTER_RESULT) { _, bundle ->
            val email = bundle.getString(RegisterFragment.EMAIL, "")
            binding?.emailInput?.setText(email)
        }

        binding?.apply {
            goToRegister.setOnClickListener {
                findNavController().navigate(
                    R.id.action_loginFragment_to_registerFragment,
                    null,
                    null,
                    FragmentNavigatorExtras(
                        emailInput to emailInput.transitionName,
                        passwordInput to passwordInput.transitionName,
                        login to login.transitionName,
                        goToRegister to goToRegister.transitionName
                    )
                )
            }

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
                tryLogin()
            }
        }

        viewModel.apply {
            isLoading.observe(viewLifecycleOwner) {
                showLoading(it)
            }

            token.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { token ->
                    if (token.isNotEmpty()) {
                        loggedIn(token)
                    }
                }
            }

            error.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { message ->
                    binding?.root?.let { root ->
                        if (message.isEmpty()) {
                            showSnackbar(root, getString(R.string.generic_error))
                        } else {
                            showSnackbar(root, message)
                        }
                    }
                }
            }
        }

        sharedViewModel.getToken().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                goToStories(it)
            }
        }
    }

    private fun tryLogin() {
        hideSoftKeyboard(activity as FragmentActivity)

        with(binding ?: return) {
            val isEmailValid = emailInput.validateInput()
            val isPasswordValid = passwordInput.validateInput()

            if (!isEmailValid || !isPasswordValid) {
                showSnackbar(root, getString(R.string.validation_error))
                return
            }

            viewModel.login(emailInput.text.toString(), passwordInput.text.toString())
        }
    }

    private fun loggedIn(token: String) {
        sharedViewModel.saveToken(token)

        binding?.root?.let {
            showSnackbar(it, getString(R.string.login_success))
        }
    }

    private fun goToStories(token: String) {
        val navigateAction = LoginFragmentDirections
            .actionLoginFragmentToStoriesFragment()
        navigateAction.token = token

        findNavController().navigate(navigateAction)
    }

    private fun showLoading(isLoading: Boolean) {
        binding?.apply {
            loginGroup.visibility = visibility(!isLoading)
            loginLoadingGroup.visibility = visibility(isLoading)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}