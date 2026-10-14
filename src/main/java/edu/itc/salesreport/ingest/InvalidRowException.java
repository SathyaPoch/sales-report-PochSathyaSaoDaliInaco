package edu.itc.salesreport.ingest;

public class InvalidRowException extends Exception {
    public InvalidRowException(String message) {
        super(message);
    }
}