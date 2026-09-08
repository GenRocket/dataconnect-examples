package com.genrocket.dataconnect;

import java.util.List;

public final class Scenarios {
  private Scenarios() {}

  public static GenerateRequest explicit(int count) {
    return new GenerateRequest(
        "Customer",
        count,
        List.of(
            AttributeSpec.generated("customerId", "String", "UUIDGen"),
            AttributeSpec.generated("email", "String", "EmailGen")),
        "Financial",
        null,
        List.of());
  }

  public static GenerateRequest inferred(int count) {
    return new GenerateRequest(
        "Customer",
        count,
        List.of(
            AttributeSpec.inferred(
                "fullName", "String", "A realistic full name for an adult customer"),
            AttributeSpec.inferred(
                "email", "String", "An email address derived from a person's name")),
        null,
        null,
        List.of());
  }

  public static GenerateRequest enumerated(int count) {
    return new GenerateRequest(
        "Order",
        count,
        List.of(
            new AttributeSpec(
                "status",
                "String",
                null,
                null,
                List.of("PENDING", "PAID", "SHIPPED"),
                null,
                List.of())),
        null,
        null,
        List.of());
  }

  public static GenerateRequest regex(int count) {
    return new GenerateRequest(
        "Account",
        count,
        List.of(
            new AttributeSpec(
                "accountCode", "String", null, null, List.of(), "ACCT-[0-9]{6}", List.of())),
        null,
        null,
        List.of());
  }

  public static GenerateRequest mixed(int count) {
    return new GenerateRequest(
        "Patient",
        count,
        List.of(
            AttributeSpec.generated("patientId", "String", "UUIDGen"),
            AttributeSpec.inferred("fullName", "String", "An adult patient's full name"),
            new AttributeSpec(
                "status", "String", null, null, List.of("ACTIVE", "INACTIVE"), null, List.of()),
            new AttributeSpec(
                "medicalRecordNumber", "String", null, null, List.of(), "MRN-[0-9]{8}", List.of())),
        "Healthcare",
        "Generate internally consistent outpatient registration data",
        List.of());
  }

  public static GenerateRequest file(String receiver, int count) {
    GenerateRequest base = explicit(count);
    return new GenerateRequest(
        base.domain(),
        base.loopCount(),
        base.attributes(),
        base.industry(),
        null,
        List.of(new ReceiverSpec(receiver)));
  }
}
