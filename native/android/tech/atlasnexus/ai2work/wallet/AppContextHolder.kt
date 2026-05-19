package tech.atlasnexus.ai2work.wallet

import android.content.Context

/** Holds the application Context for non-Android-component classes. */
object AppContextHolder {
    lateinit var context: Context
        private set

    fun init(ctx: Context) {
        context = ctx.applicationContext
        EthWalletHelper.appContext = context
    }
}
