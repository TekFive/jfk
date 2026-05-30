package org.tekfive.jfk

import java.util.Base64
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Interface for deserializing JSON objects into typed instances.
 *
 * The default implementation uses reflection to map JSON properties to the primary constructor
 * parameters of [T]. Simply implement this interface on a companion object:
 *
 * ```
 * data class Color(val r: Int, val g: Int, val b: Int) {
 *     companion object : FromJsonObject<Color>
 * }
 *
 * val color = """{"r":255,"g":128,"b":0}""".fromJson(Color)
 * ```
 *
 * **Mapping rules:**
 * - Each constructor parameter is matched to a JSON property by name
 * - Primitive types (String, Int, Long, Double, Float, Boolean) are mapped directly
 * - If [lax] is true (default), string values are coerced to numeric/boolean types
 * - Nullable parameters receive null when the JSON property is missing or null
 * - Non-nullable List parameters receive an empty list when the JSON property is null
 * - Parameters with default values use their default when the JSON property is missing
 * - Non-nullable parameters without defaults throw [JsonMappingException] when missing
 * - Nested types must have a companion implementing [FromJsonObject]
 * - List properties are supported for primitives and [FromJsonObject]-backed types
 */
interface FromJsonObject<T : Any> {

    /**
     * Whether to use lax type coercion (e.g., parse "42" as Int). Defaults to true.
     */
    val lax: Boolean get() = true

    /**
     * Deserialize [json], returning `null` when [json] is null or mapping fails.
     */
    fun fromJsonOptional(json: JsonObject?): T? {
        if (json == null) return null
        return try {
            fromJson(json)
        } catch (e: JsonMappingException) {
            null
        }
    }

    /**
     * Deserialize [json] into an instance of [T].
     *
     * @throws JsonMappingException if a required property is missing or has an incompatible type.
     */
    fun fromJson(json: JsonObject): T {
        return fromJson(json, false)
    }

    /**
     * Deserialize a [JsonObject] into an instance of [T].
     *
     * The default implementation uses reflection on the primary constructor.
     * Override for custom deserialization logic.
     *
     * @param treatEmptyStringAsNull when true, a JSON property with value `""` is handled
     *   the same as a missing or null property — nullable parameters receive null, optional
     *   parameters use their default, and required non-nullable parameters throw
     *   [JsonMappingException]. Defaults to false.
     * @param overrides constructor property values that bypass JSON deserialization. Each pair
     *   maps a [KProperty] to the value to inject. The property is matched by name to the
     *   corresponding constructor parameter.
     *
     * @throws JsonMappingException if a required property is missing or has an incompatible type
     */
    fun fromJson(json: JsonObject, treatEmptyStringAsNull: Boolean, vararg overrides: Pair<KProperty<*>, Any?>): T {
        return JsonReflection.construct(this, json, "", treatEmptyStringAsNull, overrides)
    }

    /**
     * Deserialize a [JsonObject] into an instance of [T], calling [onError] when a property
     * is missing or has an incompatible type instead of throwing [JsonMappingException].
     *
     * The [onError] block receives the [JsonMappingException] and must return [Nothing] —
     * typically by throwing an application-specific exception.
     *
     * @param treatEmptyStringAsNull when true, empty string values are treated as null.
     * @param overrides constructor property values that bypass JSON deserialization.
     */
    fun fromJson(json: JsonObject, treatEmptyStringAsNull: Boolean, vararg overrides: Pair<KProperty<*>, Any?>, onError: (JsonMappingException) -> Nothing): T {
        return try {
            JsonReflection.construct(this, json, "", treatEmptyStringAsNull, overrides)
        } catch (e: JsonMappingException) {
            onError(e)
        }
    }

    /**
     * Apply JSON values to an existing instance's mutable (`var`) primary constructor properties.
     *
     * Only properties declared as `var` in the primary constructor are candidates. Properties
     * listed in [excludedProperties] are skipped. JSON properties that don't correspond to a
     * mutable constructor parameter are ignored. If a JSON value is present for a candidate
     * property but cannot be coerced to the expected type, [onError] is called.
     *
     * Properties not present in the JSON are left untouched.
     *
     * @param instance the existing object to mutate
     * @param json the JSON values to apply
     * @param excludedProperties var properties to skip
     * @param onError called when a JSON value cannot be coerced to the property type
     */
    fun applyJson(instance: T, json: JsonObject, vararg excludedProperties: KProperty<*>, onError: (JsonMappingException) -> Nothing): T {
        try {
            JsonReflection.apply(this, instance, json, lax, excludedProperties)
            return instance
        } catch (e: JsonMappingException) {
            onError(e)
        }
    }

    /**
     * Apply JSON values to an existing instance's mutable (`var`) primary constructor properties.
     *
     * @throws JsonMappingException if a JSON value cannot be coerced to the property type
     */
    fun applyJson(instance: T, json: JsonObject, vararg excludedProperties: KProperty<*>): T {
        JsonReflection.apply(this, instance, json, lax, excludedProperties)
        return instance
    }

    /**
     * Apply JSON values to only the specified mutable (`var`) primary constructor properties.
     *
     * Unlike [applyJson] which excludes listed properties, this method includes only the listed
     * properties. Properties not in [includedProperties] are left untouched regardless of whether
     * a corresponding JSON value exists.
     *
     * @param instance the existing object to mutate
     * @param json the JSON values to apply
     * @param includedProperties the specific var properties to apply from the JSON
     * @param onError called when a JSON value cannot be coerced to the property type
     * @throws IllegalArgumentException if any property in [includedProperties] is not a mutable
     *   (`var`) primary constructor property
     */
    fun applyJsonOnly(instance: T, json: JsonObject, vararg includedProperties: KProperty<*>, onError: (JsonMappingException) -> Nothing): T {
        try {
            JsonReflection.applyOnly(this, instance, json, lax, includedProperties)
            return instance
        } catch (e: JsonMappingException) {
            onError(e)
        }
    }

    /**
     * Apply JSON values to only the specified mutable (`var`) primary constructor properties.
     *
     * @throws IllegalArgumentException if any property in [includedProperties] is not a mutable
     *   (`var`) primary constructor property
     * @throws JsonMappingException if a JSON value cannot be coerced to the property type
     */
    fun applyJsonOnly(instance: T, json: JsonObject, vararg includedProperties: KProperty<*>): T {
        JsonReflection.applyOnly(this, instance, json, lax, includedProperties)
        return instance
    }
}

internal object JsonReflection {

    fun <T : Any> construct(
        fromJson: FromJsonObject<T>,
        json: JsonObject,
        pathPrefix: String,
        treatEmptyStringAsNull: Boolean = false,
        overrides: Array<out Pair<KProperty<*>, Any?>> = emptyArray(),
    ): T {
        val targetClass = resolveTargetClass(fromJson)
        val overridesByName = overrides.associate { it.first.name to it.second }
        return constructClass(targetClass, json, pathPrefix, fromJson.lax, treatEmptyStringAsNull, overridesByName)
    }

    private fun <T : Any> constructClass(
        klass: KClass<T>, json: JsonObject, pathPrefix: String, lax: Boolean,
        treatEmptyStringAsNull: Boolean, overridesByName: Map<String, Any?> = emptyMap(),
    ): T {
        val ctor = klass.primaryConstructor
            ?: throw JsonMappingException(pathPrefix, "primary constructor", json,
                "Class ${klass.simpleName} has no primary constructor")

        val params = mutableMapOf<KParameter, Any?>()

        for (param in ctor.parameters) {
            val key = param.name
                ?: throw JsonMappingException(pathPrefix, "named parameter", json,
                    "Constructor parameter at index ${param.index} has no name")

            if (overridesByName.containsKey(key)) {
                params[param] = overridesByName[key]
                continue
            }

            val path = buildPath(pathPrefix, key)
            val jsonValue = json[key]

            val isEffectivelyNull = jsonValue.isNull
                || (treatEmptyStringAsNull && jsonValue is JsonString && jsonValue.value.isEmpty())

            if (isEffectivelyNull) {
                when {
                    json.containsKey(key) && jsonValue.isNull && isNonNullableListType(param.type) ->
                        params[param] = emptyListValueFor(param.type)
                    param.isOptional -> continue
                    param.type.isMarkedNullable -> params[param] = null
                    else -> throw JsonMappingException(path, typeName(param.type), jsonValue)
                }
                continue
            }

            params[param] = convertValue(jsonValue, param.type, path, lax, treatEmptyStringAsNull)
        }

        return ctor.callBy(params)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> apply(
        fromJson: FromJsonObject<T>,
        instance: T,
        json: JsonObject,
        lax: Boolean,
        excludedProperties: Array<out KProperty<*>>,
    ) {
        val targetClass = resolveTargetClass(fromJson)
        val ctor = targetClass.primaryConstructor
            ?: throw IllegalStateException("Class ${targetClass.simpleName} has no primary constructor")

        val excludedNames = excludedProperties.map { it.name }.toSet()
        val mutableProps = targetClass.memberProperties
            .filterIsInstance<KMutableProperty1<T, Any?>>()
            .filter { it.visibility == KVisibility.PUBLIC }

        val ctorParamNames = ctor.parameters.mapNotNull { it.name }.toSet()
        val ctorParamsByName = ctor.parameters.associateBy { it.name }

        for (prop in mutableProps) {
            if (prop.name !in ctorParamNames) continue
            if (prop.name in excludedNames) continue

            val jsonValue = json[prop.name]
            if (jsonValue.isNull) continue

            val param = ctorParamsByName[prop.name] ?: continue
            val converted = convertValue(jsonValue, param.type, prop.name, lax)
            prop.setter.call(instance, converted)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> applyOnly(
        fromJson: FromJsonObject<T>,
        instance: T,
        json: JsonObject,
        lax: Boolean,
        includedProperties: Array<out KProperty<*>>,
    ) {
        val targetClass = resolveTargetClass(fromJson)
        val ctor = targetClass.primaryConstructor
            ?: throw IllegalStateException("Class ${targetClass.simpleName} has no primary constructor")

        val mutableProps = targetClass.memberProperties
            .filterIsInstance<KMutableProperty1<T, Any?>>()
            .filter { it.visibility == KVisibility.PUBLIC }
        val mutableNames = mutableProps.map { it.name }.toSet()

        val ctorParamNames = ctor.parameters.mapNotNull { it.name }.toSet()
        val ctorParamsByName = ctor.parameters.associateBy { it.name }

        val includedNames = includedProperties.map { prop ->
            if (prop.name !in mutableNames || prop.name !in ctorParamNames) {
                throw IllegalArgumentException(
                    "Property '${prop.name}' is not a mutable (var) primary constructor property of ${targetClass.simpleName}"
                )
            }
            prop.name
        }.toSet()

        for (prop in mutableProps) {
            if (prop.name !in includedNames) continue

            val jsonValue = json[prop.name]
            if (jsonValue.isNull) continue

            val param = ctorParamsByName[prop.name] ?: continue
            val converted = convertValue(jsonValue, param.type, prop.name, lax)
            prop.setter.call(instance, converted)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> resolveTargetClass(fromJson: FromJsonObject<T>): KClass<T> {
        for (supertype in fromJson::class.supertypes) {
            val targetType = resolveFromJsonTargetType(supertype, emptyMap()) ?: continue
            val targetClass = targetType.classifier as? KClass<*> ?: continue
            return targetClass as KClass<T>
        }
        throw IllegalStateException("Could not resolve target type for ${fromJson::class.simpleName}")
    }

    private fun resolveFromJsonTargetType(type: KType, bindings: Map<KTypeParameter, KType>): KType? {
        val classifier = type.classifier as? KClass<*> ?: return null
        if (classifier == FromJsonObject::class) {
            val targetType = type.arguments.firstOrNull()?.type ?: return null
            return resolveTypeArgument(targetType, bindings)
        }

        val typeParameterBindings = classifier.typeParameters.zip(type.arguments).mapNotNull { (parameter, argument) ->
            val argumentType = argument.type ?: return@mapNotNull null
            parameter to resolveTypeArgument(argumentType, bindings)
        }.toMap()
        val nextBindings = bindings + typeParameterBindings

        return classifier.supertypes.firstNotNullOfOrNull { supertype ->
            resolveFromJsonTargetType(supertype, nextBindings)
        }
    }

    private tailrec fun resolveTypeArgument(type: KType, bindings: Map<KTypeParameter, KType>): KType {
        val typeParameter = type.classifier as? KTypeParameter ?: return type
        val resolved = bindings[typeParameter] ?: return type
        return if (resolved == type) type else resolveTypeArgument(resolved, bindings)
    }

    private fun convertValue(value: JsonValue, type: KType, path: String, lax: Boolean, treatEmptyStringAsNull: Boolean = false): Any? {
        val classifier = type.classifier as? KClass<*>
            ?: throw JsonMappingException(path, "known type", value)

        if (value.isNull) {
            if (type.isMarkedNullable) return null
            throw JsonMappingException(path, typeName(type), value)
        }

        return when {
            classifier == String::class -> if (lax) value.laxString else value.string
            classifier == Number::class -> if (lax) value.laxDouble else value.double
            classifier == Int::class -> if (lax) value.laxInt else value.int
            classifier == Long::class -> if (lax) value.laxLong else value.long
            classifier == Double::class -> if (lax) value.laxDouble else value.double
            classifier == Float::class -> (if (lax) value.laxDouble else value.double)?.toFloat()
            classifier == Boolean::class -> if (lax) value.laxBoolean else value.boolean
            classifier == ByteArray::class -> decodeByteArray(value, path)
            classifier == JsonValue::class -> value
            classifier == JsonContainer::class -> value as? JsonContainer
            classifier == JsonObject::class -> value as? JsonObject
            classifier == JsonArray::class -> value as? JsonArray
            classifier == JsonString::class -> value as? JsonString
            classifier == JsonNumber::class -> value as? JsonNumber
            classifier == JsonBool::class -> value as? JsonBool
            classifier == Any::class -> value.toValue()
            classifier.isSubclassOf(Map::class) -> convertMap(value, type, path, lax)
            classifier.isSubclassOf(Collection::class) -> convertCollection(value, type, path, lax, classifier)
            hasFromJsonCompanion(classifier) -> convertNested(value, classifier, path, lax, treatEmptyStringAsNull)
            classifier.isSubclassOf(Enum::class) -> convertEnum(value, classifier, path)
            else -> throw JsonMappingException(path, typeName(type), value)
        } ?: throw JsonMappingException(path, typeName(type), value)
    }

    private fun convertEnum(value: JsonValue, klass: KClass<*>, path: String): Any {
        val name = value.string
            ?: throw JsonMappingException(path, "String (for ${klass.simpleName})", value)
        @Suppress("UNCHECKED_CAST")
        val constants = klass.java.enumConstants as Array<Enum<*>>
        return constants.firstOrNull { it.name == name }
            ?: throw JsonMappingException(path, "one of ${constants.map { it.name }}", value,
                "Unknown enum value '$name' for ${klass.simpleName}")
    }

    private fun hasFromJsonCompanion(klass: KClass<*>): Boolean {
        val companion = klass.companionObjectInstance
        return (companion is FromJsonObject<*>)
    }

    private fun convertNested(value: JsonValue, klass: KClass<*>, path: String, lax: Boolean, treatEmptyStringAsNull: Boolean = false): Any? {
        // When the target is an enum with a FromJsonObject companion (e.g., DataEnum) but the
        // value is a plain string, resolve by enum constant name instead of requiring the full
        // object format. This allows enums to be deserialized from either "NAME" or {id, name, ...}.
        if (value is JsonString && klass.isSubclassOf(Enum::class)) {
            return convertEnum(value, klass, path)
        }

        val obj = value as? JsonObject
            ?: throw JsonMappingException(path, "JsonObject (for ${klass.simpleName})", value)

        val companion = klass.companionObjectInstance
        if (companion is FromJsonObject<*>) {
            if (klass.isSubclassOf(Enum::class) || hasCustomFromJson(companion)) {
                return companion.fromJson(obj)
            }

            @Suppress("UNCHECKED_CAST")
            return constructClass(klass as KClass<Any>, obj, path, lax, treatEmptyStringAsNull)
        }

        throw JsonMappingException(path, "${klass.simpleName} (must have companion : FromJsonObject)", value,
            "${klass.simpleName} does not have a companion implementing FromJsonObject")
    }

    private fun decodeByteArray(value: JsonValue, path: String): ByteArray? {
        val encoded = value.string ?: return null
        return try {
            Base64.getDecoder().decode(encoded)
        } catch (e: IllegalArgumentException) {
            throw JsonMappingException(
                path,
                "Base64-encoded String",
                value,
                "Required Base64-encoded String at '${path.ifEmpty { "<root>" }}' but found ${JsonMappingException.describeValue(value)}: ${e.message}",
            )
        }
    }

    private fun hasCustomFromJson(companion: FromJsonObject<*>): Boolean {
        return companion::class.java.methods.any {
            it.name == "fromJson" &&
                !it.isBridge &&
                !it.isSynthetic &&
                it.parameterTypes.singleOrNull() == JsonObject::class.java &&
                it.declaringClass != FromJsonObject::class.java
        }
    }

    private fun convertMap(value: JsonValue, type: KType, path: String, lax: Boolean): Map<String, *> {
        val obj = value as? JsonObject
            ?: throw JsonMappingException(path, "JsonObject (for Map)", value)
        val keyType = type.arguments.getOrNull(0)?.type
            ?: throw JsonMappingException(path, "Map with known key type", value)
        val keyClassifier = keyType.classifier as? KClass<*>
        if (keyClassifier != String::class) {
            throw JsonMappingException(path, "Map with String keys", value)
        }
        val valueType = type.arguments.getOrNull(1)?.type
            ?: throw JsonMappingException(path, "Map with known value type", value)

        return obj.entries.entries.associate { (key, jsonValue) ->
            key to convertValue(jsonValue, valueType, "$path.$key", lax)
        }
    }

    private fun convertCollection(value: JsonValue, type: KType, path: String, lax: Boolean, classifier: KClass<*>): Collection<*> {
        val arr = value as? JsonArray
            ?: throw JsonMappingException(path, "JsonArray", value)
        val elementType = type.arguments.firstOrNull()?.type
            ?: throw JsonMappingException(path, "Collection with known element type", value)

        val elements = arr.items.mapIndexed { i, elem ->
            convertValue(elem, elementType, "$path[$i]", lax)
        }
        return if (classifier.isSubclassOf(Set::class)) elements.toSet() else elements
    }

    private fun isNonNullableListType(type: KType): Boolean {
        if (type.isMarkedNullable) return false
        val classifier = type.classifier as? KClass<*> ?: return false
        return classifier.isSubclassOf(List::class)
    }

    private fun emptyListValueFor(type: KType): List<*> {
        val classifier = type.classifier as? KClass<*> ?: return emptyList<Any?>()
        return if (classifier.isSubclassOf(MutableList::class)) mutableListOf<Any?>() else emptyList<Any?>()
    }

    private fun buildPath(prefix: String, key: String): String =
        if (prefix.isEmpty()) key else "$prefix.$key"

    private fun typeName(type: KType): String {
        val name = (type.classifier as? KClass<*>)?.simpleName ?: type.toString()
        return if (type.isMarkedNullable) "$name?" else name
    }
}
