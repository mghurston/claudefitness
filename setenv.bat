@echo off
REM ASCENDANT - portable toolchain activation (cmd.exe)
REM Sets JAVA_HOME / ANDROID_HOME for THIS SESSION ONLY (no system changes).
REM Usage:  setenv.bat

set "JAVA_HOME=%~dp0jdk"
set "ANDROID_HOME=%~dp0android-sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "GRADLE_USER_HOME=%~dp0.gradle-home"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin;%PATH%"

echo ASCENDANT toolchain active (this session only):
echo   JAVA_HOME    = %JAVA_HOME%
echo   ANDROID_HOME = %ANDROID_HOME%
