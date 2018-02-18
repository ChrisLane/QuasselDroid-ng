package de.kuschku.quasseldroid_ng.ui.chat

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import de.kuschku.libquassel.protocol.Message_Flag
import de.kuschku.libquassel.protocol.Message_Flags
import de.kuschku.libquassel.protocol.Message_Type
import de.kuschku.libquassel.protocol.Message_Types
import de.kuschku.libquassel.util.hasFlag
import de.kuschku.quasseldroid_ng.persistence.QuasselDatabase
import de.kuschku.quasseldroid_ng.ui.settings.data.RenderingSettings
import de.kuschku.quasseldroid_ng.util.helper.getOrPut

class MessageAdapter(context: Context) :
  PagedListAdapter<QuasselDatabase.DatabaseMessage, QuasselMessageViewHolder>(
    QuasselDatabase.DatabaseMessage.MessageDiffCallback
  ) {
  private val messageRenderer: MessageRenderer = QuasselMessageRenderer(
    context,
    RenderingSettings(
      showPrefix = RenderingSettings.ShowPrefixMode.FIRST,
      colorizeNicknames = RenderingSettings.ColorizeNicknamesMode.ALL_BUT_MINE,
      colorizeMirc = true,
      timeFormat = ""
    )
  )

  private val messageCache = LruCache<Int, FormattedMessage>(512)

  override fun onBindViewHolder(holder: QuasselMessageViewHolder, position: Int) {
    getItem(position)?.let {
      messageRenderer.bind(
        holder,
        messageCache.getOrPut(it.messageId) {
          messageRenderer.render(it)
        }
      )
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    if (item != null) {
      return viewType(Message_Flags.of(item.type), Message_Flags.of(item.flag))
    } else {
      return 0
    }
  }

  private fun viewType(type: Message_Types, flags: Message_Flags): Int {
    if (flags.hasFlag(Message_Flag.Highlight)) {
      return -type.value
    } else {
      return type.value
    }
  }

  override fun getItemId(position: Int): Long {
    return getItem(position)?.messageId?.toLong() ?: 0L
  }

  private fun messageType(viewType: Int): Message_Type?
    = Message_Type.of(Math.abs(viewType)).enabledValues().firstOrNull()

  private fun hasHiglight(viewType: Int)
    = viewType < 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuasselMessageViewHolder {
    val messageType = messageType(viewType)
    val hasHighlight = hasHiglight(viewType)
    val viewHolder = QuasselMessageViewHolder(
      LayoutInflater.from(parent.context).inflate(
        messageRenderer.layout(messageType, hasHighlight),
        parent,
        false
      )
    )
    messageRenderer.init(viewHolder, messageType, hasHighlight)
    return viewHolder
  }

  operator fun get(position: Int): QuasselDatabase.DatabaseMessage? {
    return getItem(position)
  }
}

