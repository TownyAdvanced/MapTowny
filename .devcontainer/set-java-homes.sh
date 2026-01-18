#!/usr/bin/env bash

# Ensure SDKMAN is available
if ! command -v sdk >/dev/null 2>&1; then
  echo "❌ sdk command not found. Is SDKMAN! initialized?"
  echo "Try: source \"$HOME/.sdkman/bin/sdkman-init.sh\""
  return 1 2>/dev/null || exit 1
fi

while IFS= read -r candidate; do
  major_version="$(echo "$candidate" | sed -E 's/^([0-9]+).*/\1/')"
  java_home="$(sdk home java "$candidate" 2>/dev/null)"

  if [[ -n "$java_home" && -d "$java_home" ]]; then
    var_name="JAVA${major_version}_HOME"
    export "${var_name}=${java_home}"
    echo "✔ Set ${var_name}=${java_home}"
  fi
done < <(
  sdk list java | awk '/installed/ {print $NF}'
)