# json2avro

A Groovy project built with Gradle.

## Project Structure

```
json2avro/
├── build.gradle              # Gradle build configuration
├── settings.gradle           # Project settings
├── gradlew                   # Gradle wrapper script (Unix)
├── gradlew.bat               # Gradle wrapper script (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── groovy/
    │   │   └── com/
    │   │       └── example/
    │   │           └── Main.groovy
    │   └── resources/
    └── test/
        └── groovy/
            └── com/
                └── example/
                    └── MainSpec.groovy
```

## Requirements

- Java 8 or higher

## Building the Project

```bash
./gradlew build
```

## Running the Application

```bash
./gradlew run
```

## Running Tests

```bash
./gradlew test
```

## Other Useful Commands

- `./gradlew clean` - Clean build artifacts
- `./gradlew tasks` - List all available tasks
- `./gradlew dependencies` - Show project dependencies
