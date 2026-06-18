package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProposalParserTest {

    private final ProposalParser parser = new ProposalParser();

    @Test
    public void parsesAllFourLabelledFields() {
        DishProposal proposal = parser.parse(
                "Nazwa: Placki ziemniaczane\n"
                        + "Opis: Chrupiące placki, klasyka obiadu.\n"
                        + "Czas: ok. 30 min\n"
                        + "Składniki: ziemniaki, jajko, mąka, cebula");

        assertEquals("Placki ziemniaczane", proposal.getName());
        assertEquals("Chrupiące placki, klasyka obiadu.", proposal.getDescription());
        assertEquals("ok. 30 min", proposal.getTime());
        assertEquals(4, proposal.getKeyIngredients().size());
        assertEquals("ziemniaki", proposal.getKeyIngredients().get(0));
        assertEquals("cebula", proposal.getKeyIngredients().get(3));
    }

    @Test
    public void stripsMarkdownAndBulletsAroundFields() {
        DishProposal proposal = parser.parse(
                "**Nazwa:** Omlet\n"
                        + "- Opis: Szybki omlet.\n"
                        + "* Czas: 10 min\n"
                        + "Składniki: **jajka**, ser");

        assertEquals("Omlet", proposal.getName());
        assertEquals("Szybki omlet.", proposal.getDescription());
        assertEquals("10 min", proposal.getTime());
        assertEquals("jajka", proposal.getKeyIngredients().get(0));
    }

    @Test
    public void fallsBackToFirstLineAsNameWhenNoLabel() {
        DishProposal proposal = parser.parse("Spaghetti carbonara\nbez etykiet");
        assertEquals("Spaghetti carbonara", proposal.getName());
    }

    @Test
    public void toleratesMissingFields() {
        DishProposal proposal = parser.parse("Nazwa: Sałatka\nCzas: 5 min");
        assertEquals("Sałatka", proposal.getName());
        assertEquals("", proposal.getDescription());
        assertEquals("5 min", proposal.getTime());
        assertTrue(proposal.getKeyIngredients().isEmpty());
    }

    @Test
    public void nullAnswerGivesEmptyProposal() {
        assertTrue(parser.parse(null).isEmpty());
    }

    @Test
    public void handlesSemicolonSeparatedIngredients() {
        DishProposal proposal = parser.parse(
                "Nazwa: Zupa\nSkładniki: marchew; pietruszka; seler");
        assertEquals(3, proposal.getKeyIngredients().size());
        assertEquals("pietruszka", proposal.getKeyIngredients().get(1));
    }
}
