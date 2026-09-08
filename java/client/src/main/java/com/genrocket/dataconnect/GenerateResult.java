package com.genrocket.dataconnect;

import com.google.gson.JsonElement;

public record GenerateResult(JsonElement jsonValue, byte[] content, String contentType) {
  public static GenerateResult json(JsonElement value, String contentType) {
    return new GenerateResult(value, null, contentType);
  }

  public static GenerateResult file(byte[] content, String contentType) {
    return new GenerateResult(null, content, contentType);
  }
}
