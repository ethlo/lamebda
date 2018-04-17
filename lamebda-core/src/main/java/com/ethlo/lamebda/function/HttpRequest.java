package com.ethlo.lamebda.function;

import com.fasterxml.jackson.databind.JsonNode;

public interface HttpRequest
{
    /**
     * Case insensitive
     * @param name
     * @return
     */
    String header(String name);
    
    /**
     * Case sensitive
     * @param name
     * @return
     */
    String param(String name);
    
    /**
     * Returns the request path
     * @return
     */
    String path();
    
    /**
     * Returns the raw body as a byte array
     * @return
     */
    byte[] rawBody();
    
    /**
     * Returns the body as a UTF-8 encoded string
     * @return
     */
    String body();
    
    /**
     * Returns the body as a JSON node
     * @return
     */
    JsonNode json();
    
    /**
     * Returns the remote host calling this function
     * @return
     */
    String remoteHost();
}