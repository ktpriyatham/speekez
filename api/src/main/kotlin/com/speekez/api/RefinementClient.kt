package com.speekez.api

/**
 * Interface for AI text refinement services.
 */
interface RefinementClient {
    /**
     * Refines the given [text] using the specified [model] and [systemPrompt].
     *
     * @param text The raw transcription text to refine.
     * @param model The model identifier to use for refinement.
     * @param systemPrompt The system prompt to guide the refinement process.
     * @return The refined text.
     */
    suspend fun refine(text: String, model: String, systemPrompt: String): String

    /**
     * Validates the API key by sending a minimal request.
     *
     * @param model The model identifier to use for validation.
     * @return True if the key is valid, false if unauthorized.
     */
    suspend fun validateKey(model: String): Boolean
}
