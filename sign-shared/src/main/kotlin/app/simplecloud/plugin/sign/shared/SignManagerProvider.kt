package app.simplecloud.plugin.sign.shared

object SignManagerProvider {

    private lateinit var signManager: SignManager<*>

    fun register(signManager: SignManager<*>) {
        this.signManager = signManager
    }

    fun get(): SignManager<*> = signManager
}