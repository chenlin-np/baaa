package com.northpointdigital.com.search;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.result.Hit;

public final class DocHit extends DocHitBase {
    private static final Logger log = LoggerFactory.getLogger(DocHit.class);
    private static final String JCR_TITLE_PROPERTY = "jcr:content/jcr:title";
    private static final String DC_TITLE_PROPERTY = "jcr:content/metadata/dc:title";
    private static final String DC_DESCRIPTION_PROPERTY = "jcr:content/metadata/dc:description";
    
    private final Hit hit;
    private static final Pattern STRONG_PATTERN = Pattern.compile("<strong>.*?</strong>");
    private static final Pattern STRIP_STRONG_PATTERN = Pattern.compile("</?strong>");
    private static final String MSWORD = "application/msword";
    private static final String APPL_VND = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Pattern[] EXCERPT_PATTERNS = {
        Pattern.compile("<.*?>"),
        Pattern.compile("<.*?$"),
        Pattern.compile("^.*?>")
    };

    public DocHit(Hit hit) throws RepositoryException {
        super(hit.getNode());
        this.hit = hit;
    }

    public String getTitle() throws RepositoryException {
        String excerpt = (String) this.hit.getExcerpts().get("jcr:title");
        if (excerpt != null) {
            return excerpt;
        }
        Node node = getPageOrAsset();
        try {
            String primaryType = node.getPrimaryNodeType().getName();
            if (primaryType.equals("cq:Page") && node.hasProperty(JCR_TITLE_PROPERTY)) {
                return node.getProperty(JCR_TITLE_PROPERTY).getString();
            } else if (primaryType.equals("dam:Asset") && node.hasProperty(DC_TITLE_PROPERTY)) {
            		if(node.getProperty(DC_TITLE_PROPERTY).isMultiple()){
            			Value[] value=node.getProperty(DC_TITLE_PROPERTY).getValues();
            			if((!value[0].getString().isEmpty()) && (value[0].getString()!=null)) {
							return(value[0].getString());
						}
            		}
                return node.getProperty(DC_TITLE_PROPERTY).getString();
            }
        } catch (Exception e) {
            log.info("Cannot get the title. Use node name instead.");
        }
        return getPageOrAsset().getName();
    }

    public String getExcerpt() throws RepositoryException {
        String excerpt = this.hit.getExcerpt();
       	if(excerpt == null) { 
		return "";
	}
	Matcher queryMatcher = STRONG_PATTERN.matcher(excerpt);
        String queryString = null;
        if (queryMatcher.find()) {
            queryString = STRIP_STRONG_PATTERN.matcher(queryMatcher.group()).replaceAll("");
        }

        for (Pattern pattern: EXCERPT_PATTERNS) {
            excerpt = pattern.matcher(excerpt).replaceAll("");
        }

        if (queryString != null) {
            excerpt = excerpt.replaceAll(queryString, "<strong>" + queryString + "</strong>");
        }
        
        if(excerpt.indexOf(MSWORD) >0){
        	excerpt = excerpt.replaceAll(MSWORD," ");
        }
        
        if(excerpt.indexOf(APPL_VND) >0){
        	excerpt = excerpt.replaceAll(APPL_VND," ");
        }
        return excerpt;
    }
    
    public String getDescription() {
        try {
            Node pageOrAssetNode = getPageOrAsset();
            if (pageOrAssetNode.hasProperty(DC_DESCRIPTION_PROPERTY)) {
            	if(pageOrAssetNode.getProperty(DC_DESCRIPTION_PROPERTY).isMultiple()){
            		Value[] value = pageOrAssetNode.getProperty(DC_DESCRIPTION_PROPERTY).getValues();
            		if((!value[0].getString().isEmpty()) && (value[0].getString()!=null)) {
            			return(value[0].getString());
					}
            	}
                return pageOrAssetNode.getProperty(DC_DESCRIPTION_PROPERTY).getString();
            }
        } catch (Exception e) {
            log.info("Cannot get description. Return empty string.");
        }

        return "";
    }

    public Map getProperties() throws RepositoryException {
        return this.hit.getProperties();
    }
}
