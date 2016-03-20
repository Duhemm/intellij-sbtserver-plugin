package sbt.intellij.sbtsupport

case class CommandExec(commandLine: String) {
  def serialize: String =
    s"""{ "type" : "command_exec", "command_line": "$commandLine" }"""
}
