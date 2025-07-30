package dev.alsalman.agenticworkflowengine.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Configuration for JSONB support in Spring Data JDBC.
 * Provides converters for Map<String, Object> to/from JSONB columns.
 */
@Configuration
public class JsonbConfiguration extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    public JsonbConfiguration(@Autowired(required = false) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(List.of(
            new MapToJsonbConverter(),
            new JsonbToMapConverter()
        ));
    }

    /**
     * Converts Map<String, Object> to JSON string for JSONB storage
     */
    @WritingConverter
    public class MapToJsonbConverter implements Converter<Map<String, Object>, String> {
        @Override
        public String convert(Map<String, Object> source) {
            if (source == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert Map to JSON", e);
            }
        }
    }

    /**
     * Converts JSON string from JSONB to Map<String, Object>
     */
    @ReadingConverter
    public class JsonbToMapConverter implements Converter<String, Map<String, Object>> {
        @Override
        public Map<String, Object> convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            try {
                return objectMapper.readValue(source, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert JSON to Map", e);
            }
        }
    }
}