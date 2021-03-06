package de.kuschku.quasseldroid.settings

import android.content.Context
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.settings.AppearanceSettings.InputEnterMode
import de.kuschku.quasseldroid.settings.AppearanceSettings.Theme
import de.kuschku.quasseldroid.util.helper.sharedPreferences

object Settings {
  fun appearance(context: Context) = context.sharedPreferences {
    AppearanceSettings(
      theme = Theme.of(
        getString(
          context.getString(R.string.preference_theme_key),
          ""
        )
      ) ?: AppearanceSettings.DEFAULT.theme,
      inputEnter = InputEnterMode.of(
        getString(
          context.getString(R.string.preference_input_enter_key),
          ""
        )
      ) ?: AppearanceSettings.DEFAULT.inputEnter,
      showLag = getBoolean(
        context.getString(R.string.preference_show_lag_key),
        AppearanceSettings.DEFAULT.showLag
      )
    )
  }

  fun message(context: Context) = context.sharedPreferences {
    MessageSettings(
      useMonospace = getBoolean(
        context.getString(R.string.preference_monospace_key),
        MessageSettings.DEFAULT.useMonospace
      ),
      textSize = getInt(
        context.getString(R.string.preference_textsize_key),
        MessageSettings.DEFAULT.textSize
      ),
      showSeconds = getBoolean(
        context.getString(R.string.preference_show_seconds_key),
        MessageSettings.DEFAULT.showSeconds
      ),
      use24hClock = getBoolean(
        context.getString(R.string.preference_use_24h_clock_key),
        MessageSettings.DEFAULT.use24hClock
      ),
      showPrefix = MessageSettings.ShowPrefixMode.of(
        getString(
          context.getString(R.string.preference_show_prefix_key),
          ""
        )
      ) ?: MessageSettings.DEFAULT.showPrefix,
      colorizeNicknames = MessageSettings.ColorizeNicknamesMode.of(
        getString(
          context.getString(R.string.preference_colorize_nicknames_key),
          ""
        )
      ) ?: MessageSettings.DEFAULT.colorizeNicknames,
      colorizeMirc = getBoolean(
        context.getString(R.string.preference_colorize_mirc_key),
        MessageSettings.DEFAULT.colorizeMirc
      ),
      showHostmask = getBoolean(
        context.getString(R.string.preference_hostmask_key),
        MessageSettings.DEFAULT.showHostmask
      ),
      nicksOnNewLine = getBoolean(
        context.getString(R.string.preference_nicks_on_new_line_key),
        MessageSettings.DEFAULT.nicksOnNewLine
      ),
      timeAtEnd = getBoolean(
        context.getString(R.string.preference_time_at_end_key),
        MessageSettings.DEFAULT.timeAtEnd
      ),
      showAvatars = getBoolean(
        context.getString(R.string.preference_show_avatars_key),
        MessageSettings.DEFAULT.showAvatars
      )
    )
  }

  fun autoComplete(context: Context) = context.sharedPreferences {
    AutoCompleteSettings(
      button = getBoolean(
        context.getString(R.string.preference_autocomplete_button_key),
        AutoCompleteSettings.DEFAULT.button
      ),
      doubleTap = getBoolean(
        context.getString(R.string.preference_autocomplete_doubletap_key),
        AutoCompleteSettings.DEFAULT.button
      ),
      auto = getBoolean(
        context.getString(R.string.preference_autocomplete_auto_key),
        AutoCompleteSettings.DEFAULT.button
      ),
      prefix = getBoolean(
        context.getString(R.string.preference_autocomplete_prefix_key),
        AutoCompleteSettings.DEFAULT.button
      )
    )
  }

  fun backlog(context: Context) = context.sharedPreferences {
    BacklogSettings(
      pageSize = getString(
        context.getString(R.string.preference_page_size_key),
        BacklogSettings.DEFAULT.pageSize.toString()
      ).toIntOrNull() ?: BacklogSettings.DEFAULT.pageSize
    )
  }

  fun connection(context: Context) = context.sharedPreferences {
    ConnectionSettings(
      showNotification = getBoolean(
        context.getString(R.string.preference_show_notification_key),
        ConnectionSettings.DEFAULT.showNotification
      )
    )
  }
}