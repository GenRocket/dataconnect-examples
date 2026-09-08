package com.genrocket.dataconnect;

import java.util.Map;

public record ReceiverSpec(String name) {
  public Map<String, Object> toMap() {
    return Map.of("name", name);
  }
}
