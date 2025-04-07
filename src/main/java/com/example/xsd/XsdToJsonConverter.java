package com.example.xsd;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.System.out;
import static java.lang.System.err;

public class XsdToJsonConverter {

    private ObjectMapper createJaxbObjectMapper() {
        return JsonMapper.builder()
                .annotationIntrospector(new JakartaXmlBindAnnotationIntrospector())
                .build();
    }

    /**
     * Returns a list of all class names in the specified package
     * @param packageName The package to scan
     * @return List of fully qualified class names
     */
    private List<String> getClassesInPackage(String packageName) {
        List<String> classNames = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if (protocol.equals("file")) {
                    // Handle file system resources
                    File directory = new File(resource.toURI());
                    if (directory.exists()) {
                        File[] files = directory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile() && file.getName().endsWith(".class")) {
                                    String className = packageName + '.' +
                                            file.getName().substring(0, file.getName().length() - 6);
                                    classNames.add(className);
                                } else if (file.isDirectory()) {
                                    // Recursively scan subpackages
                                    String subPackage = packageName + "." + file.getName();
                                    classNames.addAll(getClassesInPackage(subPackage));
                                }
                            }
                        }
                    }
                } else if (protocol.equals("jar")) {
                    // Handle JAR file resources
                    String jarPath = resource.getPath();
                    if (jarPath.contains("!")) {
                        String[] parts = jarPath.split("!");
                        URL jarUrl = new URL(parts[0]);

                        try (JarFile jar = new JarFile(new File(jarUrl.toURI()))) {
                            Enumeration<JarEntry> entries = jar.entries();
                            String packagePath = path + "/";

                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String entryName = entry.getName();

                                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                                    // Convert path to class name format
                                    String className = entryName.replace('/', '.')
                                            .substring(0, entryName.length() - 6);
                                    classNames.add(className);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning package: " + e.getMessage());
            e.printStackTrace();
        }
        return classNames;
    }

    private void generateSchemaForClass(ObjectMapper mapper, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isEnum() || clazz.isInterface() || clazz.isAnnotation()) {
                return; // Skip non-class types
            }

            out.println("\nGenerating JSON Schema for: " + className);
            JsonSchema schema = mapper.generateJsonSchema(clazz);
            out.println(schema);
        } catch (ClassNotFoundException e) {
            err.println("Class not found: " + className);
        } catch (JsonMappingException e) {
            err.println("Failed to generate schema for " + className + ": " + e.getMessage());
        } catch (Exception e) {
            err.println("Unexpected error processing " + className + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        XsdToJsonConverter converter = new XsdToJsonConverter();
        ObjectMapper mapper = converter.createJaxbObjectMapper();

        // Get all classes in the package
        String packageName = "com.example.jaxb";
        List<String> classNames = converter.getClassesInPackage(packageName);

        if (classNames.isEmpty()) {
            err.println("No classes found in package: " + packageName);
            return;
        }

        // Generate schemas for all found classes
        out.println("Found " + classNames.size() + " classes in package " + packageName + ":");
        out.println(classNames);
        for (String className : classNames) {
            if (className.endsWith("ObjectFactory")) {
                continue;
            }
            converter.generateSchemaForClass(mapper, className);
        }
    }
}