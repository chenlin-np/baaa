package com.northpoint.basics.dataimport;

import java.io.Reader;

import org.apache.sling.api.resource.ResourceResolver;

public interface DataImporterFactory {
    DataImporter getDataImporter(String type, Reader reader, ResourceResolver rr, String confPath, String destPath);
}
