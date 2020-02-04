package jsonsql.physical.operators

import jsonsql.ast.Ast
import jsonsql.ast.Field
import jsonsql.ast.TableType
import jsonsql.fileformats.FileFormat
import jsonsql.physical.*


class DescribeOperator(
        private val table: Ast.Table,
        private val tableOutput: Boolean
): PhysicalOperator() {

    override val columnAliases =
        (if (tableOutput) listOf("table") else listOf("column_name", "column_type")).map { Field(null, it) }

    override fun data(context: ExecutionContext): ClosableSequence<Tuple> {
        return scanTable().asSequence().withClose()
    }

    private fun scanTable(): Sequence<Tuple> {

        val cols = mutableMapOf<String, UsedTypes>()

        val tableReader = FileFormat.reader(table)

        for (i in 0 until 2000) {
            val json = tableReader.next()
            json ?: break
            json.forEach { (key, value) ->
                val usedTypes = cols.computeIfAbsent(key) { UsedTypes() }
                populateUsedTypes(usedTypes, value)
            }
        }
        tableReader.close()

        val outRows = if (table.type == TableType.JSON) {
            cols.toSortedMap()
        } else {
            cols
        }


        return if(tableOutput) {
            val rows = outRows.map {
                it.value.tableString("  ")

                "  ${it.key} ${it.value.tableString("  ").trimStart()}"
            }.joinToString(",\n")

            """
CREATE TABLE '${table.path}' (
$rows
)
""".splitToSequence("\n").map { listOf(it) }
        } else {
            outRows.map { listOf(it.key, it.value.toString()) }.asSequence()
        }
    }

    private fun populateUsedTypes(usedTypes: UsedTypes, value: Any?) {
        when(value) {
            is Boolean -> usedTypes.couldBeBoolean = true
            is Number -> usedTypes.couldBeNumber = true
            is String -> usedTypes.couldBeString = true
            is Map<*,*> -> {
                usedTypes.couldBeStruct = true
                value.forEach { (k, v) ->
                    val u = usedTypes.structEntries.computeIfAbsent(k as String) { UsedTypes() }
                    populateUsedTypes(u, v)
                }
            }
            is List<*> -> {
                usedTypes.couldBeArray = true
                if (usedTypes.arrayEntries == null) usedTypes.arrayEntries = UsedTypes()
                value.map { populateUsedTypes(usedTypes.arrayEntries!!, it) }
            }
        }
    }


    // Class to keep track of what we see
    private data class UsedTypes(
            var couldBeNumber: Boolean = false,
            var couldBeString: Boolean = false,
            var couldBeBoolean: Boolean = false,
            var couldBeStruct: Boolean = false,
            var couldBeArray: Boolean = false,
            var arrayEntries: UsedTypes? = null,
            val structEntries: MutableMap<String, UsedTypes> = mutableMapOf()
    ) {
        override fun toString(): String {
            val descriptions = mutableListOf<String>()
            if (couldBeNumber) descriptions.add("Number")
            if (couldBeString) descriptions.add("String")
            if (couldBeBoolean) descriptions.add("Boolean")
            if (couldBeArray) descriptions.add("Array<${arrayEntries!!}>")
            if (couldBeStruct) descriptions.add("Struct<$structEntries>")
            return descriptions.joinToString(" OR ")
        }

        fun tableString(baseIndent: String): String {
            val types = listOf(couldBeNumber, couldBeString, couldBeBoolean, couldBeArray, couldBeStruct).filter { it }
            if(types.isEmpty()) return "${baseIndent}NULL"

            val union = types.size > 1

            val unionItems = mutableListOf<String>()
            val indent = if(union) "$baseIndent  " else baseIndent


            if (couldBeNumber) unionItems.add("${indent}NUMBER")
            if (couldBeString) unionItems.add("${indent}STRING")
            if (couldBeBoolean) unionItems.add("${indent}BOOLEAN")
            if (couldBeArray) {
                unionItems.add("${indent}ARRAY<" + arrayEntries!!.tableString(indent).trimStart() + ">")
            }
            if (couldBeStruct) {
                if (structEntries.isEmpty()) {
                    unionItems.add("${indent}STRUCT<>")
                } else {
                    val entryString = structEntries.map { (key, value) ->
                        "$indent  $key: " + value.tableString("$indent  ").trimStart()
                    }.joinToString(",\n")
                    unionItems.add("${indent}STRUCT<\n" + entryString + "\n${indent}>")
                }
            }

            return if (union) {
                "${baseIndent}UNION<\n${unionItems.joinToString(",\n")}\n${baseIndent}>"
            } else {
                unionItems.first()
            }
        }
    }


}