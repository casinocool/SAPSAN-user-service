package org.example.sapsanuserservice.util;

import jakarta.ws.rs.core.Response;

public class KeycloakUtil {
    public static String getCreatedId(Response response){
        String location = response.getHeaderString("Location");
        if(location==null){
            return null;
        }
        return location.substring(location.lastIndexOf("/")+1);
    }
}
