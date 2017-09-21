package de.kuschku.libquassel.protocol.message

import de.kuschku.libquassel.protocol.QVariantMap
import de.kuschku.libquassel.protocol.QVariant_
import de.kuschku.libquassel.protocol.Type

object ClientLoginAckSerializer : HandshakeMessageSerializer<HandshakeMessage.ClientLoginAck> {
  override fun serialize(data: HandshakeMessage.ClientLoginAck) = mapOf(
    "MsgType" to QVariant_("ClientLoginAck", Type.QString)
  )

  override fun deserialize(data: QVariantMap) = HandshakeMessage.ClientLoginAck()
}
