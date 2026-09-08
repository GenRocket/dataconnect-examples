package com.genrocket.dataconnect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GenerateRequest(
    String domain,
    int loopCount,
    List<AttributeSpec> attributes,
    String industry,
    String context,
    List<ReceiverSpec> receivers) {

  public GenerateRequest {
    attributes = attributes == null ? List.of() : List.copyOf(attributes);
    receivers = receivers == null ? List.of() : List.copyOf(receivers);
    if (domain == null || domain.isBlank()) {
      throw new IllegalArgumentException("domain must not be blank");
    }
    if (loopCount < 1 || loopCount > 10_000) {
      throw new IllegalArgumentException("loopCount must be between 1 and 10000");
    }
    if (attributes.isEmpty()) {
      throw new IllegalArgumentException("at least one attribute is required");
    }
    if (receivers.size() > 1) {
      throw new IllegalArgumentException("DataConnect accepts at most one receiver");
    }
  }

  public Map<String, Object> toMap(boolean includeRestOnly) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("domain", domain);
    value.put("loopCount", loopCount);
    List<Map<String, Object>> mappedAttributes = new ArrayList<>();
    attributes.forEach(attribute -> mappedAttributes.add(attribute.toMap()));
    value.put("attributes", mappedAttributes);
    if (industry != null && !industry.isBlank()) {
      value.put("industry", industry);
    }
    if (includeRestOnly && context != null && !context.isBlank()) {
      value.put("context", context);
    }
    if (includeRestOnly && !receivers.isEmpty()) {
      value.put("receivers", receivers.stream().map(ReceiverSpec::toMap).toList());
    }
    return value;
  }
}
