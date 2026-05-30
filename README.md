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

## Mapping Rules

`FromJsonObject` maps JSON object keys to primary constructor parameters by exact
name. `ToJsonObject` writes public primary-constructor properties with the same
key names. There is no automatic case conversion.

Missing or `null` values follow Kotlin constructor semantics:

- Non-null parameters without defaults must be present and non-null.
- Parameters with defaults use the default when the key is missing.
- Nullable parameters accept `null`.

## License

See `LICENSE`.
