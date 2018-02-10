import java.io.PrintStream

case class ProgressBar(private val ps: PrintStream,
                       private val namespace: String = "",
                       private val startingTask: String = "Starting!") extends Thread {

  @volatile private var showProgress = true
  @volatile private var currentTask = startingTask
  @volatile private var currentNamespace = namespace

  private def formattedNamespace = if (currentNamespace.length > 0) s" $currentNamespace:" else ""

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

  def stepInto(namespace: String): Unit = {
    currentNamespace = namespace
  }

  def show(task: String): Unit = {
    currentTask = task
  }

  def finishedNamespace(): Unit = {
    ps.print("\r" + Seq.fill(100)(" ").mkString)
    ps.print(s"\r ✓$formattedNamespace Done!\n")
  }

  def finished(): Unit = {
    showProgress = false
  }

  def failed(): Unit = {
    showProgress = false
    ps.print("\r" + Seq.fill(100)(" ").mkString)
    ps.print(s"\r x$formattedNamespace Failed!\n")
  }

}