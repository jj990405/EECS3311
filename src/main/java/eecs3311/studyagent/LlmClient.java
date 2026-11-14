package eecs3311.studyagent;

import java.io.IOException;

/** Adapter boundary: tests can replace the network client. */
@FunctionalInterface
public interface LlmClient {
    String complete(String systemPrompt, String userPrompt) throws IOException;
    default boolean supportsImages() { return false; }
    default String completeWithImages(String systemPrompt, String userPrompt, java.util.List<String> dataUrls)
            throws IOException {
        throw new IOException("The configured AI client cannot read slide images.");
    }
}
