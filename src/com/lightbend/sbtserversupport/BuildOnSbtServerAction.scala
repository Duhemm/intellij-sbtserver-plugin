package com.lightbend.sbtserversupport

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.lightbend.intellijson.Command

class BuildOnSbtServerAction extends AnAction {

  @Override
  def actionPerformed(e: AnActionEvent): Unit = {
    val consoleComponent = e.getProject.getComponent(classOf[SbtServerConsole])
    val command = Command("compile")
    consoleComponent.tellSbtTo(command)
  }

}
