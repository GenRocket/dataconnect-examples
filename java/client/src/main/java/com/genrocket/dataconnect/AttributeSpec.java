package com.genrocket.dataconnect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AttributeSpec(
    String name,
    String dataType,
    String description,
    Integer size,
    List<String> listOfOptions,
    String regex,
    List<GeneratorSpec> generators) {

  public AttributeSpec {
    listOfOptions = listOfOptions == null ? List.of() : List.copyOf(listOfOptions);
    generators = generators == null ? List.of() : List.copyOf(generators);
  }

  public static AttributeSpec inferred(String name, String dataType, String description) {
    return new AttributeSpec(name, dataType, description, null, List.of(), null, List.of());
  }

  public static AttributeSpec generated(String name, String dataType, String generator) {
    return new AttributeSpec(
        name, dataType, null, null, List.of(), null, List.of(new GeneratorSpec(generator)));
  }

  public Map<String, Object> toMap() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("name", name);
    putIfNotNull(value, "dataType", dataType);
    putIfNotNull(value, "description", description);
    putIfNotNull(value, "size", size);
    putIfNotNull(value, "regex", regex);
    if (!listOfOptions.isEmpty()) {
      value.put("listOfOptions", listOfOptions);
    }
    if (!generators.isEmpty()) {
      List<Map<String, Object>> result = new ArrayList<>();
      generators.forEach(generator -> result.add(generator.toMap()));
      value.put("generator", result);
    }
    return value;
  }

  private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }
}
