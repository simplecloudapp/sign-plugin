package app.simplecloud.plugin.sign.shared.repository.base

interface LoadableRepository<I, E> : Repository<I, E> {
    fun load(): List<E>
}