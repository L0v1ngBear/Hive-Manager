@echo off
setlocal

set "JAVA_HOME=D:\idea\jdk-21.0.5"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "BASE_DIR=%~dp0"

if not exist "%BASE_DIR%target\print-bridge-0.0.1-SNAPSHOT.jar" (
  echo 未找到桥接程序 jar，正在先执行 mvn package...
  call mvn -q -f "%BASE_DIR%pom.xml" package
  if errorlevel 1 (
    echo 构建失败，请先检查 Java 和 Maven 环境。
    pause
    exit /b 1
  )
)

echo 正在启动本地打印桥接...
java -jar "%BASE_DIR%target\print-bridge-0.0.1-SNAPSHOT.jar"

endlocal
