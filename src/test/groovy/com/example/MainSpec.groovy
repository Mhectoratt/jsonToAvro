package com.example

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MainSpec extends Specification {

    @TempDir
    Path tempDir

    Path inputDir
    Path outputDir
    Path testDataDir

    def setup() {
        inputDir = tempDir.resolve("in")
        outputDir = tempDir.resolve("out")
        testDataDir = Paths.get("test")

        Files.createDirectories(inputDir)
        Files.createDirectories(outputDir)
    }

    def "test JsonToAvroConverter converts JSON to Avro successfully"() {
        given: "a JSON file to convert"
        def sourceJsonFile = testDataDir.resolve("test.json").toFile()
        def testJsonFile = inputDir.resolve("test.json").toFile()
        def avroFile = outputDir.resolve("test.avro").toFile()

        // Copy test file to input directory
        Files.copy(sourceJsonFile.toPath(), testJsonFile.toPath())

        and: "a converter instance"
        def converter = new JsonToAvroConverter("user_record.avsc")

        when: "converting the JSON file to Avro"
        converter.convertJsonToAvro(testJsonFile, avroFile)

        then: "the Avro file should be created"
        avroFile.exists()
        avroFile.length() > 0
    }

    def "test JsonToAvroConverter handles nullable fields correctly"() {
        given: "a JSON file with nullable fields"
        def sourceJsonFile = testDataDir.resolve("test2.json").toFile()
        def testJsonFile = inputDir.resolve("test2.json").toFile()
        def avroFile = outputDir.resolve("test2.avro").toFile()

        // Copy test file to input directory
        Files.copy(sourceJsonFile.toPath(), testJsonFile.toPath())

        and: "a converter instance"
        def converter = new JsonToAvroConverter("user_record.avsc")

        when: "converting the JSON file with nulls to Avro"
        converter.convertJsonToAvro(testJsonFile, avroFile)

        then: "the Avro file should be created successfully"
        avroFile.exists()
        avroFile.length() > 0
    }

    def "test FileMonitorDaemon processes existing files and deletes them"() {
        given: "a JSON file in the input directory"
        def sourceJsonFile = testDataDir.resolve("test.json").toFile()
        def testJsonFile = inputDir.resolve("test-existing.json").toFile()

        // Copy test file to input directory
        Files.copy(sourceJsonFile.toPath(), testJsonFile.toPath())

        and: "a file monitor daemon"
        def daemon = new FileMonitorDaemon(
            inputDir.toString(),
            outputDir.toString(),
            "user_record.avsc"
        )

        when: "processing existing files"
        daemon.processExistingFiles()

        then: "the Avro file should be created"
        outputDir.resolve("test-existing.avro").toFile().exists()

        and: "the original JSON file should be deleted"
        !testJsonFile.exists()
    }

    def "test FileMonitorDaemon creates output directories if they don't exist"() {
        given: "non-existent directories"
        def newInputDir = tempDir.resolve("new-in")
        def newOutputDir = tempDir.resolve("new-out")

        when: "creating a daemon with non-existent directories"
        new FileMonitorDaemon(
            newInputDir.toString(),
            newOutputDir.toString(),
            "user_record.avsc"
        )

        then: "the directories should be created"
        Files.exists(newInputDir)
        Files.exists(newOutputDir)
    }

    def "test converter handles missing optional fields"() {
        given: "a JSON file with only required fields"
        def minimalJson = inputDir.resolve("minimal.json").toFile()
        minimalJson.text = '''[
            {
                "timestamp": "2025-11-04T12:00:00Z",
                "id": "TEST-001",
                "userid": "U999"
            }
        ]'''

        def avroFile = outputDir.resolve("minimal.avro").toFile()

        and: "a converter instance"
        def converter = new JsonToAvroConverter("user_record.avsc")

        when: "converting minimal JSON to Avro"
        converter.convertJsonToAvro(minimalJson, avroFile)

        then: "the conversion should succeed"
        avroFile.exists()
        avroFile.length() > 0
    }

    def "test converter handles id as integer"() {
        given: "a JSON file with id as integer"
        def jsonWithIntId = inputDir.resolve("int-id.json").toFile()
        jsonWithIntId.text = '''[
            {
                "timestamp": "2025-11-04T12:00:00Z",
                "id": 123,
                "userid": "U888",
                "text": "Test with integer ID"
            }
        ]'''

        def avroFile = outputDir.resolve("int-id.avro").toFile()

        and: "a converter instance"
        def converter = new JsonToAvroConverter("user_record.avsc")

        when: "converting JSON with integer id to Avro"
        converter.convertJsonToAvro(jsonWithIntId, avroFile)

        then: "the conversion should succeed"
        avroFile.exists()
        avroFile.length() > 0
    }

    def "test multiple file processing and deletion"() {
        given: "multiple JSON files in the input directory"
        def sourceJsonFile1 = testDataDir.resolve("test.json").toFile()
        def sourceJsonFile2 = testDataDir.resolve("test2.json").toFile()
        def testJsonFile1 = inputDir.resolve("multi-test1.json").toFile()
        def testJsonFile2 = inputDir.resolve("multi-test2.json").toFile()

        Files.copy(sourceJsonFile1.toPath(), testJsonFile1.toPath())
        Files.copy(sourceJsonFile2.toPath(), testJsonFile2.toPath())

        and: "a file monitor daemon"
        def daemon = new FileMonitorDaemon(
            inputDir.toString(),
            outputDir.toString(),
            "user_record.avsc"
        )

        when: "processing existing files"
        daemon.processExistingFiles()

        then: "both Avro files should be created"
        outputDir.resolve("multi-test1.avro").toFile().exists()
        outputDir.resolve("multi-test2.avro").toFile().exists()

        and: "both original JSON files should be deleted"
        !testJsonFile1.exists()
        !testJsonFile2.exists()
    }

    def "test converter handles latitude and longitude float fields"() {
        given: "a JSON file with latitude and longitude coordinates"
        def jsonWithCoordinates = inputDir.resolve("coordinates.json").toFile()
        jsonWithCoordinates.text = '''[
            {
                "timestamp": "2025-11-06T10:00:00Z",
                "id": "LOC-001",
                "userid": "U777",
                "text": "Location data",
                "city": "San Francisco",
                "state": "CA",
                "latitude": 37.7749,
                "longitude": -122.4194
            },
            {
                "timestamp": "2025-11-06T10:01:00Z",
                "id": "LOC-002",
                "userid": "U888",
                "text": "Another location",
                "latitude": 40.7128,
                "longitude": -74.0060
            },
            {
                "timestamp": "2025-11-06T10:02:00Z",
                "id": "LOC-003",
                "userid": "U999",
                "text": "No coordinates"
            }
        ]'''

        def avroFile = outputDir.resolve("coordinates.avro").toFile()

        and: "a converter instance"
        def converter = new JsonToAvroConverter("user_record.avsc")

        when: "converting JSON with coordinates to Avro"
        converter.convertJsonToAvro(jsonWithCoordinates, avroFile)

        then: "the conversion should succeed"
        avroFile.exists()
        avroFile.length() > 0
    }
}
