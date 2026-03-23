#!/usr/bin/env sh
set -eu
if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
exec gradle "$@"
