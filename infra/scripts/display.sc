import java.io.PrintStream

case class ProgressBar(val ps: PrintStream, namespace: String = "", startingTask: String = "Starting!") extends Thread {

  @volatile var showProgress = true
  @volatile var currentTask = startingTask

  private val formattedNamespace = if (namespace.length > 0) s" $namespace:" else ""

  override def run(): Unit = {
    val anim = "⢎⡰⢎⡡⢎⡑⢎⠱⠎⡱⢊⡱⢌⡱⢆⡱"
    var x = 0
    while (showProgress) {
      x += 2
      val pos = x % anim.length
      val char1 = anim.charAt(pos)
      val char2 = anim.charAt(pos + 1)
      val message = s"\r$char1$char2$formattedNamespace $currentTask"
      ps.print("\r" + Seq.fill(100)(" ").mkString)
      ps.print(message)
      ps.print("\r")

      try Thread.sleep(100) catch {
        case _: Exception =>
      }
    }
  }

  def finished(): Unit = {
    showProgress = false
    ps.print("\r" + Seq.fill(100)(" ").mkString)
    ps.print(s"\r ✓$formattedNamespace Done!\n")
  }

}