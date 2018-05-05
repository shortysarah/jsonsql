package jsonsql.fileformats

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import jsonsql.filesystems.FileSystem
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvFormat: FileFormat {
    override fun reader(path: String): FileFormat.Reader = Reader(path)
    override fun writer(path: String) = TODO("not implemented")

    private class Reader(val path: String): FileFormat.Reader {
        private val files: Iterator<String> by lazy(::listDirs)
        private var reader: CSVReader? = null
        private var headers: Array<String> = arrayOf()

        override fun next(): Map<String, *>? {
            while (true) {
                if (reader == null) {
                    if (files.hasNext()) {
                        openReader(files.next())
                    } else {
                        return null
                    }
                }

                val raw = reader!!.readNext()

                if (raw != null) {
                    return raw.mapIndexed { index, s -> headers.getOrElse(index, { "_col_$it" }) to s }
                            .associate { it }
                } else {
                    reader = null
                }
            }
        }

        override fun close() {
            reader?.close()
        }

        private fun listDirs(): Iterator<String> {
            // Sort so the describe operator has more of a chance of getting the latest data
            return FileSystem.listDir(path).sortedDescending().iterator()
        }

        private fun openReader(path: String) {
            val bufferedReader = BufferedReader(InputStreamReader(FileSystem.read(path)))
            val parser = CSVParserBuilder()
                    .withEscapeChar('\\')
                    .withQuoteChar('"')
                    .withSeparator(',')
                    .build()

            reader = CSVReaderBuilder(bufferedReader).withCSVParser(parser).build()
            headers = reader!!.readNext()
        }
    }
}