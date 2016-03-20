package sbt.intellij.sbtsupport

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class BuildOnSbtServerAction extends AnAction {
  @Override
  def actionPerformed(e: AnActionEvent): Unit = {
    val consoleComponent = e.getProject.getComponent(classOf[SbtServerConsole])
    val command = CommandExec("compile")
    consoleComponent.tellSbtTo(command)
  }
}
