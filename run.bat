@echo off
setlocal

REM ===================================================================
REM Vaccination Drive Scheduler - Manual Build and Execution Script
REM Core Java 21+ | No Maven | Pure JDK and MySQL JDBC
REM ===================================================================

set LIB_JAR=lib\mysql-connector-j-9.2.0.jar
set CLASSPATH=bin;%LIB_JAR%;config

echo ===================================================================
echo     VACCINATION DRIVE SCHEDULER - BUILD AND RUNNER
echo ===================================================================

if not exist "%LIB_JAR%" (
    echo [ERROR] MySQL Connector/J JAR not found at %LIB_JAR%
    exit /b 1
)

if not exist "bin" (
    mkdir "bin"
)

echo [1/2] Compiling Java source files...
javac -encoding UTF-8 -cp "%LIB_JAR%;src" -d bin src\com\vaccination\exception\*.java src\com\vaccination\model\*.java src\com\vaccination\util\*.java src\com\vaccination\dao\*.java src\com\vaccination\service\*.java src\com\vaccination\main\*.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Compilation successful! Output written to bin\

echo.
if "%~1"=="" (
    echo [2/2] Running Main application...
    echo ===================================================================
    java -cp "%CLASSPATH%" com.vaccination.main.Main
) else (
    echo [2/2] Running %~1...
    echo ===================================================================
    java -cp "%CLASSPATH%" %~1
)

endlocal
