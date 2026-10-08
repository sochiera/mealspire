package com.mealspire.backend;
import com.sun.net.httpserver.HttpServer;
import org.json.*;
import org.junit.Test;
import java.net.*;
import java.net.http.*;
import static org.junit.Assert.*;
public class ServerTest {
    @Test public void httpBoundaryRejectsInvalidRequestsAndServesCatalog() throws Exception {
        HttpServer server=Server.create(0,Operations.catalog()); server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();
            HttpClient c=HttpClient.newHttpClient();
            assertEquals(200,c.send(HttpRequest.newBuilder(URI.create(base+"/health")).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            String catalog=c.send(HttpRequest.newBuilder(URI.create(base+"/v1/catalog")).build(),HttpResponse.BodyHandlers.ofString()).body();
            assertEquals(3,new JSONObject(catalog).getJSONArray("meals").length());
            assertEquals(404,post(c,base,"/v1/unknown","{}",true));
            assertEquals(401,post(c,base,"/v1/taste","{}",false));
            assertEquals(400,post(c,base,"/v1/taste","broken",true));
            assertEquals(413,post(c,base,"/v1/taste","x".repeat(65537),true));
            assertEquals(400,post(c,base,"/v1/taste","{\"model\":\"arbitrary\"}",true));
        } finally { server.stop(0); ((java.util.concurrent.ExecutorService)server.getExecutor()).shutdownNow(); }
    }
    private int post(HttpClient c,String base,String path,String body,boolean auth) throws Exception {
        HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(base+path)).POST(HttpRequest.BodyPublishers.ofString(body));
        if(auth) b.header("Authorization","Bearer test-token");
        return c.send(b.build(),HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
