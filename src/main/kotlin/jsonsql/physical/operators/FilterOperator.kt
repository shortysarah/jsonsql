package jsonsql.physical.operators

import jsonsql.ast.Ast
import jsonsql.functions.BooleanInspector
import jsonsql.physical.ExpressionExecutor
import jsonsql.physical.PhysicalOperator
import jsonsql.physical.compileExpression

class FilterOperator(val predicate: Ast.Expression, val source: PhysicalOperator): PhysicalOperator() {
    private lateinit var compiledExpression: ExpressionExecutor

    override fun columnAliases() = source.columnAliases()

    override fun compile() {
        source.compile()

        compiledExpression = compileExpression(predicate, columnAliases())
    }

    override fun next(): List<Any?>? {
        while(true) {
            val sourceRow = source.next()
            sourceRow ?: return null
            val result = compiledExpression.evaluate(sourceRow)

            if (BooleanInspector.inspect(result) == true) return sourceRow
        }
    }

    override fun close() {
        source.close()
    }

    // For explain output
    override fun toString() = "Filter(${predicate})"
    override fun children() = listOf(source)
}
