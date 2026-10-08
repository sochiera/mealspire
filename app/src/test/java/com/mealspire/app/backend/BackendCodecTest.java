package com.mealspire.app.backend;
import com.mealspire.app.domain.*;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class BackendCodecTest {
    @Test public void roundTripPreservesConstraintsAndPreferences() throws Exception {
        RecipeRequest request = new RecipeRequest("Obiad", new UserPreferences(Arrays.asList("Ryż"), Arrays.asList("Zupa")),
                Arrays.asList("Makaron"), Arrays.asList("Dla 2 osób"), Arrays.asList("Curry"), Arrays.asList("ryż"),
                HouseholdProfile.empty().withDiet(DietConstraints.of(Arrays.asList(DietConstraints.Exclusion.VEGETARIAN))))
                .withTasteContext(new TasteContext(Arrays.asList("Warzywa"), Arrays.asList("Curry")).withExploration("Eksploruj"));
        JSONObject data = BackendCodec.request(request); data.put("futureField", "ignored");
        RecipeRequest copy = BackendCodec.request(data);
        assertEquals(request.getPreferences().getLikes(), copy.getPreferences().getLikes());
        assertEquals(request.getRecentToAvoid(), copy.getRecentToAvoid());
        assertEquals(request.getHouseholdProfile().getDiet().getExclusions(), copy.getHouseholdProfile().getDiet().getExclusions());
        assertEquals("Eksploruj", copy.getTasteContext().getExplorationSentence());
    }
    @Test public void unknownApiFailsInsteadOfMisreading() throws Exception {
        try { BackendCodec.response("{\"apiVersion\":2}"); fail(); }
        catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("APK")); }
    }
}
