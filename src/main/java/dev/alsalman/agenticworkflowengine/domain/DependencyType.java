package dev.alsalman.agenticworkflowengine.domain;

public enum DependencyType {
    /**
     * Task cannot start until the dependency is completed
     */
    BLOCKING,
    
    /**
     * Task can start independently but would benefit from dependency results
     */
    INFORMATIONAL
}