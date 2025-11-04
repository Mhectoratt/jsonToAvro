# JSON to Avro Converter Daemon

A Groovy-based daemon service that monitors a directory for JSON files and automatically converts them to Apache Avro format.

## Features

- **Automatic File Monitoring**: Watches the `in` directory for new JSON files
- **Real-time Conversion**: Converts JSON files to Avro format as soon as they are detected
- **Schema Support**: Uses an Avro schema that supports nullable fields
- **Graceful Shutdown**: Properly handles Ctrl+C shutdown signals

## Project Structure

```
jsonToAvro/
├── build.gradle                      # Gradle build configuration
├── in/                               # Input directory for JSON files
├── out/                              # Output directory for Avro files
├── test/                             # Sample JSON files for testing
│   ├── test.json
│   └── test2.json
└── src/
    ├── main/
    │   ├── groovy/
    │   │   └── com/example/
    │   │       ├── Main.groovy                    # Main entry point
    │   │       ├── FileMonitorDaemon.groovy       # File monitoring daemon
    │   │       └── JsonToAvroConverter.groovy     # JSON to Avro converter
    │   └── resources/
    │       └── user_record.avsc                   # Avro schema definition
    └── test/
        └── groovy/
            └── com/example/
                └── MainSpec.groovy
```

## Schema

The Avro schema (`user_record.avsc`) defines the following fields:

- **timestamp** (required, string): Timestamp in ISO 8601 format
- **id** (required, string): Unique identifier
- **userid** (required, string): User ID
- **text** (optional, string): Text content
- **error_code** (optional, string): Error code if applicable
- **username** (optional, string): Username
- **address** (optional, string): Address
- **city** (optional, string): City
- **state** (optional, string): State
- **postal_code** (optional, string): Postal code
- **phone_num** (optional, string): Phone number
- **mobile_num** (optional, string): Mobile number

All fields except `timestamp`, `id`, and `userid` can be null or missing from the JSON input.

## Requirements

- Java 8 or higher
- Gradle 8.5 or higher

## Building the Project

```bash
./gradlew build
```

Or with system Gradle:

```bash
gradle build
```

## Running the Daemon

### Default Usage

By default, the daemon monitors the `in` directory and outputs to the `out` directory:

```bash
./gradlew run
```

Or:

```bash
gradle run
```

### Custom Directories

You can specify custom input and output directories:

```bash
./gradlew run --args="<input-dir> <output-dir> <schema-path>"
```

Example:
```bash
./gradlew run --args="my-input my-output user_record.avsc"
```

### Running as a Standalone JAR

After building, you can also run the application directly:

```bash
java -cp build/libs/jsonToAvro-1.0-SNAPSHOT.jar:build/libs/* com.example.Main
```

## Testing the Daemon

1. Start the daemon:
   ```bash
   ./gradlew run
   ```

2. In another terminal, copy a test file to the `in` directory:
   ```bash
   cp test/test.json in/
   ```

3. Check the `out` directory for the converted Avro file:
   ```bash
   ls -la out/
   ```

4. The daemon will display:
   ```
   Processing file: test.json
   Successfully converted test.json to test.avro
   ```

## Sample JSON Format

The daemon expects JSON files containing an array of records:

```json
[
  {
    "timestamp": "2025-11-04T12:00:00Z",
    "id": "1",
    "userid": "U001",
    "text": "Sample text",
    "username": "user_one",
    "address": "123 Elm St",
    "city": "Springfield",
    "state": "IL",
    "postal_code": "62701",
    "phone_num": "555-1234",
    "mobile_num": "555-5678"
  }
]
```

Fields can be null or missing (except `timestamp`, `id`, and `userid`):

```json
[
  {
    "timestamp": "2025-11-04T12:00:01Z",
    "id": "2",
    "userid": "U002",
    "error_code": "404",
    "city": "Riverdale"
  }
]
```

## Stopping the Daemon

Press `Ctrl+C` to gracefully stop the daemon. The shutdown hook will clean up resources properly.

## How It Works

1. **FileMonitorDaemon**: Uses Java's WatchService API to monitor the input directory for file changes
2. **Processing**: When a JSON file is detected:
   - The file is read and parsed
   - Each record is validated against the Avro schema
   - Records are converted to Avro GenericRecord format
   - The Avro file is written to the output directory
3. **File Naming**: Output files use the same name as input files with `.avro` extension (e.g., `test.json` → `test.avro`)

## Development

### Running Tests

```bash
./gradlew test
```

### Clean Build

```bash
./gradlew clean build
```

### Other Useful Commands

- `./gradlew tasks` - List all available tasks
- `./gradlew dependencies` - Show project dependencies

## Dependencies

- Apache Groovy 4.0.15
- Apache Avro 1.11.3
- Jackson Databind 2.15.2 (for JSON parsing)

## Troubleshooting

### Files not being processed

- Ensure the JSON file is valid JSON format
- Check that the JSON contains an array of records
- Verify that required fields (`timestamp`, `id`, `userid`) are present

### Permission errors

- Ensure the daemon has read permissions for the `in` directory
- Ensure the daemon has write permissions for the `out` directory

### Schema validation errors

- Verify that your JSON structure matches the expected schema
- Check that required fields are not null or missing
