package de.kuschku.quasseldroid

import android.os.Build
import android.os.StrictMode
import android.support.v7.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import de.kuschku.malheur.CrashHandler
import de.kuschku.quasseldroid.dagger.DaggerAppComponent
import de.kuschku.quasseldroid.util.backport.AndroidThreeTenBackport
import de.kuschku.quasseldroid.util.compatibility.AndroidCompatibilityUtils
import de.kuschku.quasseldroid.util.compatibility.AndroidLoggingHandler
import de.kuschku.quasseldroid.util.compatibility.AndroidStreamChannelFactory


class QuasselDroid : DaggerApplication() {
  override fun applicationInjector(): AndroidInjector<out QuasselDroid> =
    DaggerAppComponent.builder().create(this)

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return
    }
    LeakCanary.install(this)
    // Normal app init code...

    CrashHandler.init(
      application = this,
      buildConfig = BuildConfig::class.java
    )

    // Init compatibility utils
    AndroidCompatibilityUtils.inject()
    AndroidLoggingHandler.inject()
    AndroidStreamChannelFactory.inject()

    AndroidThreeTenBackport.init(this)

    // Initialize preferences unless already set
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

    /*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      systemService<ShortcutManager>().dynamicShortcuts = listOf(
        ShortcutInfo.Builder(this, "id1")
          .setShortLabel("#quassel")
          .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_channel))
          .setIntent(packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID))
          .build(),
        ShortcutInfo.Builder(this, "id2")
          .setShortLabel("#quasseldroid")
          .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_channel))
          .setIntent(packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID))
          .build(),
        ShortcutInfo.Builder(this, "id3")
          .setShortLabel("#quassel.de")
          .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_channel))
          .setIntent(packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID))
          .build(),
        ShortcutInfo.Builder(this, "id4")
          .setShortLabel("justJanne")
          .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_query))
          .setIntent(packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID))
          .build()
      )
    }
    */

    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
          .detectNetwork()
          .detectCustomSlowCalls()
          .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              it.detectResourceMismatches()
            } else {
              it
            }
          }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              it.detectUnbufferedIo()
            } else {
              it
            }
          }
          .penaltyLog()
          .build()
      )
      StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
          .detectLeakedSqlLiteObjects()
          .detectActivityLeaks()
          .detectLeakedClosableObjects()
          .detectLeakedRegistrationObjects()
          .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
              it.detectFileUriExposure()
            } else {
              it
            }
          }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              it.detectContentUriWithoutPermission()
            } else {
              it
            }
          }
          .penaltyLog()
          .build()
      )
    }
  }
}
