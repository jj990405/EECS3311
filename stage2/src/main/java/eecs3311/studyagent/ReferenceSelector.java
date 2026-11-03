package eecs3311.studyagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Model-chosen references must match an uploaded, readable course file exactly. */
public final class ReferenceSelector {
    public List<MaterialFile> select(LlmClient llm, PromptBuilder prompts, ResponseParser parser,
                                     String request, List<MaterialFile> candidates) throws IOException {
        if (candidates.isEmpty()) return List.of();
        String names = candidates.stream().map(MaterialFile::originalName).toList().toString();
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.REFERENCE_SELECTION),
                prompts.user(PromptBuilder.Task.REFERENCE_SELECTION, "Available files: " + names, request)));
        List<MaterialFile> selected = new ArrayList<>();
        for (Object item : Json.array(response.get("fileNames"))) {
            if (!(item instanceof String name)) throw new IllegalArgumentException("Reference name must be a string");
            MaterialFile match = candidates.stream().filter(f -> f.originalName().equals(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("AI referenced an unavailable file: " + name));
            if (!selected.contains(match)) selected.add(match);
        }
        return selected;
    }
}
