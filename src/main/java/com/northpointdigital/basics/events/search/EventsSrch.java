package com.northpoint.basics.events.search;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import com.northpoint.basics.events.search.impl.FacetBuilderImpl;
import com.northpoint.basics.search.utils.SearchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;

public class EventsSrch  
{
	private static Logger log = LoggerFactory.getLogger(EventsSrch.class);
	private SlingHttpServletRequest slingRequest;
	private QueryBuilder queryBuilder;
	private Map<String,List<FacetsInfo>> facetAndTags = new HashMap<String,List<FacetsInfo>>();
	
	private static String FACETS_PATH = "/etc/tags/norhtpoint";
	
	private final String COUNCIL_SPE_PATH = "/etc/tags/";
	private static String EVENTS_PROP="jcr:content/cq:tags";
	
	
 	private SearchResultsInfo searchResultsInfo;
 	int propertyCounter = 1;
 	LinkedHashMap<String, String> searchQuery = new LinkedHashMap<String, String>();
	
	// Which return the object of Facets as Well as the Results;
	
	public EventsSrch(SlingHttpServletRequest slingRequest,QueryBuilder builder){
		this.slingRequest = slingRequest;
		this.queryBuilder = builder;
	}
	
	public void search(String q,String[] tags,String offset, String month,String year, String startdtRange, String enddtRange, String region,String path,String facetsPath )
	{
		try {
			createFacets(facetsPath);
			eventResults(q,offset,month,year,startdtRange,enddtRange,region,tags,path);
			
			// Don't delete this code if in future client need count for the tagging just enable this functionality.
			// searchResultsInfo = SearchUtils.combineSearchTagsCounts(searchResultsInfo,facetAndTags);
			
		} catch (RepositoryException e) {
			log.error("Error Generated in the search() of EventSrch Class");
			e.printStackTrace();
		}
	}
	
	public Map<String,List<FacetsInfo>> getFacets() {
		return facetAndTags;
	}
	
	private void createFacets(String facetPath) throws RepositoryException{
		FacetBuilder facetBuilder = new FacetBuilderImpl();
		if(facetPath!=null && !facetPath.isEmpty()){
			facetAndTags = facetBuilder.getFacets(slingRequest, queryBuilder, COUNCIL_SPE_PATH+facetPath );
		}
		if(facetAndTags==null){
			facetAndTags = facetBuilder.getFacets(slingRequest, queryBuilder, FACETS_PATH );
		}
	}
	
	
	private void eventResults(String q,String offset,String month,String year, String startdtRange, String enddtRange,String region,String[] tags, String path) throws RepositoryException{

		
		this.searchResultsInfo = new SearchResultsInfo();
		
		// Generate Regions
		searchResultsInfo.setRegion(eventRegions(path));

		if(q!=null && !q.isEmpty()){
			log.info("Search Query Term [" +q +"]");
			searchQuery.put("fulltext",q);
		}
		searchQuery.put("type", "cq:Page");
		searchQuery.put("path",path);
		searchQuery.put("1_property",EVENTS_PROP);
		searchQuery.put("p.limit", "-1");
		searchQuery.put("orderby","@jcr:content/data/start");
		searchQuery.put("orderby.sort", "asc");
		log.debug("Query Parameter : " +q);
		
		if(tags.length > 0) {
			try{
				addToDefaultQuery(searchQuery,tags);
			}catch(Exception e){
				log.error("Tagging could be parsed correctly");
			}
		}
		
		if((startdtRange!=null  && !startdtRange.isEmpty()) && (enddtRange!=null && !enddtRange.isEmpty())){
			addDateRangesToQuery(startdtRange, enddtRange,searchQuery);
		}
		
		if(region!=null && !region.isEmpty() && !region.equals("choose")){
			addRegionToQuery(region);
		}
		
		List<String> relts = new ArrayList<String>(); 
		List<Hit>hits = SearchUtils.performContentSearch(searchQuery,slingRequest,this.queryBuilder);
		if(hits!=null && !hits.isEmpty()){
			for(int i=0;i<hits.size();i++){
				Hit ht = hits.get(i);
				String pth = ht.getNode().isNodeType("cq:Page")?ht.getPath():ht.getPath();
				relts.add(pth);
			}
		}		
		searchResultsInfo.setResults(relts);
	}
	
	public SearchResultsInfo getSearchResultsInfo(){
		return this.searchResultsInfo;
	}
	
	private void addToDefaultQuery(Map<String, String> searchQuery,String[] tags) throws UnsupportedEncodingException{
		
		Map<String, List<String>> facetsQryBuilder = new HashMap<String, List<String>>();
		for(String s:tags) {
			String temp = URLDecoder.decode(s,"UTF-8");
			// categories/badge
			String key = temp.substring(temp.indexOf(":")+1,temp.length());
			String category = key.substring(0,key.indexOf("/"));
			if(facetsQryBuilder.containsKey(category)){
				facetsQryBuilder.get(category).add(temp);
			}else{
				List<String> value = new ArrayList<String>();
				value.add(s);
				facetsQryBuilder.put(category, value);
			}
		}
		Object[] categories = facetsQryBuilder.keySet().toArray();
		for(int i=0;i<categories.length;i++){
			List<String> tagsPath = facetsQryBuilder.get(categories[i].toString());
			int count=1;
			if(i>0){
				searchQuery.put("gsproperty", "jcr:content/cq:tags");
				searchQuery.put("gsproperty.or", "true");
				for(String tagPath:tagsPath ) {
					searchQuery.put("gsproperty."+count+++"_value", tagPath);
				}
			}else{
					searchQuery.put("1_property.or", "true");
					for(String tagPath:tagsPath) {
						searchQuery.put("1_property."+count+++"_value", tagPath);
					}
			}
		}
		
	}
	
	private void addRegionToQuery(String region){
		searchQuery.put(++propertyCounter+"_property", "jcr:content/data/region"); 
		searchQuery.put(propertyCounter+"_property.value", region);
		
	}
	
	private void addDateRangeQuery(String lowerBound, String upperBound){
		searchQuery.put("daterange.property","jcr:content/data/start" );
		if(!lowerBound.isEmpty()){
			searchQuery.put("daterange.lowerBound",lowerBound);
		}
		if(!upperBound.isEmpty()){	
			searchQuery.put("daterange.upperBound",upperBound);
		}
		
	}
	private void addDateRangesToQuery(String startdtRange, String enddtRange,Map<String, String> searchQuery){
		log.debug("startdtRange" +startdtRange  +"enddtRange" +enddtRange);
		DateFormat parse = new SimpleDateFormat("MM/dd/yyyy");
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date startRange = null;
		Date endRange =null;
		String strRange="";
		String edRange="";
		try {
			if(!startdtRange.isEmpty()) {
				String startDtDecoder = URLDecoder.decode(startdtRange,"UTF-8");
				startRange = (Date)parse.parse(startDtDecoder);
				strRange = formatter.format(startRange);
			}
			if(!enddtRange.isEmpty()) {
				String endDtDecoder= URLDecoder.decode(enddtRange,"UTF-8");
				endRange = (Date)parse.parse(endDtDecoder);
				edRange = formatter.format(endRange);
			}
		}catch (Exception e) {
			log.error("Error ::::::::::::::[" +e.getMessage() +"]");
			
		}
		addDateRangeQuery(strRange,edRange);
		
	}
	
	private List<String> eventRegions(String path) {
		Map<String, String> region = new LinkedHashMap<String, String>();
		
		// As compare after() depends on the today which keeps on changing as it has a time format in it
		// So we need to convert it into the date format yyyy-MM-dd so event and today return the same 
		Date today = new Date();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String startDateStr = formatter.format(today);
		try{
			today = formatter.parse(startDateStr);
		}catch(Exception e){
			
		}
		region.put("type", "cq:Page");
		region.put("path",path);
		region.put("p.limit", "-1");
		Set<String> regions = new HashSet<String>();
		long startTime = System.nanoTime();
		Date eventDate = null;
		String eventDt = "";
		try {
			List<Hit> hits = SearchUtils.performContentSearch(region,slingRequest,this.queryBuilder);
			for(Hit hts : hits) {
				eventDate = null;
				eventDt = "";
				Node node = hts.getNode();
				if(node.hasNode("jcr:content/data")) {
					try {
						Node propNode = node.getNode("jcr:content/data");
						if(propNode.hasProperty("region")) {
							if(propNode.hasProperty("end")){
								eventDate = propNode.getProperty("end").getDate().getTime();
							}
							else if(propNode.hasProperty("start")) {
								eventDate = propNode.getProperty("start").getDate().getTime();
							}
							eventDt = formatter.format(eventDate);
								// As compare after() depends on the today which keeps on changing as it has a time format in it
								// So we need to convert it into the date format yyyy-MM-dd so event and today return the same 
							try{
								eventDate = formatter.parse(eventDt);
							}catch(Exception e){
								log.error("couldn't parse the date");
							}
								//System.out.println("EventDate" +eventDate  +today);
							if(eventDate.after(today) || eventDate.equals(today)) {
								regions.add(propNode.getProperty("region").getString());
							}
						}
						
					}catch(Exception e) {
						log.error("Event Node doesn't contains required jcr:content/data Node" +e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			log.error(" Problem Occured whne performing search" +e.getMessage());
		}
		List<String> list = new ArrayList<String>(regions);
		Collections.sort(list);
		long endTime = System.nanoTime();
		log.debug("-------------Time Take in Milliseconds --------------" +(endTime - startTime)/1000000);
		return list;
		
	}
	

}
