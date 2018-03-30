import javax.servlet.ServletContext

import com.virtuslab.base.sync.StatusServlet
import com.virtuslab.billingsync.BillingServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  val apiVersion = "/api/v1"

  override def init(context: ServletContext) {
    context.mount(new StatusServlet, "/*")
    context.mount(new BillingServlet, s"$apiVersion/billing/*")
  }

}

