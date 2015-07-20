package com.northpoint.basics.events.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

public class SearchResultsInfo {
	private List<String> results;
	private Map<String,Map<String,Long>> facetsWithCount;
	private Map<String,ArrayList<String>> facts;
	private long hitCounts ;
	private SearchResult searchResults;
	private List<Hit> hits;
	private List<String> regions;
	
	public SearchResultsInfo(){
		results = new ArrayList<String>();
		facetsWithCount = new HashMap<String,Map<String,Long>>();
		facts = new HashMap<String,ArrayList<String>>();
	}
	
	public void setResults(List<String>results){
		this.results = results;
	}

	public List<String> getResults(){
		return results;
	}
	
	public void setHitCounts(long l){
		this.hitCounts = l;
	}
	
	public long getHitCounts(){
		return hitCounts;
	}
	
	public void setFacetsWithCounts(Map<String,Map<String,Long>> facets){
		this.facetsWithCount = facets;
	}
	
	public Map<String, ArrayList<String>> getFacts(){
		return this.facts;
	}
	
	public Map<String,Map<String,Long>> getFacetsWithCount(){
		return facetsWithCount;
	}
	
	public void setSearchResults(SearchResult searchResults){
		this.searchResults = searchResults;
	}
	
	public SearchResult getSearchResults(){
		return searchResults;
	}
	
	public void setResultsHits(List<Hit> hts){
		this.hits = hts;
		
	}
	
	public List<Hit> getResultsHits(){
		return hits;
	}
	
	public void setRegion(List<String> regions){
		
		this.regions = regions;
	}
	
	public List<String> getRegion(){
		return this.regions;
	}
	

}
