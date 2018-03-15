import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object JettyLauncher {
  def main(args: Array[String]) {
    val port = Option(System.getProperty("http.port")).map(_.toInt).getOrElse(8080)
    DefaultExports.initialize()
    val metricsServer = new HTTPServer(8081)

    val server = new Server(port)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")

    context.setEventListeners(Array(new ScalatraListener))

    server.setHandler(context)

    server.start()
    server.join()
    metricsServer.stop()
  }
}
