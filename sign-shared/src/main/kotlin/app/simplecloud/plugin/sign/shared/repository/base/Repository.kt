package app.simplecloud.plugin.sign.shared.repository.base

interface Repository<I, E> {
    fun delete(element: E): Boolean
    fun save(element: E)
    fun find(identifier: I): E?
    fun getAll(): List<E>
}