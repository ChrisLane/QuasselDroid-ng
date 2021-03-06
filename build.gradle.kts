buildscript {
  repositories {
    google()
    jcenter()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.1.0")
    withVersion("1.2.31") {
      classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$version")
      classpath("org.jetbrains.kotlin:kotlin-android-extensions:$version")
    }
  }
}

allprojects {
  repositories {
    google()
    jcenter()
    maven(url = "https://jitpack.io")
  }
}
