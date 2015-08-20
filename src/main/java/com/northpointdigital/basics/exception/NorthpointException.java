package com.northpointdigital.com.exception;

public class NorthpointException extends Exception {

	private static final long serialVersionUID = 3572728608339258326L;
	
	private Exception exception;
    private String reason;
    
    public NorthpointException(Exception e, String reason) {
	this.exception = e;
	this.reason = reason;
    }
    
    public Exception getException() {
	return exception;
    }
    
    public String getReason() {
	return reason;
    }
}
