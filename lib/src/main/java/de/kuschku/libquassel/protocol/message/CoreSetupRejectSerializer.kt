package de.kuschku.libquassel.protocol.message

import de.kuschku.libquassel.protocol.QVariant
import de.kuschku.libquassel.protocol.QVariantMap
import de.kuschku.libquassel.protocol.Type
import de.kuschku.libquassel.protocol.value

object CoreSetupRejectSerializer : HandshakeMessageSerializer<HandshakeMessage.CoreSetupReject> {
  override fun serialize(data: HandshakeMessage.CoreSetupReject) = mapOf(
    "MsgType" to QVariant.of("CoreSetupReject", Type.QString),
    "Error" to QVariant.of(data.errorString, Type.QString)
  )

  override fun deserialize(data: QVariantMap) = HandshakeMessage.CoreSetupReject(
    errorString = data["Error"].value()
  )
}
