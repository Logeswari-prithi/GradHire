package com.gradhire.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StringToListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isArray()) {
            List<String> list = new java.util.ArrayList<>();
            for (JsonNode element : node) {
                list.add(element.asText());
            }
            return list;
        } else if (node.isTextual()) {
            String text = node.asText();
            if (text == null || text.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
}
