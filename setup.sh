#!/bin/bash
# ================================================
# Setup Script for E-Commerce Platform
# Run this FIRST to initialize Gradle wrapper
# ================================================

echo ""
echo "========================================"
echo " E-Commerce Platform - Initial Setup"
echo "========================================"
echo ""

# Check if Gradle is installed
if command -v gradle &> /dev/null; then
    echo "[OK] Gradle found, generating wrapper..."
    gradle wrapper --gradle-version 8.5
    chmod +x gradlew
    echo ""
    echo "[SUCCESS] Gradle wrapper generated!"
    echo "You can now use: ./gradlew build"
else
    echo "[INFO] Gradle not found in PATH."
    echo ""
    echo "Please do ONE of the following:"
    echo ""
    echo "OPTION 1: Use SDKMAN (recommended)"
    echo "  curl -s \"https://get.sdkman.io\" | bash"
    echo "  source ~/.sdkman/bin/sdkman-init.sh"
    echo "  sdk install gradle 8.5"
    echo "  Then run this script again"
    echo ""
    echo "OPTION 2: Install Gradle manually"
    echo "  - Download from: https://gradle.org/releases/"
    echo "  - Extract and add bin folder to PATH"
    echo "  - Run this script again"
    echo ""
    echo "OPTION 3: Use Homebrew (macOS)"
    echo "  brew install gradle"
    echo "  Then run this script again"
    echo ""
fi
