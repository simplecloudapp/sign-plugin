package app.simplecloud.plugin.sign.shared.repository

interface LoadableRepository<I, E> : Repository<I, E> {
    fun load(): List<E>
}