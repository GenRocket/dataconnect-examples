package com.genrocket.dataconnect;

import java.util.Map;

public record ReceiverSpec(String name) {
  public Map<String, Object> toMap() {
    return Map.of("receiverName", name, "parameters", Map.of("fileName", defaultFileName(name)));
  }

  private static String defaultFileName(String receiverName) {
    return switch (receiverName) {
      case "DelimitedFileReceiver" -> "dataconnect-output.csv";
      case "JSONFileReceiver" -> "dataconnect-output.json";
      case "XMLFileReceiver" -> "dataconnect-output.xml";
      default -> "dataconnect-output.data";
    };
  }
}
