package com.example.capstoneproject.views.register

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.raassh.dicodingstoryapp.R
import com.raassh.dicodingstoryapp.customviews.EditTextWithValidation
import com.example.capstoneproject.data.api.ApiConfig
import com.example.capstoneproject.data.repository.AuthRepository
import com.raassh.dicodingstoryapp.databinding.RegisterFragmentBinding
import com.raassh.dicodingstoryapp.misc.hideSoftKeyboard
import com.raassh.dicodingstoryapp.misc.showSnackbar
import com.raassh.dicodingstoryapp.misc.visibility

class RegisterFragment : Fragment() {
    private val viewModel by viewModels<RegisterViewModel> {
        RegisterViewModel.Factory(
            AuthRepository(
                ApiConfig.getApiService()
            )
        )
    }

    private var binding: RegisterFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RegisterFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoading(false)

        binding?.apply {
            goToLogin.setOnClickListener {
                findNavController().navigateUp()
            }

            nameInput.setValidationCallback(object : EditTextWithValidation.InputValidation {
                override val errorMessage: String
                    get() = getString(R.string.name_validation_message)

                override fun validate(input: String) = input.isNotEmpty()
            })

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

            register.setOnClickListener {
                tryRegister()
            }
        }

        viewModel.apply {
            isLoading.observe(viewLifecycleOwner) {
                showLoading(it)
            }

            isSuccess.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { success ->
                    if (success) {
                        registered()
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
    }

    private fun tryRegister() {
        hideSoftKeyboard(activity as FragmentActivity)

        with(binding ?: return) {
            // note to self:
            // doing it like this will validate all input
            // instead of stopping after the first invalid
            val isNameValid = nameInput.validateInput()
            val isEmailValid = emailInput.validateInput()
            val isPasswordValid = passwordInput.validateInput()

            if (!isNameValid || !isEmailValid || !isPasswordValid) {
                showSnackbar(root, getString(R.string.validation_error))
                return
            }

            viewModel.register(
                nameInput.text.toString(),
                emailInput.text.toString(),
                passwordInput.text.toString()
            )
        }
    }

    private fun registered() {
        binding?.root?.let {
            showSnackbar(it, getString(R.string.register_success))
        }

        setFragmentResult(
            REGISTER_RESULT, bundleOf(
                EMAIL to binding?.emailInput?.text.toString()
            )
        )

        findNavController().navigateUp()
    }

    private fun showLoading(isLoading: Boolean) {
        binding?.apply {
            registerGroup.visibility = visibility(!isLoading)
            registerLoadingGroup.visibility = visibility(isLoading)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val REGISTER_RESULT = "register_result"
        const val EMAIL = "email"
    }
}