package app.simplecloud.plugin.sign.shared.repository.base

import org.spongepowered.configurate.serialize.TypeSerializerCollection

interface LoadableRepository<I, E> : Repository<I, E> {
    fun load(serializers: TypeSerializerCollection? = null): List<E>
}