package com.northpoint.basics.events.search.impl;

import java.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import com.northpoint.basics.events.search.EventsSrch;
import com.northpoint.basics.events.search.FacetBuilder;
import com.northpoint.basics.events.search.FacetsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.tagging.TagManager;
import com.day.cq.tagging.Tag;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.Query;
import com.day.cq.search.facets.Facet;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

@Component
@Service
public class FacetBuilderImpl implements FacetBuilder{
	private static Logger log = LoggerFactory.getLogger(FacetBuilderImpl.class);
	public Map<String, List<FacetsInfo>> getFacets(SlingHttpServletRequest slingRequest, QueryBuilder queryBuilder, String FACETS_PATH ) {
		ResourceResolver resourceResolver = slingRequest.getResourceResolver();
		log.debug("Building Facets ");
		TagManager tagMgr = resourceResolver.adaptTo(TagManager.class);
		Resource tagResource = resourceResolver.getResource(FACETS_PATH);
		if (tagResource == null) {
			log.error("The repository requires " + FACETS_PATH + " to function properly to support tagging.");
//			throw new IllegalArgumentException("The facet path " + FACETS_PATH + " does not exit.");
			return null;
		}
		Iterator<Resource> resources = tagResource.listChildren();
		Map<String,List<FacetsInfo>> facets = new HashMap<String, List<FacetsInfo>>();
		while(resources.hasNext()) {
			Resource resource = resources.next();
		    Tag tag = tagMgr.resolve(resource.getPath());
		    List<FacetsInfo> tagItems = new ArrayList<FacetsInfo>();
			Iterator <Resource> childFacets= resourceResolver.listChildren(resource);
			while(childFacets.hasNext()){
				Tag cTag = tagMgr.resolve(childFacets.next().getPath());
				tagItems.add(new FacetsInfo(cTag.getTitle(),cTag.getTagID(),false,0L));
			}
			facets.put(tag.getName(), tagItems);
		}
		return facets;
	}
}
