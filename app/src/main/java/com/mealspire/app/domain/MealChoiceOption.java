package com.mealspire.app.domain;

/**
 * One choice the user can make when asking for a meal idea (e.g. "vegetarian",
 * "quick"). Each option carries:
 * <ul>
 *   <li>{@code label} — what the user sees,</li>
 *   <li>{@code promptFragment} — added to the LLM prompt when selected,</li>
 *   <li>{@code learnTag} — recorded as a liked preference, so the app learns
 *       from every choice the user makes.</li>
 * </ul>
 */
public final class MealChoiceOption {

    private final String id;
    private final String label;
    private final String promptFragment;
    private final String learnTag;

    public MealChoiceOption(String id, String label, String promptFragment, String learnTag) {
        this.id = id;
        this.label = label;
        this.promptFragment = promptFragment;
        this.learnTag = learnTag;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getPromptFragment() {
        return promptFragment;
    }

    public String getLearnTag() {
        return learnTag;
    }
}
