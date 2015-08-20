package com.northpoint.basics.search.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import com.northpoint.basics.events.search.FacetsInfo;
import com.northpoint.basics.events.search.SearchResultsInfo;
import com.northpoint.basics.search.DocHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.facets.Bucket;
import com.day.cq.search.facets.Facet;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

public class SearchUtils {
	private static Logger log = LoggerFactory.getLogger(SearchUtils.class);
	
	public static SearchResultsInfo combineSearchTagsCounts(SearchResultsInfo searchResultsInfo,Map<String,List<FacetsInfo>> facetAndTags)
	{
		if(searchResultsInfo.getFacetsWithCount().isEmpty())
		{
			return searchResultsInfo;
		}
		Iterator <String> everyThingFacets=null;
		try{
			everyThingFacets = facetAndTags.keySet().iterator();
		}catch(Exception e){
			log.error("Exception Caught" +e.getMessage());
		}
		Map<String, Map<String, Long>> facetsWithCounts = searchResultsInfo.getFacetsWithCount();
		while(everyThingFacets.hasNext()){
			String facetName = everyThingFacets.next();
			List <FacetsInfo> facetInfo = facetAndTags.get(facetName);
			log.debug("Facets Name ["+facetsWithCounts.toString() +"]");
			if(facetsWithCounts.containsKey(facetName)){
				Map<String, Long> fwc = facetsWithCounts.get(facetName);
				for(int i=0;i<facetInfo.size();i++) {
					if(fwc.containsKey(facetInfo.get(i).getFacetsTitle())) {
						facetInfo.get(i).setChecked(true);						
						facetInfo.get(i).setCount(fwc.get(facetInfo.get(i).getFacetsTitle()));
					}
				}
			}
			
		}
		return searchResultsInfo;	
	}
	public static List<Hit> performContentSearch(Map<String, String> map, SlingHttpServletRequest slingRequest, QueryBuilder builder) {
		PredicateGroup predicateGroup = PredicateGroup.create(map);
		Query query = builder.createQuery(predicateGroup,slingRequest.getResourceResolver().adaptTo(Session.class));
		query.setExcerpt(true);
		SearchResult searchResults=query.getResult();
		java.util.List<Hit> hits = searchResults.getHits();
		return hits;
   }
}
