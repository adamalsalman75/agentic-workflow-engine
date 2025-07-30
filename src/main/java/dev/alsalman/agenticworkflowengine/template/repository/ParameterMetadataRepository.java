package dev.alsalman.agenticworkflowengine.template.repository;

import dev.alsalman.agenticworkflowengine.template.domain.ParameterMetadata;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for parameter metadata
 */
@Repository
public interface ParameterMetadataRepository extends CrudRepository<ParameterMetadata, UUID> {
    
    /**
     * Find metadata by parameter ID
     */
    Optional<ParameterMetadata> findByParameterId(UUID parameterId);
    
    /**
     * Delete metadata by parameter ID
     */
    void deleteByParameterId(UUID parameterId);
    
    /**
     * Check if metadata exists for a parameter
     */
    boolean existsByParameterId(UUID parameterId);
}