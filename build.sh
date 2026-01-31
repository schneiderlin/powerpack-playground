#!/bin/bash
set -e

# Vercel runs npm/pnpm install automatically when package.json exists.

# Install Clojure CLI to user directory if not present (no sudo on Vercel)
if ! command -v clojure &> /dev/null; then
  echo "Installing Clojure CLI tools..."
  curl -O https://download.clojure.org/install/linux-install-1.11.1.1347.sh
  chmod +x linux-install-1.11.1.1347.sh
  export PREFIX="$HOME/.local"
  mkdir -p "$PREFIX"
  ./linux-install-1.11.1.1347.sh --prefix "$PREFIX"
  rm linux-install-1.11.1.1347.sh
  export PATH="$PREFIX/bin:$PATH"
fi

# Build CSS (input.css -> styles.css in resources/public/css)
echo "Building CSS..."
npx @tailwindcss/cli -i ./resources/public/input.css -o ./resources/public/styles.css

# Run the static site build
echo "Building static site..."
clojure -X:build

echo "Build complete! Output in target/powerpack/"
