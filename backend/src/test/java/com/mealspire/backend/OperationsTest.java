package com.mealspire.backend;
import com.mealspire.app.domain.*;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class OperationsTest {
    private JSONObject request() {
        return BackendWire.request(new RecipeRequest("Obiad",UserPreferences.empty().withLike("Zupa"),
            Collections.emptyList(),Collections.emptyList()));
    }
    @Test public void assessesTasteOnServerBeforeGeneratingAndFiltersDiet() throws Exception {
        List<String> prompts=new ArrayList<>();
        Operations ops=new Operations((s,u) -> {
            prompts.add(s+u);
            return prompts.size()==1?"Lubi zupy":"Nazwa: Zupa pomidorowa\nOpis: Prosta zupa\nCzas: 20 minut\nSkładniki: pomidory, woda";
        });
        JSONObject response=ops.execute("proposals",request().put("count",1));
        assertEquals(2,prompts.size());
        assertTrue(prompts.get(0).contains("Zupa"));
        assertTrue(prompts.get(1).contains("Lubi zupy"));
        assertEquals(1,response.getJSONArray("proposals").length());
    }
    @Test public void rejectsDietBreakingProposalsAndUsesAllowedCatalog() throws Exception {
        Operations ops=new Operations((system,user) -> system.startsWith("Oceń")?"Lubi zupy":
            "Nazwa: Boczek\nOpis: Smażony boczek\nCzas: 10 minut\nSkładniki: boczek");
        HouseholdProfile profile=HouseholdProfile.empty().withDiet(DietConstraints.of(
            Collections.singleton(DietConstraints.Exclusion.VEGETARIAN)));
        JSONObject input=request().put("count",1).put("household",new JSONObject(new HouseholdProfileSerializer().toJson(profile)));
        JSONArray rows=ops.execute("proposals",input).getJSONArray("proposals");
        assertEquals(1,rows.length());
        DishProposal p=BackendWire.proposal(rows.getJSONObject(0));
        assertTrue(profile.getDiet().allows(p.getName()+p.summary()));
    }
    @Test public void recipeAndModificationUseBackendPrompts() throws Exception {
        List<String> prompts=new ArrayList<>();
        Operations ops=new Operations((system,user) -> { prompts.add(user); return "Zupa\nSkładniki: woda\nGotuj."; });
        assertEquals("Zupa",ops.execute("recipe",request().put("dish","Zupa"))
            .getJSONObject("recipe").getString("title"));
        JSONObject modify=new JSONObject().put("recipe",BackendWire.recipe(new Recipe("Zupa","Składniki: mleko")))
            .put("instruction","Zastąp mleko wodą").put("household",new JSONObject());
        ops.execute("modify",modify);
        assertTrue(prompts.get(1).contains("Zastąp mleko wodą"));
    }
    @Test public void emptyLikesDoNotSpendTasteCall() throws Exception {
        Operations ops=new Operations((s,u) -> { throw new AssertionError("no LLM"); });
        JSONObject r=request().put("likes",new JSONArray());
        assertEquals("Brak danych o guście.",ops.execute("taste",r).getString("assessment"));
    }
    @Test public void catalogMatchesClientSchema() { assertEquals(3,BackendWire.catalog(Operations.catalog()).length); }
    @Test public void importDoesNotFetchUserUrl() throws Exception {
        Operations ops=new Operations((s,u) -> "Nazwa: Zupa\nSkładniki: woda\nGotuj.");
        assertFalse(ops.execute("import",new JSONObject().put("input","http://127.0.0.1/admin"))
            .getJSONObject("recipe").getString("title").isEmpty());
    }
    @Test(expected=IllegalArgumentException.class) public void limitsProposalCount() throws Exception {
        new Operations((s,u)->"unused").execute("proposals",request().put("count",100));
    }
}
