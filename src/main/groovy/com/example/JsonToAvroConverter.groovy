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

            // Coerce the value to match the Avro schema type
            def coercedValue = coerceValue(value, field.schema())

            // Set the field value (null is allowed for optional fields)
            avroRecord.put(fieldName, coercedValue)
        }

        return avroRecord
    }

    /**
     * Coerce a value to match the expected Avro schema type
     * @param value The input value from JSON
     * @param fieldSchema The Avro field schema
     * @return The coerced value
     */
    private Object coerceValue(Object value, Schema fieldSchema) {
        // Handle null values
        if (value == null) {
            return null
        }

        // Get the actual type from the schema (handle union types like ["null", "string"])
        Schema.Type targetType = getActualType(fieldSchema)

        // If we can't determine the type, return the value as-is
        if (targetType == null) {
            return value
        }

        try {
            switch (targetType) {
                case Schema.Type.STRING:
                    return value.toString()

                case Schema.Type.INT:
                    if (value instanceof Number) {
                        return ((Number) value).intValue()
                    } else if (value instanceof String) {
                        return Integer.parseInt(value.toString())
                    }
                    return value

                case Schema.Type.LONG:
                    if (value instanceof Number) {
                        return ((Number) value).longValue()
                    } else if (value instanceof String) {
                        return Long.parseLong(value.toString())
                    }
                    return value

                case Schema.Type.FLOAT:
                    if (value instanceof Number) {
                        return ((Number) value).floatValue()
                    } else if (value instanceof String) {
                        return Float.parseFloat(value.toString())
                    }
                    return value

                case Schema.Type.DOUBLE:
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue()
                    } else if (value instanceof String) {
                        return Double.parseDouble(value.toString())
                    }
                    return value

                case Schema.Type.BOOLEAN:
                    if (value instanceof Boolean) {
                        return value
                    } else if (value instanceof String) {
                        return Boolean.parseBoolean(value.toString())
                    } else if (value instanceof Number) {
                        return ((Number) value).intValue() != 0
                    }
                    return value

                default:
                    return value
            }
        } catch (NumberFormatException e) {
            System.err.println "Warning: Could not coerce value '${value}' to type ${targetType}. Using original value."
            return value
        }
    }

    /**
     * Get the actual type from a schema, handling union types
     * @param schema The Avro schema (might be a union)
     * @return The actual type, or null if it's a complex union
     */
    private Schema.Type getActualType(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            // For union types like ["null", "string"], find the non-null type
            for (Schema unionSchema : schema.getTypes()) {
                if (unionSchema.getType() != Schema.Type.NULL) {
                    return unionSchema.getType()
                }
            }
            return null
        } else {
            return schema.getType()
        }
    }
}
