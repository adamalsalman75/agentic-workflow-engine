package dev.alsalman.agenticworkflowengine.planning;

import dev.alsalman.agenticworkflowengine.planning.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.planning.domain.TaskPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for creating task plans.
 * Coordinates with TaskPlanAgent to generate task plans with dependencies.
 */
@Service
public class TaskPlanService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskPlanService.class);
    
    private final TaskPlanAgent taskPlanAgent;
    
    public TaskPlanService(TaskPlanAgent taskPlanAgent) {
        this.taskPlanAgent = taskPlanAgent;
    }
    
    /**
     * Creates a task plan with dependencies for the given user query.
     * 
     * @param userQuery The user's query/request
     * @return TaskPlan containing tasks and their dependencies
     */
    public TaskPlan createTaskPlan(String userQuery) {
        log.info("Creating task plan for query: '{}'", userQuery);
        return taskPlanAgent.createTaskPlanWithDependencies(userQuery);
    }
}