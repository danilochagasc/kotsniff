#!/usr/bin/env bash

# This script installs the KotSniff CLI on Unix environments (Linux/macOS).
# It receives the path to the compiled installDist directory from Gradle.

if [ "$#" -ne 1 ]; then
    echo "Usage: install.sh <path_to_installDist>"
    exit 1
fi

INSTALL_DIST_DIR="$1"
TARGET_DIR="$HOME/.kotsniff"
BIN_DIR="$TARGET_DIR/bin"

echo "Installing KotSniff CLI to $TARGET_DIR..."

# Remove old installation if exists
if [ -d "$TARGET_DIR" ]; then
    rm -rf "$TARGET_DIR"
fi
mkdir -p "$TARGET_DIR"

# Copy the compiled application contents (bin/, lib/) directly into the target,
# so the launcher ends up at <target>/bin/kotsniff regardless of whether the
# target directory already existed.
cp -r "$INSTALL_DIST_DIR"/* "$TARGET_DIR"

# Ensure the executable has execution permissions
chmod +x "$BIN_DIR/kotsniff"

echo "Successfully copied KotSniff CLI to $TARGET_DIR"

# Try to add to PATH if not already there
SHELL_CONFIG=""
if [ -n "$ZSH_VERSION" ] || [ "$SHELL" = "/bin/zsh" ]; then
    SHELL_CONFIG="$HOME/.zshrc"
elif [ -n "$BASH_VERSION" ] || [ "$SHELL" = "/bin/bash" ]; then
    SHELL_CONFIG="$HOME/.bashrc"
else
    # Fallback for other environments
    if [ -f "$HOME/.bash_profile" ]; then
        SHELL_CONFIG="$HOME/.bash_profile"
    elif [ -f "$HOME/.profile" ]; then
        SHELL_CONFIG="$HOME/.profile"
    fi
fi

if [ -n "$SHELL_CONFIG" ] && [ -f "$SHELL_CONFIG" ]; then
    if ! grep -q "$BIN_DIR" "$SHELL_CONFIG"; then
        echo "Adding $BIN_DIR to PATH in $SHELL_CONFIG..."
        echo "" >> "$SHELL_CONFIG"
        echo "# KotSniff CLI" >> "$SHELL_CONFIG"
        echo "export PATH=\"\$PATH:$BIN_DIR\"" >> "$SHELL_CONFIG"
        echo "KotSniff CLI installed successfully!"
        echo "Please run 'source $SHELL_CONFIG' or restart your terminal to use the 'kotsniff' command."
    else
        echo "KotSniff CLI installed successfully! It is already in your PATH."
    fi
else
    echo "KotSniff CLI installed successfully!"
    echo "Please manually add $BIN_DIR to your PATH to use the 'kotsniff' command globally."
fi
