package dev.alsalman.agenticworkflowengine.template.repository;

import dev.alsalman.agenticworkflowengine.template.domain.ParameterValidationRule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for parameter validation rules
 */
@Repository
public interface ParameterValidationRuleRepository extends CrudRepository<ParameterValidationRule, UUID> {
    
    /**
     * Find all validation rules for a parameter
     */
    List<ParameterValidationRule> findByParameterId(UUID parameterId);
    
    /**
     * Delete all validation rules for a parameter
     */
    void deleteByParameterId(UUID parameterId);
    
    /**
     * Count validation rules for a parameter
     */
    long countByParameterId(UUID parameterId);
}