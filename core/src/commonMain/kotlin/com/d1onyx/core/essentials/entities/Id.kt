package com.d1onyx.core.essentials.entities

/**
 * This class represents any entity identifier within the app.
 */
public interface Id {

    public val value: String

    public fun interface Scope {
        public fun generateId(): Id
    }

    private class DefaultId(value: String) : AbstractId(value)

    public companion object {

        public val Empty: Id = Id(0)

        public fun <T> idGenerator(block: Scope.() -> T): T {
            var seq = 0L
            val scopeImpl = Scope {
                Id(++seq)
            }
            return block(scopeImpl)
        }

        public operator fun invoke(value: String): Id = DefaultId(value)
        public operator fun invoke(id: Long): Id = invoke(id.toString())
    }
}

public abstract class AbstractId(
    override val value: String,
) : Id {

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

public interface IdOwner {
    public val id: Id
}

public interface UserId : Id {

    private class Default(value: String) : AbstractId(value), UserId

    public companion object {
        public operator fun invoke(value: String): UserId = Default(value)
    }
}

public interface EventId : Id {

    private class Default(value: String) : AbstractId(value), EventId

    public companion object {
        public operator fun invoke(value: String): EventId = Default(value)
    }
}
