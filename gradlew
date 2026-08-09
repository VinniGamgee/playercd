#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
PROPS="$DIR/gradle/wrapper/gradle-wrapper.properties"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Missing gradle-wrapper.jar. Open in Android Studio to sync, or download Gradle 8.5."
  exit 1
fi
exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
