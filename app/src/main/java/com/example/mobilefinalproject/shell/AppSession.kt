package com.example.mobilefinalproject.shell

internal data class LocalAccount(val id: String, val email: String, val ownsExistingWorkouts: Boolean = false)

/** The signed-in account for the current app process. */
internal object AppSession {
    var account: LocalAccount? = null
        private set

    fun signIn(account: LocalAccount) {
        this.account = account
    }

    fun signOut() {
        account = null
    }
}
