package eecs3311.studyagent;

import java.io.IOException;

/** Adapter boundary: tests can replace the network client. */
@FunctionalInterface
public interface LlmClient {
    String complete(String systemPrompt, String userPrompt) throws IOException;
}
