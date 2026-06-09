package me.rspaae.grandseas.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents a single auditable event that occurred on an island.
 * Stored in-memory per island (max 50 entries), resets on server restart.
 */
public class AuditLog {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public enum Action {
        BUILD,
        BREAK,
        VISIT,
        MEMBER_JOIN,
        MEMBER_LEAVE,
        COOP_ADD,
        COOP_EXPIRE,
        COOP_REMOVE,
        UPGRADE
    }

    private final long timestamp;
    private final UUID actor;
    private final String actorName;
    private final Action action;
    private final String detail;

    public AuditLog(UUID actor, String actorName, Action action, String detail) {
        this.timestamp = System.currentTimeMillis();
        this.actor = actor;
        this.actorName = actorName;
        this.action = action;
        this.detail = detail;
    }

    public long getTimestamp() { return timestamp; }
    public UUID getActor() { return actor; }
    public String getActorName() { return actorName; }
    public Action getAction() { return action; }
    public String getDetail() { return detail; }

    public String getFormattedTime() {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + actorName + " » " + action.name() + (detail.isEmpty() ? "" : ": " + detail);
    }
}
