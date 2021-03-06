package de.kuschku.libquassel

import de.kuschku.libquassel.protocol.QType
import de.kuschku.libquassel.protocol.QVariant
import de.kuschku.libquassel.protocol.Type
import de.kuschku.libquassel.protocol.primitive.serializer.*
import de.kuschku.libquassel.quassel.QuasselFeatures
import de.kuschku.libquassel.util.nio.ChainedByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.nio.ByteBuffer
import kotlin.experimental.inv

class SerializerUnitTest {
  @Test
  fun boolSerializer() {
    assertEquals(true, roundTrip(BoolSerializer, true))
    assertEquals(false, roundTrip(BoolSerializer, false))
  }

  @Test
  fun byteArraySerializer() {
    val value1 = byteArrayOf()
    assertArrayEquals(value1, roundTrip(ByteArraySerializer, ByteBuffer.wrap(value1))?.array())

    val value2 = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
    assertArrayEquals(value2, roundTrip(ByteArraySerializer, ByteBuffer.wrap(value2))?.array())
  }

  @Test
  fun charSerializer() {
    assertEquals(' ', roundTrip(CharSerializer, ' '))
    assertEquals('a', roundTrip(CharSerializer, 'a'))
    assertEquals('ä', roundTrip(CharSerializer, 'ä'))
    assertEquals('\u0000', roundTrip(CharSerializer, '\u0000'))
    assertEquals('\uFFFF', roundTrip(CharSerializer, '\uFFFF'))
    assertEquals(' ', roundTrip(CharSerializer, ' '))
    assertEquals('a', roundTrip(CharSerializer, 'a'))
    assertEquals('ä', roundTrip(CharSerializer, 'ä'))
  }

  @Test
  fun dateTimeSerializer() {
    assertEquals(Instant.EPOCH, roundTrip(DateTimeSerializer, Instant.EPOCH))
    val now = Instant.now()
    assertEquals(now, roundTrip(DateTimeSerializer, now))

    val value1 = Instant.EPOCH.atOffset(ZoneOffset.ofTotalSeconds(1234))
    assertEquals(
      value1.atZoneSimilarLocal(ZoneOffset.UTC).toInstant(),
      roundTrip(DateTimeSerializer, value1)
    )
    val value2 = Instant.now().atOffset(ZoneOffset.ofTotalSeconds(1234))
    assertEquals(
      value2.atZoneSimilarLocal(ZoneOffset.UTC).toInstant(),
      roundTrip(DateTimeSerializer, value2)
    )

    val value3 = LocalDateTime.of(1970, 1, 1, 0, 0)
      .atZone(ZoneOffset.systemDefault()).toInstant()
    assertEquals(value3, roundTrip(DateTimeSerializer, value3))
    val value4 = LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toInstant()
    assertEquals(value4, roundTrip(DateTimeSerializer, value4))
  }

  @Test
  fun byteSerializer() {
    assertEquals(0.toByte(), roundTrip(ByteSerializer, 0.toByte()))
    assertEquals(Byte.MAX_VALUE, roundTrip(ByteSerializer, Byte.MAX_VALUE))
    assertEquals(Byte.MIN_VALUE, roundTrip(ByteSerializer, Byte.MIN_VALUE))
    assertEquals((0.toByte().inv()), roundTrip(ByteSerializer, (0.toByte().inv())))
  }

  @Test
  fun shortSerializer() {
    assertEquals(0.toShort(), roundTrip(ShortSerializer, 0.toShort()))
    assertEquals(Short.MAX_VALUE, roundTrip(ShortSerializer, Short.MAX_VALUE))
    assertEquals(Short.MIN_VALUE, roundTrip(ShortSerializer, Short.MIN_VALUE))
    assertEquals((0.toShort().inv()), roundTrip(ShortSerializer, (0.toShort().inv())))
  }

  @Test
  fun intSerializer() {
    assertEquals(0, roundTrip(IntSerializer, 0))
    assertEquals(Integer.MAX_VALUE, roundTrip(IntSerializer, Integer.MAX_VALUE))
    assertEquals(Integer.MIN_VALUE, roundTrip(IntSerializer, Integer.MIN_VALUE))
    assertEquals(0.inv(), roundTrip(IntSerializer, 0.inv()))
  }

  @Test
  fun longSerializer() {
    assertEquals(0, roundTrip(LongSerializer, 0))
    assertEquals(Long.MAX_VALUE, roundTrip(LongSerializer, Long.MAX_VALUE))
    assertEquals(Long.MIN_VALUE, roundTrip(LongSerializer, Long.MIN_VALUE))
    assertEquals(0L.inv(), roundTrip(LongSerializer, 0L.inv()))
  }

  @Test
  fun stringSerializer() {
    assertEquals("Test", roundTrip(StringSerializer.UTF16, "Test"))
    assertEquals("Test", roundTrip(StringSerializer.UTF8, "Test"))
    assertEquals("Test", roundTrip(StringSerializer.C, "Test"))
  }

  @Test
  fun variantListSerializer() {
    val value = listOf(
      QVariant.of(1, Type.Int),
      QVariant.of(ByteBuffer.wrap(byteArrayOf(
        66,
        97,
        99,
        107,
        108,
        111,
        103,
        77,
        97,
        110,
        97,
        103,
        101,
        114,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0
      )), Type.QByteArray),
      QVariant.of(ByteBuffer.wrap(byteArrayOf(
        114,
        101,
        113,
        117,
        101,
        115,
        116,
        66,
        97,
        99,
        107,
        108,
        111,
        103,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0
      )), Type.QByteArray),
      QVariant.of(1873, QType.BufferId),
      QVariant.of(-1, QType.MsgId),
      QVariant.of(-1, QType.MsgId),
      QVariant.of(20, Type.Int),
      QVariant.of(0, Type.Int)
    )
    assert(value == roundTrip(VariantListSerializer, value))
  }

  companion object {
    fun <T> roundTrip(serializer: Serializer<T>, value: T,
                      features: QuasselFeatures = QuasselFeatures.all()): T {
      val chainedBuffer = ChainedByteBuffer(
        direct = false
      )
      serializer.serialize(chainedBuffer, value, features)
      val buffer = chainedBuffer.toBuffer()
      return serializer.deserialize(buffer, features)
    }
  }
}
