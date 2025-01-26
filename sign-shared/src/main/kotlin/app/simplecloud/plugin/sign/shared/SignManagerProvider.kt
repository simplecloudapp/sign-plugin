package app.simplecloud.plugin.sign.shared

object SignManagerProvider {

    private var signManager: SignManager<*>? = null

    @Synchronized
    fun initialize(signManager: SignManager<*>) {
        if (this.signManager != null) {
            throw IllegalStateException("SignManager is already initialize")
        }
        this.signManager = signManager
    }

    fun get(): SignManager<*> = signManager
        ?: throw IllegalStateException("SignManager has not been initialize")
}