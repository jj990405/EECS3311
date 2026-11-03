package eecs3311.studyagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pending suggestions are transient and never execute without explicit approval. */
public final class CommandInvoker {
    public record Proposal(UUID id, String description) { }
    private final Map<UUID, AgentCommand> commands = new LinkedHashMap<>();
    private final Map<UUID, Proposal> descriptions = new LinkedHashMap<>();

    public synchronized Proposal propose(String description, AgentCommand command) {
        Proposal proposal = new Proposal(UUID.randomUUID(), description);
        commands.put(proposal.id(), command);
        descriptions.put(proposal.id(), proposal);
        return proposal;
    }

    public synchronized List<Proposal> pending() { return new ArrayList<>(descriptions.values()); }

    public synchronized Proposal replace(UUID id, String description, AgentCommand command) {
        if (!commands.containsKey(id)) throw new IllegalArgumentException("Unknown proposal: " + id);
        Proposal revised = new Proposal(id, description);
        commands.put(id, command);
        descriptions.put(id, revised);
        return revised;
    }

    public synchronized void approve(UUID id) throws IOException {
        AgentCommand command = commands.get(id);
        if (command == null) throw new IllegalArgumentException("Unknown proposal: " + id);
        command.execute();
        commands.remove(id);
        descriptions.remove(id);
    }

    public synchronized void reject(UUID id) {
        if (commands.remove(id) == null) throw new IllegalArgumentException("Unknown proposal: " + id);
        descriptions.remove(id);
    }

    public synchronized void clear() {
        commands.clear();
        descriptions.clear();
    }
}
