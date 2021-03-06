package de.kuschku.malheur.collectors

import android.app.Application
import android.os.Debug
import android.os.Environment
import de.kuschku.malheur.CrashContext
import de.kuschku.malheur.config.EnvConfig
import de.kuschku.malheur.data.EnvInfo
import de.kuschku.malheur.data.MemoryInfo
import de.kuschku.malheur.util.reflectionCollectGetters
import java.io.File

class EnvCollector(application: Application) : Collector<EnvInfo, EnvConfig> {
  private val configurationCollector = ConfigurationCollector(application)

  override fun collect(context: CrashContext, config: EnvConfig) = EnvInfo(
    paths = collectIf(config.paths) {
      reflectionCollectGetters(
        Environment::class.java
      )?.map { (key, value) ->
        key to if (value is File) {
          value.canonicalPath
        } else {
          value
        }
      }?.toMap()
    },
    memory = collectIf(config.memory) {
      val memoryInfo = Debug.MemoryInfo()
      Debug.getMemoryInfo(memoryInfo)
      MemoryInfo(memoryInfo)
    },
    configuration = configurationCollector.collectIf(context, config.configuration),
    startTime = collectIf(config.startTime) {
      context.startTime.time
    },
    crashTime = collectIf(config.crashTime) {
      context.crashTime.time
    }
  )
}
