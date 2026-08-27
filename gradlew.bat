@echo off
setlocal
set DIR=%~dp0
set JAVA_EXE=C:\Program Files\Android\Android Studio\jbr\bin\java.exe
"%JAVA_EXE%" -cp "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
