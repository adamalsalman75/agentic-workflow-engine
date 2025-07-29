package dev.alsalman.agenticworkflowengine.planning;

import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.planning.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlanReviewService {
    
    private static final Logger log = LoggerFactory.getLogger(PlanReviewService.class);
    
    private final TaskPlanAgent taskPlanAgent;
    private final WorkflowPersistenceService persistenceService;
    
    public PlanReviewService(TaskPlanAgent taskPlanAgent, WorkflowPersistenceService persistenceService) {
        this.taskPlanAgent = taskPlanAgent;
        this.persistenceService = persistenceService;
    }
    
    /**
     * Updates a task in the task list and persists it to the database.
     * 
     * @param allTasks The current list of all tasks
     * @param completedTask The task that was just completed
     * @param goalId The goal ID for persistence
     * @return Updated list of tasks with the completed task updated
     */
    public List<Task> updateTaskInList(List<Task> allTasks, Task completedTask, UUID goalId) {
        // Save completed task to database
        persistenceService.saveTask(completedTask, goalId);
        
        // Update task in the list
        for (int i = 0; i < allTasks.size(); i++) {
            if (allTasks.get(i).id().equals(completedTask.id())) {
                allTasks.set(i, completedTask);
                break;
            }
        }
        
        return allTasks;
    }
    
    /**
     * Handles plan review after a task completion, potentially updating the task plan
     * based on the results of the completed task.
     * 
     * @param allTasks The current list of all tasks
     * @param completedTask The task that was just completed
     * @param goalId The goal ID for persistence of new tasks
     * @return Updated list of tasks (may include new tasks from plan review)
     */
    public List<Task> handlePlanReview(List<Task> allTasks, Task completedTask, UUID goalId) {
        // Only review plan for completed tasks
        if (completedTask.status() != TaskStatus.COMPLETED) {
            return allTasks;
        }
        
        log.debug("Starting plan review after completing task: '{}'", completedTask.description());
        
        List<Task> updatedTasks = reviewPlanAfterTask(allTasks, completedTask);
        
        if (!updatedTasks.equals(allTasks)) {
            int originalCount = allTasks.size();
            int newCount = updatedTasks.size();
            int addedTasks = newCount - originalCount;
            
            log.info("📋 PLAN REVIEW UPDATED TASKS: {} original → {} new ({} tasks {})", 
                    originalCount, newCount, 
                    Math.abs(addedTasks), 
                    addedTasks > 0 ? "ADDED" : "REMOVED");
            
            // Update allTasks with persisted versions that have database IDs
            List<Task> persistedUpdatedTasks = new ArrayList<>();
            for (Task task : updatedTasks) {
                if (task.status() == TaskStatus.PENDING && task.id() == null) {
                    log.info("💡 NEW TASK FROM PLAN REVIEW: '{}'", task.description());
                    Task savedTask = persistenceService.saveTask(task, goalId);
                    persistedUpdatedTasks.add(savedTask);
                } else {
                    persistedUpdatedTasks.add(task);
                }
            }
            return persistedUpdatedTasks;
        } else {
            log.debug("Plan review completed - no changes needed");
            return allTasks;
        }
    }
    
    /**
     * Reviews the current plan after a task completion and determines if updates are needed.
     * 
     * @param currentTasks The current list of tasks
     * @param completedTask The task that was just completed
     * @return Updated task list (may be unchanged if no updates needed)
     */
    private List<Task> reviewPlanAfterTask(List<Task> currentTasks, Task completedTask) {
        try {
            return taskPlanAgent.reviewAndUpdatePlan(currentTasks, completedTask);
        } catch (Exception e) {
            log.warn("Failed to review plan after task completion: {}", e.getMessage());
            return currentTasks; // Return unchanged if review fails
        }
    }
}