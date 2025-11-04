package com.example

class Main {
    static void main(String[] args) {
        println "JSON to Avro Converter Daemon"
        println "=============================="
        println ""

        // Get directories from command line arguments or use defaults
        String inputDir = args.length > 0 ? args[0] : "in"
        String outputDir = args.length > 1 ? args[1] : "out"
        String schemaPath = args.length > 2 ? args[2] : "user_record.avsc"

        // Create and start the file monitor daemon
        FileMonitorDaemon daemon = new FileMonitorDaemon(inputDir, outputDir, schemaPath)

        // Add shutdown hook to gracefully stop the daemon
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            void run() {
                println "\nShutting down daemon..."
                daemon.stop()
            }
        })

        // Start the daemon
        daemon.start()
    }
}
