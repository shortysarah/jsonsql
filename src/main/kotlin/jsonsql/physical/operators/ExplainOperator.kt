package jsonsql.physical.operators

import jsonsql.ast.Field
import jsonsql.physical.PhysicalOperator
import jsonsql.physical.VectorizedPhysicalOperator

class ExplainOperator(val stmt: VectorizedPhysicalOperator): PhysicalOperator() {
    private lateinit var plan: Iterator<String>

    override fun columnAliases() = listOf(Field(null, "plan"))

    override fun compile() {
        stmt.compile()
        plan = buildOutput(stmt).iterator()
    }

    override fun next(): List<Any?>? {
        return if (plan.hasNext()) {
            listOf(plan.next())
        } else {
            null
        }
    }

    override fun close() {} // Noop

    private fun buildOutput(operator: VectorizedPhysicalOperator,
                            outputLines: MutableList<String> = mutableListOf(),
                            indent: Int = 0): MutableList<String> {
        TODO()
//        outputLines.add( (0 until indent).map { "  " }.joinToString("") + "$operator")
//        operator.children().forEach { buildOutput(it, outputLines, indent + 1) }
//        return outputLines
    }
}