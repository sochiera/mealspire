package com.mealspire.backend;
import com.mealspire.app.domain.*;
import com.sun.net.httpserver.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;
/** Loopback only: put an isolated TLS proxy in front. Tokens exist only during a request. */
public final class Server {
    private static final int MAX_BODY=65536;
    private static final HttpClient CLIENT=HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
    public static void main(String[] args) throws IOException {
        String catalogFile=System.getenv("MEALSPIRE_CATALOG");
        final JSONObject catalog=catalogFile==null?Operations.catalog():
            new JSONObject(Files.readString(Path.of(catalogFile)));
        BackendWire.catalog(catalog); // refuse malformed releases before binding
        int port=Integer.parseInt(System.getenv().getOrDefault("MEALSPIRE_PORT","18081"));
        HttpServer server=create(port,catalog);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(2)));
        System.out.println("Mealspire API v1 listening on loopback port "+server.getAddress().getPort());
    }
    static HttpServer create(int port,JSONObject catalog) throws IOException {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),32);
        server.setExecutor(new ThreadPoolExecutor(4,8,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32),new ThreadPoolExecutor.CallerRunsPolicy()));
        server.createContext("/",exchange -> {
            try {
                String path=exchange.getRequestURI().getPath();
                if("GET".equals(exchange.getRequestMethod()) && path.equals("/health")) {
                    reply(exchange,200,new JSONObject().put("status","ok").put("api",1)); return;
                }
                if("GET".equals(exchange.getRequestMethod()) && path.equals("/v1/catalog")) {
                    reply(exchange,200,catalog); return;
                }
                if(!path.matches("/v1/(proposals|recipe|modify|import|taste)")) { reply(exchange,404,error()); return; }
                if(!"POST".equals(exchange.getRequestMethod())) { reply(exchange,405,error()); return; }
                String auth=exchange.getRequestHeaders().getFirst("Authorization");
                if(auth==null || !auth.startsWith("Bearer ") || auth.length()<12) { reply(exchange,401,error()); return; }
                byte[] bytes=exchange.getRequestBody().readNBytes(MAX_BODY+1);
                if(bytes.length>MAX_BODY) { reply(exchange,413,error()); return; }
                JSONObject input=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
                String model=input.getString("model");
                if(!model.matches("gpt-[0-9]+(\\.[0-9]+)?-(luna|sol)")) { reply(exchange,400,error()); return; }
                LlmClient llm=(system,user) -> complete(auth,model,system,user);
                reply(exchange,200,new Operations(llm).execute(path.substring(4),input));
            } catch(Unauthorized e) { reply(exchange,401,error()); }
            catch(JSONException | IllegalArgumentException e) { reply(exchange,400,error()); }
            catch(Exception e) { reply(exchange,502,error()); }
            finally { exchange.close(); }
        });
        return server;
    }
    private static String complete(String auth,String model,String system,String user) throws IOException {
        HttpRequest request=HttpRequest.newBuilder(URI.create(ResponsesApi.RESPONSES_URL))
            .timeout(Duration.ofSeconds(90)).header("Authorization",auth)
            .header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(ResponsesApi.buildRequest(model,system,user))).build();
        try {
            HttpResponse<InputStream> response=CLIENT.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(InputStream body=response.body()) {
                if(response.statusCode()==401) throw new Unauthorized();
                if(response.statusCode()/100!=2) throw new IOException("Upstream failure");
                byte[] bytes=body.readNBytes(2*1024*1024+1);
                if(bytes.length>2*1024*1024) throw new IOException("Upstream response too large");
                return ResponsesApi.parseStream(new String(bytes,StandardCharsets.UTF_8));
            }
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
    }
    private static class Unauthorized extends IOException {}
    private static JSONObject error() { return new JSONObject().put("error","Żądanie nie powiodło się."); }
    private static void reply(HttpExchange exchange,int status,JSONObject body) throws IOException {
        byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control","no-store");
        exchange.sendResponseHeaders(status,bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
