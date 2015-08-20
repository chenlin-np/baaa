package com.northpointdigital.com.dataimport;

import java.util.List;
import com.northpointdigital.com.exception.NorthpointException;

public interface DataImporter {
    static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SZ";
    static String TMP_ROOT = "/tmp/data-import/csv";

    String[] doDryRun() throws NorthpointException;
    String getDryRunPath() throws NorthpointException;
    boolean isDryRunSuccess();
    List<String[]> getFields();
}
