package dev.alsalman.agenticworkflowengine.template.repository;

import dev.alsalman.agenticworkflowengine.template.domain.SimpleWorkflowTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SimpleTemplateRepository extends CrudRepository<SimpleWorkflowTemplate, UUID> {
    
    List<SimpleWorkflowTemplate> findByIsPublicTrue();
    
    List<SimpleWorkflowTemplate> findByCategory(String category);
    
    List<SimpleWorkflowTemplate> findByName(String name);
}