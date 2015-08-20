package com.northpointdigital.com.dataimport;

import org.apache.sling.api.resource.ResourceResolver;
import com.northpointdigital.com.exception.NorthpointException;

public interface NodeMerger {
    String[] merge(String origPath, String destPath, ResourceResolver rr) throws NorthpointException;
}
