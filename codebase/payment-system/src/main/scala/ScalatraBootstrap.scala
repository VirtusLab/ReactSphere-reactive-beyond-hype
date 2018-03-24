import javax.servlet.ServletContext

import com.virtuslab.base.sync.StatusServlet
import com.virtuslab.payment.SlowLegacyPaymentServlet
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  val apiVersion = "/api/v1"

  override def init(context: ServletContext) {
    context.mount(new StatusServlet, "/*")
    context.mount(new SlowLegacyPaymentServlet, s"$apiVersion/payment/*")
  }
}