package com.lightbend.sbtserversupport

import java.awt.{ GridLayout, BorderLayout }
import java.awt.event.{ ActionEvent, ActionListener }
import javax.swing.{ JPanel, JButton }

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.actionSystem.{ DataContext, AnActionEvent, ActionManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ ToolWindow, ToolWindowFactory }

object SbtServerConsoleWindowFactory extends ToolWindowFactory {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {

    val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole
    val consoleComponent = project.getComponent(classOf[SbtServerConsole])

    val rootContainer = toolWindow.getComponent
    val buildButton = new JButton("Build on sbt server")
    buildButton.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        val action = ActionManager.getInstance().getAction("BuildOnSbtServer")
        val dataContext = new DataContext {
          override def getData(s: String): AnyRef = s match {
            case "project" => project
          }
        }
        val event = AnActionEvent.createFromAnAction(action, null, consoleComponent.getComponentName, dataContext)
        ActionManager.getInstance().getAction("BuildOnSbtServer").actionPerformed(event)
      }
    })
    val cleanButton = new JButton("Clean on sbt server")
    cleanButton.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        val action = ActionManager.getInstance().getAction("CleanOnSbtServer")
        val dataContext = new DataContext {
          override def getData(s: String): AnyRef = s match {
            case "project" => project
          }
        }
        val event = AnActionEvent.createFromAnAction(action, null, consoleComponent.getComponentName, dataContext)
        ActionManager.getInstance().getAction("CleanOnSbtServer").actionPerformed(event)
      }
    })
    val connectButton = new JButton("Connect to server")

    toolWindow.setStripeTitle(consoleComponent.CONSOLE_WINDOW_ID)
    toolWindow.setTitle(consoleComponent.CONSOLE_WINDOW_ID)
    val buttonsContainer = new JPanel()
    buttonsContainer.setLayout(new GridLayout(1, 0))
    buttonsContainer.add(buildButton)
    buttonsContainer.add(cleanButton)
    buttonsContainer.add(connectButton)
    rootContainer.add(buttonsContainer, BorderLayout.NORTH)
    rootContainer.add(console.getComponent, BorderLayout.CENTER)

    val connectionMonitor = new ConnectionMonitor(connectButton, consoleComponent)
    connectionMonitor.start()
  }
}

private class ConnectionMonitor(connectButton: JButton, component: SbtServerConsole) extends Thread {

  private val connectedLabel = "Disconnect from server"
  private val disconnectedLabel = "Connect to server"
  private val connectActionListener: ActionListener = new ActionListener {
    override def actionPerformed(e: ActionEvent): Unit = {
      connectButton.setEnabled(false)
      connectButton.removeActionListener(connectActionListener)
      connectButton.addActionListener(disconnectActionListener)
      component.connectToServer()
    }
  }

  private val disconnectActionListener: ActionListener = new ActionListener {
    override def actionPerformed(e: ActionEvent): Unit = {
      connectButton.setEnabled(true)
      connectButton.setText(disconnectedLabel)
      connectButton.removeActionListener(disconnectActionListener)
      connectButton.addActionListener(connectActionListener)
      component.closeCommunication()
    }
  }

  override def run(): Unit = component.monitor.synchronized {
    while (true) {
      if (component.connected()) {
        connectButton.setText(connectedLabel)
        connectButton.removeActionListener(connectActionListener)
        connectButton.addActionListener(disconnectActionListener)
      } else {
        connectButton.setText(disconnectedLabel)
        connectButton.removeActionListener(disconnectActionListener)
        connectButton.addActionListener(connectActionListener)
      }

      component.monitor.wait()

    }
  }
}
