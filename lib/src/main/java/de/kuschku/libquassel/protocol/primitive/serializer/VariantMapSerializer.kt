package de.kuschku.libquassel.protocol.primitive.serializer

import de.kuschku.libquassel.protocol.QVariantMap
import de.kuschku.libquassel.quassel.QuasselFeatures
import de.kuschku.libquassel.util.nio.ChainedByteBuffer
import java.nio.ByteBuffer

object VariantMapSerializer : Serializer<QVariantMap> {
  override fun serialize(buffer: ChainedByteBuffer, data: QVariantMap, features: QuasselFeatures) {
    IntSerializer.serialize(buffer, data.size, features)
    data.entries.forEach { (key, value) ->
      StringSerializer.UTF16.serialize(buffer, key, features)
      VariantSerializer.serialize(buffer, value, features)
    }
  }

  override fun deserialize(buffer: ByteBuffer, features: QuasselFeatures): QVariantMap {
    return mutableMapOf(
      *(0 until IntSerializer.deserialize(buffer, features)).map {
        Pair(
          StringSerializer.UTF16.deserialize(buffer, features) ?: "",
          VariantSerializer.deserialize(buffer, features)
        )
      }.toTypedArray()
    )
  }
}
