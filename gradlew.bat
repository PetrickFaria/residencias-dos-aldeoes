@echo off
set APP_HOME=%~dp0
java -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared-7.5.1.jar;%APP_HOME%gradle\wrapper\gradle-cli-7.5.1.jar" org.gradle.wrapper.GradleWrapperMain %*
