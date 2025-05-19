#!/bin/bash

# Set up environment variables from .env file if it exists
if [ -f .env ]; then
  export $(cat .env | grep -v '^#' | xargs)
fi

# Run the application using kotlin command
kotlinc -cp "$(find . -name '*.jar' | tr '\n' ':')src/main/kotlin" -script src/main/kotlin/com/emailprocessor/Main.kt
