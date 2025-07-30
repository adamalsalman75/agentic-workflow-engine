package dev.alsalman.agenticworkflowengine.template.repository;

import dev.alsalman.agenticworkflowengine.template.domain.TemplateParameter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for template parameters
 */
@Repository
public interface TemplateParameterRepository extends CrudRepository<TemplateParameter, UUID> {
    
    /**
     * Find all parameters for a template, ordered by display order
     */
    List<TemplateParameter> findByTemplateIdOrderByDisplayOrder(UUID templateId);
    
    /**
     * Find a specific parameter by template and name
     */
    TemplateParameter findByTemplateIdAndName(UUID templateId, String name);
    
    /**
     * Delete all parameters for a template
     */
    void deleteByTemplateId(UUID templateId);
    
    /**
     * Check if a parameter exists for a template
     */
    boolean existsByTemplateIdAndName(UUID templateId, String name);
}