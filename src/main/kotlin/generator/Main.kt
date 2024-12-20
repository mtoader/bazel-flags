package generator

import bazel.flags.BazelFlagsProto.FlagCollection
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.path
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.encoding.decodingWith
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

fun main(argv: Array<String>) = Main().main(argv)

@OptIn(ExperimentalEncodingApi::class)
class Main : CoreCliktCommand() {
  val bazelPath by option("--bazel-binary", help = "Location of bazel binary").path(
    mustExist = true, canBeDir = false, mustBeReadable = true
  )
  val target by option("--target", help = "Where to emit the file").path(canBeDir = false).default(Path("KnownFlags"))
  val dumpGenerated by option("--dumpOutput", help = "Should we dump the generated bits to stdout?").boolean()
    .default(false)

  override fun run() {
    val bazel = bazelPath?.pathString ?: "bazel"

    val process = ProcessBuilder(bazel, "help", "flags-as-proto").start()

    with(PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(target.toFile()))))) {
      var base64DecodedStream = process.inputStream.decodingWith(Base64.Default)

      parse(FlagCollection.parseFrom(base64DecodedStream), this)

      flush()
    }

    process.waitFor()
    if (dumpGenerated) {
      with(target.toFile()) {
        println(readText())
      }
    }
  }

  fun parse(flags: FlagCollection, writer: PrintWriter) {
    writer.println(header)

    flags.flagInfosList.sortedBy { it.name }.forEachIndexed { i, f ->
        i.takeIf { it > 0 }?.let {
          writer.println()
        }

        Flag(f).print(writer)
      }

    writer.println(footer)
  }

  val header: String
    get() = """
      |package org.jetbrains.bazel.languages.bazelrc.flags
      |
      |internal object ${target?.name?.replace(".kt", "")} {
      """.trimMargin()

  val footer: String = "}"

  companion object {
    fun main(argv: Array<String>) {
      Main().main(argv)
    }
  }
}

