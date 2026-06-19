package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ProposalListParserTest {

    private final ProposalListParser parser = new ProposalListParser();

    @Test
    public void parsesSeveralLabelledProposalsSeparatedByDashes() {
        List<DishProposal> proposals = parser.parse(
                "Nazwa: Jajecznica\nOpis: Szybka jajecznica.\nCzas: 10 min\nSkładniki: jajka, masło\n"
                        + "---\n"
                        + "Nazwa: Owsianka\nOpis: Ciepła owsianka.\nCzas: 8 min\nSkładniki: płatki, mleko\n"
                        + "---\n"
                        + "Nazwa: Tost\nOpis: Tost z serem.\nCzas: 5 min\nSkładniki: chleb, ser");

        assertEquals(3, proposals.size());
        assertEquals("Jajecznica", proposals.get(0).getName());
        assertEquals("Owsianka", proposals.get(1).getName());
        assertEquals("Tost", proposals.get(2).getName());
        assertEquals("10 min", proposals.get(0).getTime());
    }

    @Test
    public void splitsByNameLabelEvenWithoutSeparators() {
        List<DishProposal> proposals = parser.parse(
                "Nazwa: Zupa pomidorowa\nCzas: 30 min\n"
                        + "Nazwa: Naleśniki\nCzas: 25 min");

        assertEquals(2, proposals.size());
        assertEquals("Zupa pomidorowa", proposals.get(0).getName());
        assertEquals("Naleśniki", proposals.get(1).getName());
    }

    @Test
    public void fallsBackToBlankLineBlocksWhenNoLabels() {
        List<DishProposal> proposals = parser.parse(
                "Spaghetti\nProste spaghetti.\n\nSałatka\nLekka sałatka.");

        assertEquals(2, proposals.size());
        assertEquals("Spaghetti", proposals.get(0).getName());
        assertEquals("Sałatka", proposals.get(1).getName());
    }

    @Test
    public void dropsDuplicatesByName() {
        List<DishProposal> proposals = parser.parse(
                "Nazwa: Omlet\nCzas: 10 min\n---\nNazwa: Omlet\nCzas: 12 min");

        assertEquals(1, proposals.size());
    }

    @Test
    public void emptyOrNullGivesEmptyList() {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("   \n  ").isEmpty());
    }
}
