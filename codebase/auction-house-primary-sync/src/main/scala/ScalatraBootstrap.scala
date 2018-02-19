
import javax.servlet.ServletContext

import com.virtuslab.auctionHouse.sync.accounts.AccountsServlet
import com.virtuslab.auctionHouse.sync.auctions.AuctionsServlet
import com.virtuslab.auctionHouse.sync.signIn.SignInServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new AccountsServlet, "/accounts/*")
    context.mount(new SignInServlet, "/sign-in/*")
    context.mount(new AuctionsServlet, "/auctions/*")
  }
}

