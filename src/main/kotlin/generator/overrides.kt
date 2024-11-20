package generator

val snakeCasePattern = "_+[a-z]".toRegex()
fun String.snakeToCamelCase(): String {
  return replace(snakeCasePattern) { it.value.last().uppercase() }.replace(":", "_")
}


fun String.wrapAndIndent(wrapAt: Int, indent: String = "", unwrappedIndent: String = indent): String {
  val lines = this
    .lineSequence()
    .flatMap {
      it.splitToSequence(Regex("""(?<=\s)(?=\S)"""))
        .fold(Pair("", listOf<String>())) { (line, lines), st ->
          when (wrapAt - line.length) {
            in st.length..Int.MAX_VALUE -> Pair(line + st, lines)
            else -> Pair(st, lines.plusElement(line.trimEnd()))
          }
        }.let { (line, lines) -> lines.plusElement(line.trimEnd()) }
    }.toList()

  return when (lines.size) {
    1 -> lines.first().prependIndent(unwrappedIndent)
    else -> lines.joinToString("\n").prependIndent(indent)
  }
}
