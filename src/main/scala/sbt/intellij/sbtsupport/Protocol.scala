package sbt.intellij.sbtsupport

case class CommandExec(commandLine: String) {
  def serialize: String =
    s"""{ "type" : "exec", "command_line": "$commandLine" }"""
}
