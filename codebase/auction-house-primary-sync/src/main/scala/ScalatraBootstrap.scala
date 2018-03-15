
import com.virtuslab.auctionHouse.sync.StatusServlet
import com.virtuslab.auctionHouse.sync.accounts.AccountsServlet
import com.virtuslab.auctionHouse.sync.auctions.AuctionsServlet
import com.virtuslab.auctionHouse.sync.signIn.SignInServlet
import javax.servlet.ServletContext
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  val apiVersion = "/api/v1"

  override def init(context: ServletContext) {
    context.mount(new StatusServlet, "/*")
    context.mount(new AccountsServlet, s"$apiVersion/accounts/*")
    context.mount(new SignInServlet, s"$apiVersion/sign-in/*")
    context.mount(new AuctionsServlet, s"$apiVersion/auctions/*")
  }

}

