package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.domain.TemplateExecution;
import dev.alsalman.agenticworkflowengine.domain.TemplateParameter;
import dev.alsalman.agenticworkflowengine.domain.WorkflowTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class WorkflowTemplatePersistenceService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplatePersistenceService.class);
    
    private final JdbcClient jdbcClient;
    
    public WorkflowTemplatePersistenceService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
    
    public WorkflowTemplate saveTemplate(WorkflowTemplate template) {
        // Save or update template
        jdbcClient.sql("""
            INSERT INTO workflow_templates (
                id, name, description, category, prompt_template, 
                metadata, tags, author, version, is_public, 
                usage_count, rating, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                category = EXCLUDED.category,
                prompt_template = EXCLUDED.prompt_template,
                metadata = EXCLUDED.metadata,
                tags = EXCLUDED.tags,
                version = EXCLUDED.version,
                updated_at = EXCLUDED.updated_at
            """)
            .params(
                template.id(),
                template.name(),
                template.description(),
                template.category(),
                template.promptTemplate(),
                toJson(template.metadata()),
                template.tags().toArray(new String[0]),
                template.author(),
                template.version(),
                template.isPublic(),
                template.usageCount(),
                template.rating(),
                template.createdAt(),
                template.updatedAt()
            )
            .update();
        
        // Save parameters
        saveParameters(template.id(), template.parameters());
        
        return template;
    }
    
    private void saveParameters(UUID templateId, List<TemplateParameter> parameters) {
        // Delete existing parameters
        jdbcClient.sql("DELETE FROM template_parameters WHERE template_id = ?")
            .param(templateId)
            .update();
        
        // Insert new parameters
        for (TemplateParameter param : parameters) {
            jdbcClient.sql("""
                INSERT INTO template_parameters (
                    template_id, name, description, type, required,
                    default_value, allowed_values, validation, placeholder, order_index
                ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
                """)
                .params(
                    templateId,
                    param.name(),
                    param.description(),
                    param.type().name(),
                    param.required(),
                    toJson(param.defaultValue()),
                    toJson(param.allowedValues()),
                    toJson(param.validation()),
                    param.placeholder(),
                    param.orderIndex()
                )
                .update();
        }
    }
    
    public Optional<WorkflowTemplate> findTemplateById(UUID templateId) {
        List<WorkflowTemplate> templates = jdbcClient.sql("""
            SELECT * FROM workflow_templates WHERE id = ?
            """)
            .param(templateId)
            .query((rs, rowNum) -> mapTemplate(rs))
            .list();
        
        if (templates.isEmpty()) {
            return Optional.empty();
        }
        
        WorkflowTemplate template = templates.get(0);
        List<TemplateParameter> parameters = loadParameters(templateId);
        
        return Optional.of(new WorkflowTemplate(
            template.id(), template.name(), template.description(), template.category(),
            parameters, template.promptTemplate(), template.metadata(), template.tags(),
            template.author(), template.version(), template.isPublic(), template.usageCount(),
            template.rating(), template.createdAt(), template.updatedAt()
        ));
    }
    
    private List<TemplateParameter> loadParameters(UUID templateId) {
        return jdbcClient.sql("""
            SELECT * FROM template_parameters 
            WHERE template_id = ? 
            ORDER BY order_index
            """)
            .param(templateId)
            .query((rs, rowNum) -> new TemplateParameter(
                rs.getString("name"),
                rs.getString("description"),
                ParameterType.valueOf(rs.getString("type")),
                rs.getBoolean("required"),
                fromJson(rs.getString("default_value")),
                fromJsonList(rs.getString("allowed_values")),
                fromJsonMap(rs.getString("validation")),
                rs.getString("placeholder"),
                rs.getInt("order_index")
            ))
            .list();
    }
    
    public List<WorkflowTemplate> searchTemplates(String category, List<String> tags, String searchText) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM workflow_templates WHERE is_public = true
            """);
        
        List<Object> params = new ArrayList<>();
        
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        
        if (tags != null && !tags.isEmpty()) {
            sql.append(" AND tags && ?::text[]");
            params.add(tags.toArray(new String[0]));
        }
        
        if (searchText != null && !searchText.isBlank()) {
            sql.append(" AND (name ILIKE ? OR description ILIKE ?)");
            String searchPattern = "%" + searchText + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        sql.append(" ORDER BY usage_count DESC, created_at DESC");
        
        return jdbcClient.sql(sql.toString())
            .params(params.toArray())
            .query((rs, rowNum) -> mapTemplate(rs))
            .list()
            .stream()
            .map(template -> {
                List<TemplateParameter> parameters = loadParameters(template.id());
                return new WorkflowTemplate(
                    template.id(), template.name(), template.description(), template.category(),
                    parameters, template.promptTemplate(), template.metadata(), template.tags(),
                    template.author(), template.version(), template.isPublic(), template.usageCount(),
                    template.rating(), template.createdAt(), template.updatedAt()
                );
            })
            .toList();
    }
    
    public List<String> findAllCategories() {
        return jdbcClient.sql("""
            SELECT DISTINCT category FROM workflow_templates 
            WHERE category IS NOT NULL AND is_public = true
            ORDER BY category
            """)
            .query(String.class)
            .list();
    }
    
    public void incrementUsageCount(UUID templateId) {
        jdbcClient.sql("""
            UPDATE workflow_templates 
            SET usage_count = usage_count + 1, updated_at = NOW()
            WHERE id = ?
            """)
            .param(templateId)
            .update();
    }
    
    public void saveExecution(TemplateExecution execution) {
        jdbcClient.sql("""
            INSERT INTO template_executions (
                id, template_id, goal_id, parameters, user_id, executed_at
            ) VALUES (?, ?, ?, ?::jsonb, ?, ?)
            """)
            .params(
                execution.id(),
                execution.templateId(),
                execution.goalId(),
                toJson(execution.parameters()),
                execution.userId(),
                execution.executedAt()
            )
            .update();
    }
    
    private WorkflowTemplate mapTemplate(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new WorkflowTemplate(
            UUID.fromString(rs.getString("id")),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("category"),
            List.of(), // Parameters loaded separately
            rs.getString("prompt_template"),
            fromJsonMap(rs.getString("metadata")),
            Arrays.asList((String[]) rs.getArray("tags").getArray()),
            rs.getString("author"),
            rs.getInt("version"),
            rs.getBoolean("is_public"),
            rs.getInt("usage_count"),
            rs.getObject("rating", Double.class),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
        );
    }
    
    private String toJson(Object obj) {
        if (obj == null) return null;
        // In production, use proper JSON library
        if (obj instanceof Map || obj instanceof List) {
            return obj.toString().replace("=", ":\"").replace("}", "\"}").replace(", ", "\", \"");
        }
        return "\"" + obj.toString() + "\"";
    }
    
    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        // In production, use proper JSON library
        return json.replace("\"", "");
    }
    
    @SuppressWarnings("unchecked")
    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        // In production, use proper JSON library
        return List.of(json.replace("[", "").replace("]", "").replace("\"", "").split(","));
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        // In production, use proper JSON library
        return Map.of();
    }
}