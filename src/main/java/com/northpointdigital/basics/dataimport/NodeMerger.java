package com.northpointdigital.basics.dataimport;

import org.apache.sling.api.resource.ResourceResolver;
import com.northpointdigital.basics.exception.NorthpointException;

public interface NodeMerger {
    String[] merge(String origPath, String destPath, ResourceResolver rr) throws NorthpointException;
}
