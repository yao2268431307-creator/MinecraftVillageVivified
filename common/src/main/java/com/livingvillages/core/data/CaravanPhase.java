package com.livingvillages.core.data;

/** Stages in the caravan state machine. See detailed_design.md §9.3. */
public enum CaravanPhase {
    /** Waiting at origin cluster for departure. */
    IDLE,
    /** Loading cargo at origin warehouse. */
    LOADING,
    /** Moving along the rail path. */
    MOVING,
    /** Path blocked, waiting for recovery. */
    STUCK,
    /** Unloading cargo at destination warehouse. */
    UNLOADING,
    /** Returning to origin cluster. */
    RETURNING
}
