using System.Text.Json.Nodes;

namespace GenRocket.DataConnect;

public sealed record GeneratorSpec(string Name)
{
    internal JsonObject ToJson() => new() { ["name"] = Name };
}

public sealed record AttributeSpec(
    string Name,
    string? DataType = null,
    string? Description = null,
    int? Size = null,
    IReadOnlyList<string>? ListOfOptions = null,
    string? Regex = null,
    IReadOnlyList<GeneratorSpec>? Generators = null)
{
    internal JsonObject ToJson()
    {
        var value = new JsonObject { ["name"] = Name };
        Add(value, "dataType", DataType);
        Add(value, "description", Description);
        Add(value, "size", Size);
        Add(value, "regex", Regex);
        if (ListOfOptions is { Count: > 0 })
        {
            value["listOfOptions"] = new JsonArray(
                ListOfOptions.Select(option => (JsonNode?)JsonValue.Create(option)).ToArray());
        }

        if (Generators is { Count: > 0 })
        {
            value["generator"] = new JsonArray(Generators.Select(generator => generator.ToJson()).ToArray());
        }

        return value;
    }

    private static void Add<T>(JsonObject target, string name, T? value)
    {
        if (value is not null)
        {
            target[name] = JsonValue.Create(value);
        }
    }
}

public sealed record ReceiverSpec(string Name)
{
    internal JsonObject ToJson()
    {
        var fileName = Name switch
        {
            "DelimitedFileReceiver" => "dataconnect-output.csv",
            "JSONFileReceiver" => "dataconnect-output.json",
            "XMLFileReceiver" => "dataconnect-output.xml",
            _ => "dataconnect-output.data",
        };
        return new JsonObject
        {
            ["receiverName"] = Name,
            ["parameters"] = new JsonObject { ["fileName"] = fileName },
        };
    }
}

public sealed record GenerateRequest(
    string Domain,
    int LoopCount,
    IReadOnlyList<AttributeSpec> Attributes,
    string? Industry = null,
    string? Context = null,
    IReadOnlyList<ReceiverSpec>? Receivers = null)
{
    public JsonObject ToJson(bool includeRestOnly = true)
    {
        Validate();
        var value = new JsonObject
        {
            ["domain"] = Domain,
            ["loopCount"] = LoopCount,
            ["attributes"] = new JsonArray(Attributes.Select(attribute => attribute.ToJson()).ToArray()),
        };
        if (!string.IsNullOrWhiteSpace(Industry))
        {
            value["industry"] = Industry;
        }

        if (includeRestOnly && !string.IsNullOrWhiteSpace(Context))
        {
            value["context"] = Context;
        }

        if (includeRestOnly && Receivers is { Count: > 0 })
        {
            value["receivers"] = new JsonArray(Receivers.Select(receiver => receiver.ToJson()).ToArray());
        }

        return value;
    }

    public Dictionary<string, object?> ToDictionary(bool includeRestOnly = true)
    {
        return System.Text.Json.JsonSerializer.Deserialize<Dictionary<string, object?>>(
                   ToJson(includeRestOnly))
               ?? throw new InvalidOperationException("Unable to build generation payload");
    }

    public void Validate()
    {
        if (string.IsNullOrWhiteSpace(Domain))
        {
            throw new ArgumentException("domain must not be blank");
        }

        if (LoopCount is < 1 or > 10_000)
        {
            throw new ArgumentOutOfRangeException(nameof(LoopCount), "loopCount must be between 1 and 10000");
        }

        if (Attributes.Count == 0)
        {
            throw new ArgumentException("at least one attribute is required");
        }

        if (Receivers is { Count: > 1 })
        {
            throw new ArgumentException("DataConnect accepts at most one receiver");
        }
    }
}

public sealed record GenerateResult(JsonNode? JsonValue, byte[]? Content, string? ContentType);
