package com.example

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DatumWriter

class JsonToAvroConverter {

    private Schema schema
    private ObjectMapper objectMapper

    JsonToAvroConverter(String schemaPath) {
        // Load the Avro schema
        def schemaFile = getClass().getClassLoader().getResourceAsStream(schemaPath)
        if (!schemaFile) {
            throw new FileNotFoundException("Schema file not found: ${schemaPath}")
        }
        this.schema = new Schema.Parser().parse(schemaFile)
        this.objectMapper = new ObjectMapper()
    }

    /**
     * Convert a JSON file to Avro format
     * @param jsonFile Input JSON file
     * @param avroFile Output Avro file
     */
    void convertJsonToAvro(File jsonFile, File avroFile) {
        // Parse JSON file
        def jsonData = objectMapper.readValue(jsonFile, List)

        // Create Avro writer
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema)
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)

        try {
            dataFileWriter.create(schema, avroFile)

            // Convert each JSON record to Avro
            jsonData.each { record ->
                GenericRecord avroRecord = createAvroRecord(record)
                dataFileWriter.append(avroRecord)
            }

        } finally {
            dataFileWriter.close()
        }
    }

    /**
     * Create an Avro GenericRecord from a JSON map
     * @param jsonRecord JSON record as a Map
     * @return GenericRecord
     */
    private GenericRecord createAvroRecord(Map jsonRecord) {
        GenericRecord avroRecord = new GenericData.Record(schema)

        schema.getFields().each { field ->
            def fieldName = field.name()
            def value = jsonRecord[fieldName]

            // Convert value to string if needed (for id field which might be int)
            if (value != null && fieldName == 'id') {
                value = value.toString()
            }

            // Set the field value (null is allowed for optional fields)
            avroRecord.put(fieldName, value)
        }

        return avroRecord
    }
}
