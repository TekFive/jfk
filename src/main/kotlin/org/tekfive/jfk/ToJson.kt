package org.tekfive.jfk

import java.util.Base64
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Interface for serializing instances to JSON.
 *
 * The default implementation uses reflection to serialize all public primary constructor
 * properties to a [JsonObject].
 *
 * ```
 * data class Color(val r: Int, val g: Int, val b: Int) : ToJsonObject
 *
 * val json = Color(255, 128, 0).toJsonObject()
 * // {"r":255,"g":128,"b":0}
 * ```
 *
 * **Mapping rules:**
 * - Public primary constructor properties are serialized by name
 * - Primitive types (String, Int, Long, Double, Float, Boolean) are mapped directly
 * - Null values are serialized as JsonNull
 * - Nested objects implementing [ToJsonObject] are serialized recursively
 * - Nested objects NOT implementing [ToJsonObject] are silently skipped
 * - Lists are supported for primitives and [ToJsonObject]-backed types
 * - Override [additionalJsonValues] to add extra entries beyond the constructor
 *
 * Override [toJsonObject] for fully custom serialization.
 */
interface ToJsonObject {

    /**
     * Serializes this value to a JSON string.
     *
     * @param indent number of spaces per level for pretty output; `null` or `0` writes compact JSON.
     */
    fun toJsonString(indent: Int? = null): String {
        return toJsonObject().toJsonString(indent)
    }

    /**
     * The properties to include in JSON serialization.
     *
     * By default, returns all public primary constructor properties. Override to control which
     * properties are serialized without replacing the entire [toJsonObject] implementation.
     *
     * ```
     * data class User(val name: String, val internalId: Int) : ToJsonObject {
     *     override val jsonProperties: List<KProperty<*>>
     *         get() = listOf(User::name)
     * }
     * ```
     */
    val jsonProperties: List<KProperty<*>>
        get() = JsonSerialization.defaultProperties(this)

    /**
     * Serialize this instance to a [JsonObject].
     *
     * The default implementation serializes [jsonProperties] and appends [additionalJsonValues].
     * Override for fully custom serialization.
     */
    fun toJsonObject(): JsonObject {
        return JsonSerialization.serialize(this)
    }

    /**
     * Serialize only the provided public properties from this instance.
     *
     * Each property must belong to this instance's class or a superclass. This overload intentionally
     * requires at least one property so callers do not accidentally create an empty object.
     */
    fun toJsonObject(firstIncludedProperty: KProperty<*>, vararg additionalIncludedProperties: KProperty<*>): JsonObject {
        return JsonSerialization.serializeOnly(this, listOf(firstIncludedProperty) + additionalIncludedProperties)
    }

    /**
     * Serialize this instance like [toJsonObject], excluding the provided public properties.
     *
     * Each property must belong to this instance's class or a superclass. This overload intentionally
     * requires at least one property so callers cannot accidentally request no exclusions.
     */
    fun toJsonObjectExcluding(firstExcludedProperty: KProperty<*>, vararg additionalExcludedProperties: KProperty<*>): JsonObject {
        return JsonSerialization.serializeExcluding(this, listOf(firstExcludedProperty) + additionalExcludedProperties)
    }

    /**
     * Provide additional properties to include in the JSON output beyond the primary constructor.
     * Called after constructor properties are serialized. Entries here will be added to (or override)
     * the generated JSON.
     *
     * Values are converted to [JsonValue] automatically using the same rules as constructor
     * properties (String, Number, Boolean, null, [ToJsonObject], [JsonValue], Map, List).
     *
     * ```
     * data class User(val name: String) : ToJsonObject {
     *     val greeting: String get() = "Hello, $name!"
     *
     *     override fun additionalJsonValues(): Map<String, Any?> = mapOf(
     *         "greeting" to greeting
     *     )
     * }
     * ```
     */
    fun additionalJsonValues(): Map<String, Any?> = emptyMap()

    /**
     * Whether to include properties with null values in the JSON output.
     *
     * When `false` (the default), null-valued properties are omitted from the resulting
     * [JsonObject]. When `true`, they are included as [JsonNull].
     */
    val includeNullProperties: Boolean
        get() = false
}

/**
 * Interface for values that provide their own [JsonValue] representation.
 */
interface ToJsonValue {
    /**
     * Serializes this value directly to a [JsonValue].
     */
    fun toJsonValue(): JsonValue
}

internal object JsonSerialization {

    /**
     * Returns the default list of properties for JSON serialization: all public primary
     * constructor properties of the instance's class.
     */
    fun defaultProperties(instance: ToJsonObject): List<KProperty<*>> {
        val klass = instance::class
        val ctor = klass.primaryConstructor ?: return emptyList()
        val memberProps = klass.memberProperties.associateBy { it.name }
        val ctorParamNames = ctor.parameters.mapNotNull { it.name }.toSet()

        return memberProps.values
            .filter { it.name in ctorParamNames && it.visibility == KVisibility.PUBLIC }
            .sortedBy { prop -> ctor.parameters.indexOfFirst { it.name == prop.name } }
    }

    fun serialize(instance: ToJsonObject): JsonObject {
        return serialize(instance, instance.jsonProperties, includeAdditionalValues = true)
    }

    fun serializeOnly(instance: ToJsonObject, includedProperties: List<KProperty<*>>): JsonObject {
        return serialize(instance, validatedProperties(instance, includedProperties), includeAdditionalValues = false)
    }

    fun serializeExcluding(instance: ToJsonObject, excludedProperties: List<KProperty<*>>): JsonObject {
        val excludedNames = validatedProperties(instance, excludedProperties).map { it.name }.toSet()
        val properties = instance.jsonProperties.filter { it.name !in excludedNames }
        return serialize(instance, properties, includeAdditionalValues = true).also { json ->
            excludedNames.forEach { json.remove(it) }
        }
    }

    private fun serialize(
        instance: ToJsonObject,
        properties: List<KProperty<*>>,
        includeAdditionalValues: Boolean,
    ): JsonObject {
        val map = LinkedHashMap<String, JsonValue>()
        val includeNulls = instance.includeNullProperties

        for (prop in properties) {
            @Suppress("UNCHECKED_CAST")
            val value = (prop as KProperty1<Any, *>).get(instance)
            if (value == null) {
                if (includeNulls) {
                    map[prop.name] = JsonNull
                }
                continue
            }
            val jsonValue = convertValue(value, prop.returnType)
            if (jsonValue != null) {
                map[prop.name] = jsonValue
            }
        }

        if (includeAdditionalValues) {
            for ((key, value) in instance.additionalJsonValues()) {
                map[key] = value.toJsonValue()
            }
        }

        return JsonObject(map)
    }

    private fun validatedProperties(instance: ToJsonObject, properties: List<KProperty<*>>): List<KProperty<*>> {
        val instanceClass = instance::class
        val memberProperties = instanceClass.memberProperties.associateBy { it.name }

        return properties.map { property ->
            val memberProperty = memberProperties[property.name]
                ?: throw IllegalArgumentException(
                    "Property '${property.name}' is not a property of ${instanceClass.simpleName}"
                )

            if (memberProperty.visibility != KVisibility.PUBLIC) {
                throw IllegalArgumentException(
                    "Property '${property.name}' is not a public property of ${instanceClass.simpleName}"
                )
            }

            val ownerClass = property.ownerClass()
            if (ownerClass == null || !ownerClass.isSuperclassOf(instanceClass)) {
                throw IllegalArgumentException(
                    "Property '${property.name}' is not a property of ${instanceClass.simpleName}"
                )
            }

            memberProperty
        }
    }

    private fun KProperty<*>.ownerClass(): KClass<*>? {
        return (this as? KProperty1<*, *>)
            ?.parameters
            ?.firstOrNull()
            ?.type
            ?.classifier as? KClass<*>
    }

    private fun convertValue(value: Any, type: KType): JsonValue? {
        return when (value) {
            is JsonValue -> value
            is String -> JsonString(value)
            is Int -> JsonNumber(value)
            is Long -> JsonNumber(value)
            is Double -> JsonNumber(value)
            is Float -> JsonNumber(value)
            is Boolean -> JsonBool(value)
            is ByteArray -> JsonString(Base64.getEncoder().encodeToString(value))
            is ToJsonObject -> value.toJsonObject()
            is Map<*, *> -> convertMap(value)
            is Collection<*> -> convertCollection(value, type)
            is ToJsonValue -> value.toJsonValue()
            is Enum<*> -> JsonString(value.name)
            else -> null
        }
    }

    private fun convertCollection(list: Collection<*>, type: KType): JsonArray? {
        val elementType = type.arguments.firstOrNull()?.type
        val elements = list.map { elem ->
            if (elem == null) {
                JsonNull
            } else if (elementType != null) {
                convertValue(elem, elementType) ?: return null
            } else {
                convertAny(elem) ?: return null
            }
        }
        return JsonArray(elements)
    }

    private fun convertMap(map: Map<*, *>): JsonObject? {
        val entries = LinkedHashMap<String, JsonValue>()
        for ((key, value) in map) {
            entries[key.toString()] = convertAny(value) ?: return null
        }
        return JsonObject(entries)
    }

    private fun convertAny(value: Any?): JsonValue? {
        if (value == null) return JsonNull
        return when (value) {
            is JsonValue -> value
            is String -> JsonString(value)
            is Int -> JsonNumber(value)
            is Long -> JsonNumber(value)
            is Double -> JsonNumber(value)
            is Float -> JsonNumber(value)
            is Boolean -> JsonBool(value)
            is ByteArray -> JsonString(Base64.getEncoder().encodeToString(value))
            is ToJsonObject -> value.toJsonObject()
            is Map<*, *> -> convertMap(value)
            is Collection<*> -> JsonArray(value.map { convertAny(it) ?: return null })
            is ToJsonValue -> value.toJsonValue()
            is Enum<*> -> JsonString(value.name)
            else -> null
        }
    }
}
