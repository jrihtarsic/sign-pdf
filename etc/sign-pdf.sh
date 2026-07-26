#!/usr/bin/env bash
# Launches sign-pdf on Linux/macOS.
#
#   sign-pdf.sh                                                  -> Swing UI
#   sign-pdf.sh in.pdf out.pdf "Name" "Purpose" "Contact" 1 50 700 200 80  -> CLI
#
# Looks for sign-pdf-1.0-jar-with-dependencies.jar next to this script first
# (drop the jar in etc/ for a standalone deployment), then falls back to
# ../target (a fresh `mvn package` checkout).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_NAME="sign-pdf-1.0-jar-with-dependencies.jar"
JAVA_HOME=/opt/java/jdk-21.0.7+6/

JAR=""
for candidate in "$SCRIPT_DIR/$JAR_NAME" "$SCRIPT_DIR/../target/$JAR_NAME"; do
    if [ -f "$candidate" ]; then
        JAR="$candidate"
        break
    fi
done

if [ -z "$JAR" ]; then
    echo "Could not find $JAR_NAME next to this script or in ../target/." >&2
    echo "Build it first with: mvn package" >&2
    exit 1
fi

if [ "$#" -eq 0 ]; then
    exec ${JAVA_HOME}/bin/java -jar "$JAR"
else
    exec ${JAVA_HOME}/bin/java -cp "$JAR" org.r7c.pdf.pades.PadesUtils "$@"
fi
