import com.virtuslab.auctionHouse.sync.auctions.AuctionsServlet
import javax.servlet.ServletContext

import com.virtuslab.auctionHouse.sync.finalization.FinalizationServlet
import com.virtuslab.base.sync.StatusServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  val apiVersion = "/api/v1"

  override def init(context: ServletContext) {
    context.mount(new StatusServlet, "/*")
    context.mount(new AuctionsServlet, s"$apiVersion/auctions/*")
    context.mount(new FinalizationServlet, s"$apiVersion/finalize/*")
  }

}

