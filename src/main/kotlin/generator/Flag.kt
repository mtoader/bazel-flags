package generator

import bazel.flags.BazelFlagsProto
import java.io.PrintWriter

const val RAW_LIT_QUOTE = "\"\"\"";
const val INDENT = "  "

class Flag(val f: BazelFlagsProto.FlagInfo) {
  fun print(w: PrintWriter) {
    val optionFields = listOfNotNull(
      """name = "${f.name}"""",
      oldName,
      abbrev,
      defaultArg,
      allowMultiple,
      effectTags,
      metadataTags,
      expandsAnnotation,
      help,
      typeHelp
    ).joinToString(",\n")

    val option = """
      |@Option(
      |${optionFields.replaceIndent(INDENT)},
      |)
    """.trimMargin()

    var annotatedValue =
      listOf(option, "@JvmField", """@Suppress("unused")""", valDeclaration).joinToString("\n")

    w.println(annotatedValue.replaceIndent(INDENT))
  }

  val valDeclaration: String
    get() = when (varName.length) {
      in 55..Int.MAX_VALUE -> """
          |val $varName =
          |  ${knownFlagType ?: "Flag.Unknown"}("${f.name.snakeToCamelCase()}")
        """.trimMargin()

      else -> """val $varName = ${knownFlagType ?: "Flag.Unknown"}("${f.name.snakeToCamelCase()}")"""
    }

  val varName: String
    get() = when (f.name) {
      "null" -> "`null`"
      else -> f.name.snakeToCamelCase()
    }

  val massagedDescription: String
    get() = f.documentation.replace("$", "\${'$'}")

  val oldName: String?
    get() = f.oldOptionName.takeIf(String::isNotEmpty)?.let { """oldName = "$it"""" }

  val effectTags: String?
    get() =
      f.effectTagsList
        .filter { it != "UNKNOWN" }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { "OptionEffectTag.${it}" }
        ?.wrapAndIndent(100, "")
        ?.let {
          if (!it.contains("\n")) {
            "effectTags = [$it]"
          } else {
            """
            |effectTags = [
            |${it.replaceIndent("  ")},
            |]
            """.trimMargin()
          }
        }


  val metadataTags: String?
    get() =
      f.metadataTagsList
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { "OptionMetadataTag.${it}" }
        ?.wrapAndIndent(100)
        ?.let {
          if (!it.contains("\n")) {
            "metadataTags = [$it]"
          } else {
            """
            |metadataTags = [
            |${it.replaceIndent("  ")},
            |]
            """.trimMargin()
          }
        }

  val expandsAnnotation: String?
    get() = f.expansionList
      .takeIf { it.isNotEmpty() }
      ?.joinToString(", ") { """"$it"""" }
      ?.wrapAndIndent(100, "")
      ?.let {
        if (!it.contains("\n")) {
          "expandsTo = [$it]"
        } else {
          """
          |expandsTo = [
          |${it.replaceIndent("  ")},
          |]
          """.trimMargin()
        }
      }


  val abbrev: String?
    get() = if (f.hasAbbreviation()) "abbrev = '${f.abbreviation.removePrefix("-")}'" else null

  val allowMultiple: String?
    get() = if (f.allowsMultiple) "allowMultiple = true" else null

  val defaultArg: String?
    get() = f.defaultValue?.let {
      when {
        it in listOf("null", "see description") -> null
        it.contains('"') -> """defaultValue = $RAW_LIT_QUOTE$it$RAW_LIT_QUOTE"""
        else -> """defaultValue = "$it""""
      }
    }

  val help: String?
    get() = massagedDescription.let {
      when {
        it.length > 95 -> """
          |help = $RAW_LIT_QUOTE
          |${it.wrapAndIndent(110, INDENT)}
          |$RAW_LIT_QUOTE
        """.trimMargin()

        it.contains("\n") -> "help = $RAW_LIT_QUOTE$it$RAW_LIT_QUOTE"

        else -> """help = "$it""""
      }
    }

  val typeHelp: String?
    get() = f.typeSpec
      ?.wrapAndIndent(100, "")
      ?.let {
        when {
          it.contains("\n") -> """
            |valueHelp = $RAW_LIT_QUOTE
            |${it.replaceIndent("  ")}
            |$RAW_LIT_QUOTE
          """.trimMargin()

          it.contains('"') -> "valueHelp = $RAW_LIT_QUOTE$it$RAW_LIT_QUOTE"

          else -> """valueHelp = "$it""""
        }
      }

  val optionListRegex = Regex("""^[_\w]+\s*(?:,\s*[_\w]+\s*)*or\s+[_\w]+\s*$""")

  val knownFlagType: String?
    get() = f.typeSpec?.run {
      when (this) {
        "a boolean" -> "Flag.Boolean"
        "an integer" -> "Flag.Integer"
        "a string" -> "Flag.Str"
        "a double" -> "Flag.Double"
        "a path" -> "Flag.Path"
        "a tri-state (auto, yes, no)" -> "Flag.TriState"
        "An immutable length of time." -> "Flag.Duration"
        "a build target label" -> "Flag.Label"
        else -> {
          optionListRegex.matchEntire(this)?.let {
            "Flag.OneOf"
          }
        }
      }
    }
}

