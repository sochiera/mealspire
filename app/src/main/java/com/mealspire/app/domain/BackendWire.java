package com.mealspire.app.domain;
import org.json.*;
import java.util.*;
/** Kontrakt v1: dane domenowe, bez promptów i refresh tokenów. */
public final class BackendWire {
    public static JSONObject request(RecipeRequest r) throws JSONException {
        return new JSONObject().put("mealType", r.getMealType())
            .put("likes", new JSONArray(r.getPreferences().getLikes()))
            .put("recent", new JSONArray(r.getRecentToAvoid()))
            .put("choices", new JSONArray(r.getChoiceFragments()))
            .put("known", new JSONArray(r.getKnownDishes()))
            .put("household", new JSONObject(new HouseholdProfileSerializer().toJson(r.getHouseholdProfile())));
    }
    public static List<String> strings(JSONArray a) throws JSONException {
        List<String> result = new ArrayList<>();
        if (a != null) for (int i=0; i<a.length(); i++) result.add(a.getString(i));
        return result;
    }
    public static RecipeRequest request(JSONObject j) throws JSONException {
        return new RecipeRequest(j.getString("mealType"),
            new UserPreferences(strings(j.optJSONArray("likes")), Collections.emptyList()),
            strings(j.optJSONArray("recent")), strings(j.optJSONArray("choices")),
            strings(j.optJSONArray("known")), Collections.emptyList(),
            new HouseholdProfileSerializer().fromJson(j.getJSONObject("household").toString()));
    }
    public static JSONObject recipe(Recipe r) throws JSONException {
        return new JSONObject().put("title",r.getTitle()).put("details",r.getDetails());
    }
    public static Recipe recipe(JSONObject j) throws JSONException {
        return new Recipe(j.getString("title"),j.getString("details"));
    }
    public static JSONObject proposal(DishProposal p) throws JSONException {
        return new JSONObject().put("name",p.getName()).put("description",p.getDescription())
            .put("time",p.getTime()).put("ingredients",new JSONArray(p.getKeyIngredients()));
    }
    public static DishProposal proposal(JSONObject j) throws JSONException {
        return new DishProposal(j.getString("name"),j.getString("description"),
            j.getString("time"),strings(j.getJSONArray("ingredients")));
    }
    public static Recipe[][] catalog(JSONObject j) throws JSONException {
        JSONArray meals=j.getJSONArray("meals");
        if(meals.length()!=3) throw new JSONException("Nieprawidłowy katalog");
        Recipe[][] result=new Recipe[3][];
        for(int m=0;m<3;m++) {
            JSONArray rows=meals.getJSONArray(m);
            if(rows.length()==0 || rows.length()>500) throw new JSONException("Nieprawidłowy katalog");
            result[m]=new Recipe[rows.length()];
            for(int i=0;i<rows.length();i++) result[m][i]=recipe(rows.getJSONObject(i));
        }
        return result;
    }
}
