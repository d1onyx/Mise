package com.d1onyx.core.network.serialization

import com.d1onyx.core.essentials.entities.EventId
import com.d1onyx.core.essentials.entities.Id
import com.d1onyx.core.essentials.entities.UserId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes any [Id] subtype as a plain JSON string.
 *
 * Unlike the Android original this one also *encodes*. The original threw on
 * serialize because it was only ever used for responses; a shared core cannot
 * assume that, and an id that round-trips is strictly more useful.
 *
 * @param serialName must be unique per id type — `kotlinx.serialization` uses it
 * to identify the descriptor.
 */
public open class IdSerializer<T : Id>(
    serialName: String,
    private val idFactory: (String) -> T,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): T = idFactory(decoder.decodeString())
}

public object GenericIdSerializer : IdSerializer<Id>(
    serialName = "com.d1onyx.core.essentials.entities.Id",
    idFactory = Id.Companion::invoke,
)

public object UserIdSerializer : IdSerializer<UserId>(
    serialName = "com.d1onyx.core.essentials.entities.UserId",
    idFactory = UserId.Companion::invoke,
)

public object EventIdSerializer : IdSerializer<EventId>(
    serialName = "com.d1onyx.core.essentials.entities.EventId",
    idFactory = EventId.Companion::invoke,
)
