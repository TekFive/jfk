# JFK - JSON for Kotlin

JFK is a Kotlin-idiomatic JSON library focused on simple parsing, mutable JSON
trees, reflection-based mapping, and a compact DSL.

## Features

- Reflection-based serialization and deserialization with no code generation
- Kotlin DSL for building JSON objects and arrays
- Streaming parser for strings, readers, and input streams
- Strict, lax, and required accessors
- Mutable `JsonObject` and `JsonArray` tree model
- Parse-time transforms for value rewriting and default injection
- JSON Schema model, reader, writer, and builder support

## Build

JFK targets Java 21.

```bash
./gradlew test
```

Publish to the local Maven repository:

```bash
./gradlew publishToMavenLocal
```

## Quick Start

```kotlin
import org.tekfive.jfk.FromJsonObject
import org.tekfive.jfk.ToJsonObject
import org.tekfive.jfk.fromJsonOrThrow

data class Address(val city: String, val zip: String) : ToJsonObject {
    companion object : FromJsonObject<Address>
}

data class Person(val name: String, val age: Int, val address: Address) : ToJsonObject {
    companion object : FromJsonObject<Person>
}

val person = """{"name":"Alice","age":30,"address":{"city":"Austin","zip":"78701"}}"""
    .fromJsonOrThrow(Person)

val json = person.toJsonString(indent = 2)
```

## JSON Tree

`JsonValue` is the sealed root type for:

- `JsonObject`
- `JsonArray`
- `JsonString`
- `JsonNumber`
- `JsonBool`
- `JsonNull`

Parse JSON directly:

```kotlin
val value = Json.parse("""{"name":"Alice","age":30}""")
val name = value["name"].reqString
val age = value["age"].int
```

Build JSON with the DSL:

```kotlin
val obj = json {
    "name" set "Alice"
    "age" set 30
    "tags" set jsonArray("kotlin", "json")
}
```

## Accessors

JFK exposes three accessor styles:

- Strict accessors return `null` on type mismatch, such as `value["age"].int`.
- Lax accessors coerce compatible strings, such as `value["age"].laxInt`.
- Required accessors throw with path context, such as `value["age"].reqInt`.

`JsonObject.bigDecimal(name, treatEmptyStringAsNull = false)` accepts numbers and
decimal strings. Missing or null values return `null`; empty strings also return
`null` when the flag is true. Invalid values throw `JsonMappingException`.

## JSON Paths

Use `JsonPath` for an exact location containing object keys and zero-based array
indices:

```kotlin
val path = JsonPath.parse("$.users[0].name")
val name = value.at(path).reqString

val samePath = JsonPath.Root.key("users").index(0).key("name")
check(path == samePath)
```

Paths start with `$`, which identifies the supplied value itself, including a
subtree or root array. Each segment is `.identifier`, `["quoted key"]`, or
`[index]`. Identifiers use ASCII letters, digits, and underscores and cannot start
with a digit. Quoted keys use JSON string escaping and can contain any key,
including punctuation, Unicode, or the empty string.

| Path | Meaning |
| --- | --- |
| `$.users[0].name` | First user's name |
| `$[2].id` | ID in the third element of a root array |
| `$.matrix[1][3]` | Fourth element of the second row |
| `$["a.b"]["x[y]"]` | Keys containing punctuation |
| `$["0"]` | Object key `"0"` |
| `$[0]` | Array index zero |
| `$[""]` | Empty object key |

Paths are immutable and compare by their typed segments. `toString()` produces a
canonical path using dot notation wherever possible; parsing it returns an equal
path. Indices must fit a nonnegative Kotlin `Int`, without leading zeroes.
Whitespace outside quoted keys, negative indices, wildcards, slices, and implicit
traversal across arrays are unsupported. Invalid syntax throws
`JsonPathParseException` with a zero-based UTF-16 `offset` into the input.

Missing keys, out-of-range indices, and incompatible types return `JsonNull`, as
with chained accessors. Explicit JSON null also returns `JsonNull`.
The existing `at(String)` retains its dot-separated object-key behavior; use
`at(JsonPath.parse(...))` for this notation.

## Mapping Rules

`FromJsonObject` maps JSON object keys to primary constructor parameters by exact
name. `ToJsonObject` writes public primary-constructor properties with the same
key names. There is no automatic case conversion. Both `java.time.Instant` and
`kotlin.time.Instant` are represented as ISO-8601 strings. Both `java.util.UUID`
and `kotlin.uuid.Uuid` are represented as canonical UUID strings.
`java.math.BigDecimal` accepts numbers and decimal strings, even when `lax` is
false. Use decimal strings when parsing JSON that must preserve arbitrary
precision: the JSON parser stores fractional numbers as `Double`.

Missing or `null` values follow Kotlin constructor semantics:

- Non-null parameters without defaults must be present and non-null.
- Parameters with defaults use the default when the key is missing.
- Nullable parameters accept `null`.

## License

See `LICENSE`.
