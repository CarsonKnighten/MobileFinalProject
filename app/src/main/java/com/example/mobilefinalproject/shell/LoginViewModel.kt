package com.example.mobilefinalproject.shell

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobilefinalproject.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException

internal sealed interface LoginState {
    data object Ready : LoginState
    data object Working : LoginState
    data class Error(@param:StringRes val message: Int) : LoginState
    data class SignedIn(val account: LocalAccount) : LoginState
}

internal class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val internalState = MutableLiveData<LoginState>(LoginState.Ready)
    val state: LiveData<LoginState> = internalState

    fun clearStatus() {
        if (internalState.value != LoginState.Working) internalState.value = LoginState.Ready
    }

    fun submit(email: String, password: CharArray, registration: Boolean) {
        if (internalState.value == LoginState.Working) {
            password.fill('\u0000')
            return
        }
        internalState.value = LoginState.Working
        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    LocalAccounts(getApplication()).use {
                        if (registration) it.register(email, password) else it.signIn(email, password)
                    }
                }
                internalState.value = when {
                    account != null -> LoginState.SignedIn(account)
                    registration -> LoginState.Error(R.string.login_email_exists)
                    else -> LoginState.Error(R.string.login_wrong_credentials)
                }
            } catch (error: SQLiteException) {
                storageError(error)
            } catch (error: GeneralSecurityException) {
                storageError(error)
            } finally {
                password.fill('\u0000')
            }
        }
    }

    private fun storageError(error: Exception) {
        Log.e("LocalAccounts", "Could not process a local account operation", error)
        internalState.value = LoginState.Error(R.string.login_storage_error)
    }
}
