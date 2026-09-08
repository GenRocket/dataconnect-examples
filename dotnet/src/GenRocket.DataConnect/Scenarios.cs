namespace GenRocket.DataConnect;

public static class Scenarios
{
    public static GenerateRequest Explicit(int count = 3) => new(
        "Customer",
        count,
        [
            new("customerId", "String", Generators: [new("UUIDGen")]),
            new("email", "String", Generators: [new("EmailGen")]),
        ],
        Industry: "Financial Services");

    public static GenerateRequest Inferred(int count = 3) => new(
        "Customer",
        count,
        [
            new("fullName", "String", "A realistic full name for an adult customer"),
            new("email", "String", "An email address derived from a person's name"),
        ]);

    public static GenerateRequest Enumerated(int count = 3) => new(
        "Order",
        count,
        [new("status", "String", ListOfOptions: ["PENDING", "PAID", "SHIPPED"])]);

    public static GenerateRequest Regex(int count = 3) => new(
        "Account",
        count,
        [new("accountCode", "String", Regex: "ACCT-[0-9]{6}")]);

    public static GenerateRequest Mixed(int count = 3) => new(
        "Patient",
        count,
        [
            new("patientId", "String", Generators: [new("UUIDGen")]),
            new("fullName", "String", "An adult patient's full name"),
            new("status", "String", ListOfOptions: ["ACTIVE", "INACTIVE"]),
            new("medicalRecordNumber", "String", Regex: "MRN-[0-9]{8}"),
        ],
        "Healthcare",
        "Generate internally consistent outpatient registration data");

    public static GenerateRequest File(string receiver, int count = 3)
    {
        var request = Explicit(count);
        return request with { Receivers = [new(receiver)] };
    }
}
