import com.virtuslab.auctionHouse.identitySync.accounts.AccountsServlet
import com.virtuslab.auctionHouse.identitySync.signIn.SignInServlet
import com.virtuslab.auctionHouse.identitySync.validation.ValidationServlet
import com.virtuslab.base.sync.StatusServlet
import javax.servlet.ServletContext
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  val apiVersion = "/api/v1"

  override def init(context: ServletContext) {
    context.mount(new StatusServlet, "/*")
    context.mount(new AccountsServlet, s"$apiVersion/accounts/*")
    context.mount(new SignInServlet, s"$apiVersion/sign-in/*")
    context.mount(new ValidationServlet, s"$apiVersion/validate/*")
  }

}

