@echo off
REM ================================================
REM Setup Script for E-Commerce Platform
REM Run this FIRST to initialize Gradle wrapper
REM ================================================

echo.
echo ========================================
echo  E-Commerce Platform - Initial Setup
echo ========================================
echo.

REM Check if Gradle is installed
where gradle >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [OK] Gradle found, generating wrapper...
    gradle wrapper --gradle-version 8.5
    echo.
    echo [SUCCESS] Gradle wrapper generated!
    echo You can now use: gradlew build
) else (
    echo [INFO] Gradle not found in PATH.
    echo.
    echo Please do ONE of the following:
    echo.
    echo OPTION 1: Install Gradle
    echo   - Download from: https://gradle.org/releases/
    echo   - Extract and add bin folder to PATH
    echo   - Run this script again
    echo.
    echo OPTION 2: Use SDKMAN (recommended)
    echo   - Install SDKMAN: https://sdkman.io/
    echo   - Run: sdk install gradle 8.5
    echo   - Run this script again
    echo.
    echo OPTION 3: Manual wrapper download
    echo   - Download gradle-wrapper.jar from:
    echo     https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar
    echo   - Place it in: gradle\wrapper\gradle-wrapper.jar
    echo.
)

pause
