package com.mealspire.app.domain;
import org.json.*;
import java.io.IOException;
import java.util.*;
/** Online orchestration belongs to the VPS; APK only encodes and displays results. */
public final class BackendRecipeService extends RecipeService {
    private final BackendApi api;
    private List<String> exploratory=Collections.emptyList();
    public List<String> exploratoryDishes() { return exploratory; }
    public BackendRecipeService(BackendApi api) { super(null,null,null); this.api=api; }
    @Override public List<DishProposal> proposeDishes(RecipeRequest r,int count) throws IOException {
        try {
            JSONObject response=api.call("proposals",BackendWire.request(r).put("count",count));
            exploratory=BackendWire.strings(response.optJSONArray("exploratory"));
            JSONArray rows=response.getJSONArray("proposals");
            List<DishProposal> result=new ArrayList<>();
            for(int i=0;i<rows.length();i++) result.add(BackendWire.proposal(rows.getJSONObject(i)));
            return result;
        } catch(JSONException e) { throw new IOException("Nieczytelne propozycje",e); }
    }
    @Override public DishProposal proposeDish(RecipeRequest r) throws IOException { return proposeDishes(r,1).get(0); }
    @Override public Recipe generateRecipe(RecipeRequest r) throws IOException { return recipe("recipe",r,""); }
    @Override public Recipe generateRecipeFor(String name,RecipeRequest r) throws IOException { return recipe("recipe",r,name); }
    private Recipe recipe(String op,RecipeRequest r,String name) throws IOException {
        try { return BackendWire.recipe(api.call(op,BackendWire.request(r).put("dish",name)).getJSONObject("recipe")); }
        catch(JSONException e) { throw new IOException("Nieczytelny przepis",e); }
    }
    @Override public Recipe modifyRecipe(Recipe r,String instruction,HouseholdProfile profile) throws IOException {
        try { return BackendWire.recipe(api.call("modify",new JSONObject().put("recipe",BackendWire.recipe(r))
            .put("instruction",instruction).put("household",new JSONObject(new HouseholdProfileSerializer()
                .toJson(profile==null?HouseholdProfile.empty():profile)))).getJSONObject("recipe")); }
        catch(JSONException e) { throw new IOException("Nieczytelny przepis",e); }
    }
}
