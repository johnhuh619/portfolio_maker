package io.resume.make.domain.projects.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.resume.make.domain.projects.entity.vo.TeamInfo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class TeamInfoConverter implements AttributeConverter<TeamInfo, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(TeamInfo attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert TeamInfo to JSON", e);
            return null;
        }
    }

    @Override
    public TeamInfo convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, TeamInfo.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to TeamInfo", e);
            return null;
        }
    }
}
