package com.northpointdigital.com.search.formsdocuments.impl;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import com.northpointdigital.com.events.search.FacetBuilder;
import com.northpointdigital.com.events.search.FacetsInfo;
import com.northpointdigital.com.events.search.SearchResultsInfo;
import com.northpointdigital.com.search.DocHit;
import com.northpointdigital.com.search.formsdocuments.FormsDocumentsSearch;
import com.northpointdigital.com.search.utils.SearchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.testing.client.security.Group;
import com.day.cq.search.Predicate;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.facets.Bucket;
import com.day.cq.search.facets.Facet;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

@Component
@Service
public class FormsDocumentsSearchImpl implements FormsDocumentsSearch {
	@Reference
	FacetBuilder facetBuilder;

	private long startTime, endTime;
	
	private static Logger log = LoggerFactory.getLogger(FormsDocumentsSearchImpl.class);
	private static String FACETS_PATH = "/etc/tags/northpoint";

	private final String COUNCIL_SPE_PATH = "/etc/tags/";

	private SlingHttpServletRequest slingRequest;
	private QueryBuilder queryBuilder;

	private Map<String, List<FacetsInfo>> facets;
	private SearchResultsInfo searchResultsInfo;
	
	private final String FORM_DOC_CATEGORY = "forms_documents";

	public FormsDocumentsSearchImpl(){}

	private Map<String, List<FacetsInfo>> loadFacets(String councilSpPath){
		Map<String, List<FacetsInfo>> fts = null;
		log.info("councilSpPath  [" +councilSpPath +"]");
			fts = facetBuilder.getFacets(this.slingRequest, this.queryBuilder, councilSpPath);
		return fts;
	}
	
	public void executeSearch(SlingHttpServletRequest slingRequest, QueryBuilder queryBuilder, String q, String path,String[] checkedTags,String councilSpecificPath, String formDocumentContentPath){
		this.queryBuilder = queryBuilder;
		this.slingRequest = slingRequest;
		searchResultsInfo = new SearchResultsInfo();

		if(!councilSpecificPath.isEmpty() && councilSpecificPath!=null){
			String councilSpPath=COUNCIL_SPE_PATH+councilSpecificPath;
			this.facets = loadFacets(councilSpPath);
		}
		try{
			documentsSearch(path,q,checkedTags,formDocumentContentPath);
		}catch(RepositoryException re){
			re.printStackTrace();
		}

	}

	private void documentsSearch(String path,String q, String[] tags,String formDocumentContentPath) throws RepositoryException{

		startTime = new Date().getTime();
		System.out.println("Start Time: " + startTime);
				
		Map<String,String> mapPath = new HashMap <String,String>();
		
		mapPath.put("group.p.or","true");
		mapPath.put("group.1_group.path", formDocumentContentPath);
		mapPath.put("group.2_group.path",path);
		
		PredicateGroup predicatePath =PredicateGroup.create(mapPath);
		
		Map<String,String> mapContentDoc = new HashMap <String,String>();
		mapContentDoc.put("group.p.or","true");
		mapContentDoc.put("group.1_group.type", "cq:Page");
		if((q!=null && !q.isEmpty()) || tags.length>0) {
			mapContentDoc.put("group.2_group.type", "nt:hierarchyNode");
		}else{
			mapContentDoc.put("group.2_group.type", "dam:AssetContent");
		}
		PredicateGroup predicateDocs =PredicateGroup.create(mapContentDoc);
		
		Map<String,String> masterMap  = new HashMap<String,String>();
		masterMap.put("p.limit", "-1");
		
		
		PredicateGroup master = PredicateGroup.create(masterMap); 
		master.add(predicatePath);
		master.add(predicateDocs);
		
		if(q!=null && !q.isEmpty()) {
			log.info("Search Query Term [" +q +"]");
			Map<String,String> mapFullText = new HashMap<String,String>();
			mapFullText.put("group.p.or","true" );
			mapFullText.put("group.1_fulltext", q);
			mapFullText.put("group.1_fulltext.relPath", "@jcr:content/jcr:title"); // search cq:tags
			mapFullText.put("group.2_fulltext", q);
			mapFullText.put("group.2_fulltext.relPath", "@jcr:content/metadata/dc:title"); // search title
			mapFullText.put("group.3_fulltext", q);
			mapFullText.put("group.3_fulltext.relPath", "@jcr:content/metadata/dc:description"); // search description
			mapFullText.put("group.4_fulltext", q); //search everything, including file contents via PDFBox
			PredicateGroup predicateFullText = PredicateGroup.create(mapFullText);
			master.add(predicateFullText);
			
		}
		if(tags.length > 0) {
			Map<String,String> checkedTagMap = new HashMap<String,String>();
			checkedTagMap = addToDefaultQuery(tags);
			PredicateGroup predicateCheckedTags = PredicateGroup.create(checkedTagMap);
			predicateCheckedTags.setAllRequired(false);
			master.add(predicateCheckedTags);
		}
		
		master.setAllRequired(true);
		List<Hit> searchTermHits = new ArrayList<Hit>();
		searchTermHits = performContentSearch(master,q);
		//System.out.println("Length: " + searchTermHits.size());
		
		List<Hit> titleHits = new ArrayList<Hit>();
		List<Hit> descriptionHits = new ArrayList<Hit>();
		List<Hit> contentHits = new ArrayList<Hit>();
		
		for(Hit h : searchTermHits){
			DocHit d = new DocHit(h);			
			if(d.getTitle().toLowerCase().contains(q.toLowerCase())){
				titleHits.add(h);
			}
			else if(d.getDescription().toLowerCase().contains(q.toLowerCase())){
				descriptionHits.add(h);
			}
			else{
				contentHits.add(h);
			}
		}
		
		List<Hit> sortedList = new ArrayList<Hit>();
		
		for(Hit title : titleHits){
			sortedList.add(title);
		}
		for(Hit desc : descriptionHits){
			sortedList.add(desc);
		}
		for(Hit content : contentHits){
			sortedList.add(content);
		}
		
		this.searchResultsInfo.setResultsHits(sortedList);
		this.searchResultsInfo = combineSearchTagsCounts();
		
		endTime = new Date().getTime();
		System.out.println("End Time: " + endTime);
		System.out.println("Time elapsed: " + (endTime - startTime));
		
	}
	
	private SearchResultsInfo combineSearchTagsCounts() throws RepositoryException
	{
		
		//Iterator <String> everyThingFacets=null;
		List<FacetsInfo> facetsInfo = null;
		try{
			 facetsInfo = this.facets.get(FORM_DOC_CATEGORY);
		}catch(Exception e){
			log.error("No Forms and Documents Tags Found in the /etc/tags/" +e.getMessage());
		}
		List<Hit> searchTermHits = this.searchResultsInfo.getResultsHits();
		
		
		// Duplicate Documents containing the same path are return when performing a search due to different renditions.
		// So put the hit in to the uni TreeMap to remove duplicates.

		Map<String, DocHit> unq= new java.util.TreeMap<String,DocHit>();
		java.util.List<Hit> hits = new java.util.ArrayList<Hit>();
		for(Hit hit:searchTermHits)  {
			//System.out.println("Hit" +hit.getPath());
			DocHit docHit = new DocHit(hit);
			if(!unq.containsKey(docHit.getURL())){
				//System.out.println("Hit: " + docHit.getURL());
				unq.put(docHit.getURL(), docHit);
				hits.add(hit);
			}
		}
		Iterator<String> uniIterator = unq.keySet().iterator();
		Map<String,Long> facetWithCount = new HashMap<String, Long>();
		while(uniIterator.hasNext()) {
			try{
				// Get the path of the hits
				String cPath = unq.get(uniIterator.next()).getURL();
				Node node = this.slingRequest.getResourceResolver().getResource(cPath+"/jcr:content").adaptTo(Node.class);
				// This is specific to the PDF and other DOC types, Since HTML document has cq:tags on the JCR:CONTENT, but not pdf and docx
				if(node.hasNode("metadata")){
					node = node.getNode("metadata");
					
				}
				if(node.hasProperty("cq:tags")){
					// We need to check for the multiple properties.
					Property tagProps = node.getProperty("cq:tags");
					Value[] value = null;

					if(tagProps.isMultiple()){
						value = tagProps.getValues();
					}else{
						value = new Value[]{tagProps.getValue()};
					}
					for(Value val:value){
						String valueString = val.getString();
						if(facetWithCount.containsKey(valueString)){
							facetWithCount.put(valueString, facetWithCount.get(valueString).longValue()+1L);
						}else{
							facetWithCount.put(valueString, new Long(1L));
						}
					}
				}
			}catch(Exception e){
				log.info("No Metadata found on the content" +e.getMessage());
			}
		}
		
		//Populating the count for the search results.
		for(FacetsInfo info : facetsInfo){
			if(facetWithCount.containsKey(info.getFacetsTagId())){   
				info.setCount(facetWithCount.get(info.getFacetsTagId()));
			}
		}
		this.searchResultsInfo.setResultsHits(hits);
		return this.searchResultsInfo;	
	}
	
	private Map<String,String> addToDefaultQuery(String[] tags) throws RepositoryException{
		
		Map<String,String> tagSearch = new HashMap<String,String>();
		tagSearch.put("1_property","jcr:content/metadata/cq:tags");
		tagSearch.put("1_property.or","true");
		tagSearch.put("2_property", "jcr:content/cq:tags");
		tagSearch.put("2_property.or", "true");
		int propertyCounter = 1;
		int count = 0;
		for(String tagPath:tags) {
				count++;
				log.info("Tag :::   ["+tagPath +"]");
				tagSearch.put("1_property." + count + "_value", tagPath);
				tagSearch.put("2_property." + count + "_value", tagPath);
			}
		return tagSearch;
	}

	public Map<String, List<FacetsInfo>> getFacets(){
		return this.facets;
	}
	public SearchResultsInfo getSearchResultsInfo(){
		return searchResultsInfo;
	}

	private List<Hit> performContentSearch(PredicateGroup master,String q) throws RepositoryException{
		
		//PredicateGroup predicateGroup = PredicateGroup.create(master);
		Query query = this.queryBuilder.createQuery(master,slingRequest.getResourceResolver().adaptTo(Session.class));
		query.setExcerpt(true);
		log.info("***SQL:*******[ "+master.toString() +"]");
		SearchResult searchResults=null;
		try{
			searchResults = query.getResult();
		}catch(Exception e){
			log.error("Error Generated performContentSearch" +e.getStackTrace());
		}
		this.searchResultsInfo.setSearchResults(searchResults);
		java.util.List<Hit> hits = searchResults.getHits();
		
		return hits;
	}

	
}

