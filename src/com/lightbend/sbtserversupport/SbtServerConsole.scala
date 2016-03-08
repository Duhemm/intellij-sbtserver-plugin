package com.lightbend.sbtserversupport

import java.io.{ OutputStream, IOException, InputStreamReader, BufferedReader, PrintWriter }
import java.net.Socket

import com.intellij.execution.ui.{ ConsoleViewContentType, ConsoleView }
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ ToolWindowAnchor, ToolWindowManager }
import com.lightbend.intellijson.Command

private class LogCollector(reader: BufferedReader, console: ConsoleView, component: SbtServerConsole) extends Thread {
  private var continue: Boolean = true

  def stopCollecting(): Unit = {
    reader.close()
    continue = false
  }

  override def run(): Unit = {
    try while (continue) {
      Option(reader.readLine()) match {
        case Some(line) => console.print(line + "\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        case None => continue = false
      }
    } catch {
      case ex: IOException => ()
    } finally component.closeCommunication()
  }
}

class SbtServerConsole(project: Project) extends ProjectComponent { self =>

  override val getComponentName: String = "com.lightbend.sbtserversupport.SbtServerConsole"
  val monitor: Object = ""

  final val CONSOLE_WINDOW_ID = "sbt server console"
  private var connectionOK: Boolean = false
  private var console: Option[ConsoleView] = None
  private var sbtSocket: Socket = _
  private var toSbt: OutputStream = _
  private var logCollector: LogCollector = _
  private val logger: Logger = Logger.getInstance(getComponentName)

  private class CommunicationEstablisher extends Thread {

    private def tryToConnect(): Unit = {
      try {
        sbtSocket = new Socket("localhost", 12700)
        toSbt = sbtSocket.getOutputStream()
        connectionOK = true
      } catch {
        case ex: IOException =>
          logger.warn("Connection to sbt-server failed. Retry in 5s.")
          Thread.sleep(5000)
          tryToConnect()
      }
    }

    override def run(): Unit = monitor.synchronized {
      while (!connectionOK) {
        tryToConnect()
      }

      console foreach { c =>
        logCollector = new LogCollector(new BufferedReader(new InputStreamReader(sbtSocket.getInputStream())), c, self)
        logCollector.start()
      }
      monitor.notifyAll()
    }
  }

  def tellSbtTo(command: Command): Unit = {
    toSbt.write((command.serialize + "\n").getBytes("UTF-8"))
    toSbt.flush()
  }

  override def initComponent(): Unit = ()

  override def disposeComponent(): Unit = ()

  override def projectOpened(): Unit = {

    val toolWindowManager: ToolWindowManager = ToolWindowManager.getInstance(project)
    toolWindowManager.registerToolWindow(CONSOLE_WINDOW_ID, false, ToolWindowAnchor.BOTTOM, project, true)

    Option(toolWindowManager.getToolWindow(CONSOLE_WINDOW_ID)) foreach { window =>

      SbtServerConsoleWindowFactory.createToolWindowContent(project, window)

      console = window.getComponent.getComponents.collectFirst {
        case console: ConsoleView =>
          console
      }

      if (!window.isActive) {
        window.activate(null, true)
      }

      connectToServer()

    }

  }

  override def projectClosed(): Unit = {
    closeCommunication()
  }

  def connectToServer(): Unit = {
    if (!connectionOK)
      new CommunicationEstablisher().start()

    monitor.notifyAll()
  }

  def closeCommunication(): Unit = {
    if (connectionOK) {
      connectionOK = false
      logCollector.stopCollecting()
      logCollector.interrupt()
      toSbt.close()
      sbtSocket.close()
      monitor.notifyAll()
    }
  }

  def connected(): Boolean = connectionOK

}
