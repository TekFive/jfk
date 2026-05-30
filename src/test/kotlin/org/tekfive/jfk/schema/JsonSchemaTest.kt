package org.tekfive.jfk.schema

import org.tekfive.jfk.JsonNull
import kotlin.test.*

class JsonSchemaTest {

    @Test
    fun `resolve finds definition by path`() {
        val nameSchema = JsonSchema(type = SchemaType.STRING)
        val root = JsonSchema(
            defs = mapOf("Name" to nameSchema)
        )
        val resolved = root.resolve("#/\$defs/Name")
        assertEquals(nameSchema, resolved)
    }

    @Test
    fun `resolve returns null for missing definition`() {
        val root = JsonSchema(defs = mapOf("Name" to JsonSchema(type = SchemaType.STRING)))
        assertNull(root.resolve("#/\$defs/Missing"))
    }

    @Test
    fun `resolve returns null for invalid path format`() {
        val root = JsonSchema(defs = mapOf("Name" to JsonSchema(type = SchemaType.STRING)))
        assertNull(root.resolve("Name"))
        assertNull(root.resolve("#/Name"))
        assertNull(root.resolve("/\$defs/Name"))
    }

    @Test
    fun `resolve returns null when no defs`() {
        val root = JsonSchema()
        assertNull(root.resolve("#/\$defs/Name"))
    }

    @Test
    fun `resolveRef resolves ref against root`() {
        val nameSchema = JsonSchema(type = SchemaType.STRING, minLength = 1)
        val root = JsonSchema(
            defs = mapOf("Name" to nameSchema),
            type = SchemaType.OBJECT,
            properties = mapOf("name" to JsonSchema(ref = "#/\$defs/Name")),
        )
        val refSchema = root.properties!!["name"]!!
        val resolved = refSchema.resolveRef(root)
        assertEquals(nameSchema, resolved)
    }

    @Test
    fun `resolveRef returns self when no ref`() {
        val schema = JsonSchema(type = SchemaType.STRING)
        val root = JsonSchema()
        assertSame(schema, schema.resolveRef(root))
    }

    @Test
    fun `resolveRef returns self when ref not found`() {
        val schema = JsonSchema(ref = "#/\$defs/Missing")
        val root = JsonSchema(defs = emptyMap())
        assertSame(schema, schema.resolveRef(root))
    }

    @Test
    fun `hasConst distinguishes const null from absent const`() {
        val withConstNull = JsonSchema(const = JsonNull, hasConst = true)
        val withoutConst = JsonSchema()
        assertTrue(withConstNull.hasConst)
        assertFalse(withoutConst.hasConst)
    }

    @Test
    fun `hasDefault distinguishes default null from absent default`() {
        val withDefaultNull = JsonSchema(default = JsonNull, hasDefault = true)
        val withoutDefault = JsonSchema()
        assertTrue(withDefaultNull.hasDefault)
        assertFalse(withoutDefault.hasDefault)
    }

}
