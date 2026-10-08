package com.mealspire.app.domain;
import org.json.*;
import java.io.IOException;
public final class BackendDishImporter extends KnownDishImporter {
    private final BackendApi api;
    public BackendDishImporter(BackendApi api) { super(null,null,null,null); this.api=api; }
    @Override public CookbookEntry importDish(String input) throws IOException {
        try {
            Recipe r=BackendWire.recipe(api.call("import",new JSONObject().put("input",input)).getJSONObject("recipe"));
            return new CookbookEntry(r.getTitle(),r.getDetails(),KnownDishInput.isUrl(input)?input:"opis");
        } catch(JSONException e) { throw new IOException("Nieczytelne danie",e); }
    }
}
