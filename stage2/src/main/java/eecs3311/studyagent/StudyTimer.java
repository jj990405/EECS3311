package eecs3311.studyagent;

import java.time.LocalDateTime;
import java.util.UUID;

/** One active timer; pause time is excluded from the recorded duration. */
public final class StudyTimer {
    private UUID courseId;
    private String topic;
    private LocalDateTime startedAt;
    private long startedNanos;
    private long accumulatedNanos;
    private boolean running;
    private boolean active;
    private long targetSeconds;

    public synchronized void start(UUID courseId, String topic) {
        start(courseId, topic, 25 * 60);
    }

    public synchronized void start(UUID courseId, String topic, long targetSeconds) {
        if (active) throw new IllegalStateException("Stop the active timer first.");
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("Enter a study topic.");
        if (targetSeconds < 1) throw new IllegalArgumentException("Timer duration must be positive.");
        this.courseId = courseId;
        this.topic = topic.trim();
        this.startedAt = LocalDateTime.now();
        this.startedNanos = System.nanoTime();
        this.running = true;
        this.active = true;
        this.targetSeconds = targetSeconds;
    }

    public synchronized void pause() {
        if (!running) throw new IllegalStateException("No running timer.");
        accumulatedNanos += System.nanoTime() - startedNanos;
        running = false;
    }

    public synchronized void resume() {
        if (!active || running) throw new IllegalStateException("No paused timer.");
        startedNanos = System.nanoTime();
        running = true;
    }

    public synchronized StudySession stop() {
        if (!active) throw new IllegalStateException("No active timer.");
        long nanos = accumulatedNanos + (running ? System.nanoTime() - startedNanos : 0);
        StudySession result = new StudySession(UUID.randomUUID(), courseId, topic, startedAt, nanos / 1_000_000_000L);
        courseId = null;
        active = false;
        accumulatedNanos = 0;
        running = false;
        return result;
    }

    public synchronized boolean active() { return active; }
    public synchronized long remainingSeconds() {
        if (!active) return 0;
        long nanos = accumulatedNanos + (running ? System.nanoTime() - startedNanos : 0);
        return Math.max(0, targetSeconds - nanos / 1_000_000_000L);
    }
}
