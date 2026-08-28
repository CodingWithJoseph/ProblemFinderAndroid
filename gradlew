#!/bin/sh

# Gradle startup script for POSIX systems.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
APP_BASE_NAME=${0##*/}

if [ -z "${JAVA_HOME:-}" ]; then
    JAVACMD=java
else
    JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVACMD" ] && ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command was found." >&2
    exit 1
fi

if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "ERROR: gradle/wrapper/gradle-wrapper.jar is missing." >&2
    echo "Generate it with a local Gradle installation or open the project in Android Studio." >&2
    exit 1
fi

exec "$JAVACMD" ${DEFAULT_JVM_OPTS:-"-Xmx64m" "-Xms64m"} ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"

