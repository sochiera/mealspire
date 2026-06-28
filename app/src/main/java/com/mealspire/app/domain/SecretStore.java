package com.mealspire.app.domain;

/**
 * Remembers the unlocked Anthropic API key so the user only has to type the
 * password once. After the first successful unlock the key is persisted here and
 * reused on the next launch, skipping the password prompt entirely.
 *
 * <p>Same modest threat model as the encrypted-in-repo key: the value lives in
 * the app's private storage, which the OS sandbox keeps from other apps. It is
 * not extra-encrypted at rest — see the README's security note.
 */
public interface SecretStore {

    /** Returns the remembered API key, or an empty string if none is stored. */
    String loadApiKey();

    /** Whether a non-empty API key has been remembered. */
    boolean hasApiKey();

    /** Remembers {@code key} for next time. Null/blank values clear the store. */
    void saveApiKey(String key);

    /** Forgets the remembered key, putting the app back into offline mode. */
    void clear();
}
