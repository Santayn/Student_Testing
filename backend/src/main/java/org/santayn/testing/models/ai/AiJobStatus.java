package org.santayn.testing.models.ai;

public final class AiJobStatus {

    public static final int QUEUED = 1;
    public static final int RUNNING = 2;
    public static final int DONE = 3;
    public static final int FAILED = 4;

    private AiJobStatus() {
    }

    public static String name(int status) {
        return switch (status) {
            case QUEUED -> "queued";
            case RUNNING -> "running";
            case DONE -> "done";
            case FAILED -> "failed";
            default -> "unknown";
        };
    }
}
