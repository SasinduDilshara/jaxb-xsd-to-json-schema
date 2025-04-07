package com.example.xsd;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import jakarta.xml.bind.annotation.XmlElement;  // Changed from javax to jakarta

import com.fasterxml.jackson.databind.jsonschema.JsonSchema;

import static java.lang.System.out;
import static java.lang.System.err;

/**
 * Generates JavaScript Object Notation (JSON) from Java classes
 * with Java API for XML Binding (JAXB) annotations.
 */
public class XsdToJsonConverter
{
    /**
     * Create instance of ObjectMapper with JAXB introspector
     * and default type factory.
     *
     * @return Instance of ObjectMapper with JAXB introspector
     *    and default type factory.
     */
    private ObjectMapper createJaxbObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JakartaXmlBindAnnotationIntrospector introspector = new JakartaXmlBindAnnotationIntrospector();
        mapper.setAnnotationIntrospector(introspector);
        return mapper;
    }

    /**
     * Write out JSON Schema based upon Java source code in
     * class whose fully qualified package and class name have
     * been provided.
     *
     * @param mapper Instance of ObjectMapper from which to
     *     invoke JSON schema generation.
     * @param fullyQualifiedClassName Name of Java class upon
     *    which JSON Schema will be extracted.
     */
    private void writeToStandardOutputWithDeprecatedJsonSchema(
            final ObjectMapper mapper, final String fullyQualifiedClassName)
    {
        try
        {
            final JsonSchema jsonSchema = mapper.generateJsonSchema(Class.forName(fullyQualifiedClassName));
            out.println(jsonSchema);
        }
        catch (ClassNotFoundException cnfEx)
        {
            err.println("Unable to find class " + fullyQualifiedClassName);
        }
        catch (JsonMappingException jsonEx)
        {
            err.println("Unable to map JSON: " + jsonEx);
        }
    }

    /**
     * Accepts the fully qualified (full package) name of a
     * Java class with JAXB annotations that will be used to
     * generate a JSON schema.
     *
     * @param arguments One argument expected: fully qualified
     *     package and class name of Java class with JAXB
     *     annotations.
     */
    public static void main(String[] arguments)
    {
        final XsdToJsonConverter instance = new XsdToJsonConverter();
        final String fullyQualifiedClassName = "com.example.generated.PersonDetails";
        final ObjectMapper objectMapper = instance.createJaxbObjectMapper();
        instance.writeToStandardOutputWithDeprecatedJsonSchema(objectMapper, fullyQualifiedClassName);
    }
}