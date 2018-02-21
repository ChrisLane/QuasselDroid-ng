package de.kuschku.quasseldroid_ng.ui.chat

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import de.kuschku.quasseldroid_ng.R
import de.kuschku.quasseldroid_ng.ui.settings.data.AppearanceSettings
import de.kuschku.quasseldroid_ng.ui.viewmodel.QuasselViewModel
import de.kuschku.quasseldroid_ng.util.AndroidHandlerThread
import de.kuschku.quasseldroid_ng.util.helper.map
import de.kuschku.quasseldroid_ng.util.irc.format.IrcFormatDeserializer
import de.kuschku.quasseldroid_ng.util.service.ServiceBoundFragment

class NickListFragment : ServiceBoundFragment() {
  private lateinit var viewModel: QuasselViewModel

  private val handlerThread = AndroidHandlerThread("NickList")

  @BindView(R.id.nickList)
  lateinit var nickList: RecyclerView

  private var ircFormatDeserializer: IrcFormatDeserializer? = null
  private val renderingSettings = AppearanceSettings(
    showPrefix = AppearanceSettings.ShowPrefixMode.FIRST,
    colorizeNicknames = AppearanceSettings.ColorizeNicknamesMode.ALL_BUT_MINE,
    colorizeMirc = true,
    timeFormat = ""
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    handlerThread.onCreate()
    super.onCreate(savedInstanceState)

    viewModel = ViewModelProviders.of(activity!!)[QuasselViewModel::class.java]

    if (ircFormatDeserializer == null) {
      ircFormatDeserializer = IrcFormatDeserializer(context!!)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_nick_list, container, false)
    ButterKnife.bind(this, view)

    nickList.adapter = NickListAdapter(
      this,
      viewModel.nickData.map {
        it.map {
          it.copy(
            realname = ircFormatDeserializer?.formatString(
              it.realname.toString(), renderingSettings.colorizeMirc
            ) ?: it.realname
          )
        }
      },
      handlerThread::post,
      activity!!::runOnUiThread,
      clickListener
    )

    nickList.layoutManager = LinearLayoutManager(context)
    nickList.itemAnimator = DefaultItemAnimator()

    return view
  }

  override fun onDestroy() {
    handlerThread.onDestroy()
    super.onDestroy()
  }

  private val clickListener: ((String) -> Unit)? = {
    // TODO
  }
}