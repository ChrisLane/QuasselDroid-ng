package de.kuschku.libquassel.protocol.message

import de.kuschku.libquassel.protocol.QVariantMap
import de.kuschku.libquassel.protocol.QVariant_
import de.kuschku.libquassel.protocol.Type
import de.kuschku.libquassel.protocol.value

object ClientLoginRejectSerializer :
  HandshakeMessageSerializer<HandshakeMessage.ClientLoginReject> {
  override fun serialize(data: HandshakeMessage.ClientLoginReject) = mapOf(
    "MsgType" to QVariant_("ClientLoginReject", Type.QString),
    "Error" to QVariant_(data.errorString, Type.QString)
  )

  override fun deserialize(data: QVariantMap) = HandshakeMessage.ClientLoginReject(
    errorString = data["Error"].value()
  )
}
