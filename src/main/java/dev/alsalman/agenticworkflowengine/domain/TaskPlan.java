package dev.alsalman.agenticworkflowengine.domain;

import java.util.List;

public record TaskPlan(
    List<Task> tasks,
    List<TaskDependency> dependencies
) {
    public static TaskPlan of(List<Task> tasks, List<TaskDependency> dependencies) {
        return new TaskPlan(tasks, dependencies);
    }
    
    public static TaskPlan of(List<Task> tasks) {
        return new TaskPlan(tasks, List.of());
    }
}