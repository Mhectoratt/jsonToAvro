package com.example

import java.nio.file.*
import static java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.TimeUnit

class FileMonitorDaemon {

    private final Path inputDir
    private final Path outputDir
    private final JsonToAvroConverter converter
    private final Set<String> processedFiles = Collections.synchronizedSet(new HashSet<>())
    private volatile boolean running = true

    FileMonitorDaemon(String inputPath, String outputPath, String schemaPath) {
        this.inputDir = Paths.get(inputPath)
        this.outputDir = Paths.get(outputPath)
        this.converter = new JsonToAvroConverter(schemaPath)

        // Create directories if they don't exist
        if (!Files.exists(inputDir)) {
            Files.createDirectories(inputDir)
        }
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    /**
     * Start the daemon to monitor the input directory
     */
    void start() {
        println "File Monitor Daemon started"
        println "Monitoring directory: ${inputDir.toAbsolutePath()}"
        println "Output directory: ${outputDir.toAbsolutePath()}"
        println "Press Ctrl+C to stop"
        println ""

        // Process any existing files in the directory first
        processExistingFiles()

        // Start watching for new files
        watchDirectory()
    }

    /**
     * Process any existing JSON files in the input directory
     */
    private void processExistingFiles() {
        inputDir.toFile().listFiles()?.each { file ->
            if (file.isFile() && file.name.endsWith('.json')) {
                processJsonFile(file)
            }
        }
    }

    /**
     * Watch the input directory for new JSON files
     */
    private void watchDirectory() {
        WatchService watchService = FileSystems.getDefault().newWatchService()
        inputDir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY)

        try {
            while (running) {
                WatchKey key
                try {
                    // Wait for events (with timeout to allow checking running flag)
                    key = watchService.poll(1, TimeUnit.SECONDS)
                    if (key == null) {
                        continue
                    }
                } catch (InterruptedException e) {
                    println "Watch service interrupted"
                    break
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind()

                    if (kind == OVERFLOW) {
                        continue
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event
                    Path filename = ev.context()
                    Path fullPath = inputDir.resolve(filename)
                    File file = fullPath.toFile()

                    // Check if it's a JSON file
                    if (file.isFile() && file.name.endsWith('.json')) {
                        // Small delay to ensure file is fully written
                        Thread.sleep(100)
                        processJsonFile(file)
                    }
                }

                boolean valid = key.reset()
                if (!valid) {
                    println "Watch key no longer valid"
                    break
                }
            }
        } finally {
            watchService.close()
        }
    }

    /**
     * Process a JSON file and convert it to Avro
     * @param jsonFile The JSON file to process
     */
    private void processJsonFile(File jsonFile) {
        // Avoid processing the same file multiple times
        if (processedFiles.contains(jsonFile.name)) {
            return
        }

        try {
            println "Processing file: ${jsonFile.name}"

            // Generate output filename (replace .json with .avro)
            String outputFileName = jsonFile.name.replaceAll(/\.json$/, '.avro')
            File avroFile = new File(outputDir.toFile(), outputFileName)

            // Convert JSON to Avro
            converter.convertJsonToAvro(jsonFile, avroFile)

            println "Successfully converted ${jsonFile.name} to ${avroFile.name}"

            // Mark file as processed
            processedFiles.add(jsonFile.name)

            // Optionally, delete or move the processed JSON file
            // For now, we'll leave it in place
            // jsonFile.delete()

        } catch (Exception e) {
            System.err.println "Error processing file ${jsonFile.name}: ${e.message}"
            e.printStackTrace()
        }
    }

    /**
     * Stop the daemon
     */
    void stop() {
        running = false
    }
}
