package jsonsql.fileformats

import jsonsql.query.Table
import jsonsql.query.TableType
import jsonsql.query.TableType.*
import jsonsql.filesystems.FileSystem

interface FileFormat {

    fun reader(fs: FileSystem, path: String): Reader
    fun writer(fs: FileSystem, path: String, fields: List<String>): Writer
    fun split(): Boolean = true

    companion object {
        fun reader(table: Table) = forType(table.type).reader(FileSystem.from(table.path), table.path)
        fun writer(table: Table, fields: List<String>) = forType(table.type).writer(FileSystem.from(table.path), table.path, fields)

        fun forType(tableType: TableType) =
            when (tableType) {
                CSV -> CsvFormat
                JSON -> JsonFormat
                DIR -> DirFormat
            }
    }

    interface Reader: AutoCloseable {
        fun next(): Map<String,*>?
    }

    interface Writer: AutoCloseable {
        fun write(row: List<Any?>)
    }
}