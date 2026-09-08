package com.genrocket.dataconnect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class GenerateRequestTest {
  @Test
  void mcpPayloadOmitsRestOnlyFields() {
    var value = Scenarios.mixed(3).toMap(false);

    assertFalse(value.containsKey("context"));
    assertFalse(value.containsKey("receivers"));
  }

  @Test
  void rejectsMoreThanOneReceiver() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GenerateRequest(
                "Customer",
                3,
                List.of(AttributeSpec.inferred("name", "String", "Name")),
                null,
                null,
                List.of(new ReceiverSpec("A"), new ReceiverSpec("B"))));
  }
}
