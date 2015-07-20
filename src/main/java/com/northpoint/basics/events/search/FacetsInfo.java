package com.northpoint.basics.events.search;


public class FacetsInfo {
	
	
	private String facetsTitle;
	private String facetsTagId;
	private boolean isChecked=false;
	private Long counts;
	
	public FacetsInfo(String facetsTitle, String facetsTagId, boolean isChecked,Long counts){
		this.facetsTitle = facetsTitle;
		this.facetsTagId = facetsTagId;
		this.isChecked = isChecked;
		this.counts = counts;
	}
	
	
	public String getFacetsTitle() {
		return facetsTitle;
	}
	
	public String getFacetsTagId() {
		return facetsTagId;
	}
	
	
	public boolean isChecked() {
		return isChecked;
	}
	
	public void setChecked(boolean flag){
		this.isChecked = flag;
	}
	
	public void setCount(Long count){
		this.counts = count;
	}
	
	public Long getCounts(){
		return counts;
	}
	
}
