package com.example.mobilefinalproject.shell

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.example.mobilefinalproject.MainActivity
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val model by lazy { ViewModelProvider(this)[LoginViewModel::class.java] }
    private var registration = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppSession.account != null) {
            openDashboard()
            return
        }

        registration = savedInstanceState?.getBoolean("registration") ?: false
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.signInButton.setOnClickListener { submit() }
        binding.switchAuthMode.setOnClickListener {
            registration = !registration
            binding.loginPassword.text?.clear()
            binding.confirmPassword.text?.clear()
            binding.emailField.error = null
            binding.passwordField.error = null
            binding.confirmPasswordField.error = null
            updateMode()
            model.clearStatus()
        }
        updateMode()
        model.state.observe(this) { state ->
            val working = state == LoginState.Working
            binding.signInButton.isEnabled = !working
            binding.switchAuthMode.isEnabled = !working
            binding.emailField.isEnabled = !working
            binding.passwordField.isEnabled = !working
            binding.confirmPasswordField.isEnabled = !working
            binding.loginStatus.text = when (state) {
                LoginState.Working -> getString(if (registration) R.string.login_creating else R.string.login_signing_in)
                is LoginState.Error -> getString(state.message)
                else -> ""
            }
            if (state is LoginState.SignedIn) {
                AppSession.signIn(state.account)
                model.clearStatus()
                openDashboard()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("registration", registration)
    }

    private fun updateMode() {
        binding.loginTitle.setText(if (registration) R.string.login_register_title else R.string.login_title)
        binding.loginHint.setText(if (registration) R.string.login_register_hint else R.string.login_hint)
        binding.signInButton.setText(if (registration) R.string.login_register_title else R.string.login_title)
        binding.switchAuthMode.setText(if (registration) R.string.login_back_to_sign_in else R.string.login_register_title)
        binding.confirmPasswordField.isVisible = registration
        binding.passwordField.helperText = if (registration) getString(R.string.login_password_help) else null
    }

    private fun submit() {
        model.clearStatus()
        binding.emailField.error = null
        binding.passwordField.error = null
        binding.confirmPasswordField.error = null
        val email = normalizedEmail(binding.loginEmail.text.toString())
        if (email == null) {
            binding.emailField.error = getString(R.string.login_invalid_email)
            return
        }
        val password = binding.loginPassword.text ?: return
        if (password.isEmpty()) {
            binding.passwordField.error = getString(R.string.login_enter_password)
            return
        }
        if (registration && password.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH) {
            binding.passwordField.error = getString(R.string.login_password_help)
            return
        }
        val confirmation: CharSequence = binding.confirmPassword.text ?: ""
        if (registration && (password.length != confirmation.length || password.indices.any { password[it] != confirmation[it] })) {
            binding.confirmPasswordField.error = getString(R.string.login_password_mismatch)
            return
        }
        val characters = CharArray(password.length) { password[it] }
        binding.loginPassword.text?.clear()
        binding.confirmPassword.text?.clear()
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(binding.root.windowToken, 0)
        model.submit(email, characters, registration)
    }

    private fun openDashboard() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
