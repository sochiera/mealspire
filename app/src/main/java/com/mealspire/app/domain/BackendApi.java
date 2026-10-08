package com.mealspire.app.domain;
import org.json.*;
import java.io.IOException;
/** Token dostępowy jest przekazywany tylko dla jednego żądania; odświeżanie na telefonie. */
public final class BackendApi {
    private final ChatGptAccount account;
    private final HttpTransport transport;
    private final String base;
    public BackendApi(ChatGptAccount account, HttpTransport transport, String base) {
        if(!base.startsWith("https://")) throw new IllegalArgumentException("Backend wymaga HTTPS");
        this.account=account; this.transport=transport; this.base=base;
    }
    public JSONObject call(String operation, JSONObject data) throws IOException {
        try {
            HttpTransport.Response response=send(operation,data,false);
            if(response.status==401) response=send(operation,data,true);
            if(!response.isSuccess()) throw new IOException("Backend niedostępny (HTTP "+response.status+"). Spróbuj ponownie.");
            return new JSONObject(response.body);
        } catch(JSONException e) { throw new IOException("Nieczytelna odpowiedź backendu",e); }
    }
    private HttpTransport.Response send(String op, JSONObject data, boolean refresh) throws IOException, JSONException {
        ChatGptSession session=account.activeSession(refresh);
        String model=session.modelFor(account.modelChoice());
        if(model.isEmpty()) throw new IOException("Wybrany model jest niedostępny na koncie ChatGPT.");
        data.put("model",model);
        return transport.postJson(base+"/v1/"+op,session.accessToken,data.toString());
    }
}
