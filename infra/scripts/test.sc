import ammonite.ops._
import $file.display
import display.ProgressBar

import scala.language.postfixOps

implicit val wd: Path = pwd
implicit val progressBar = ProgressBar(System.out, "Tectonic", "Reboot starting!")
progressBar.start()

def rebootTectonic(implicit wd: Path, progressBar: ProgressBar) {
  progressBar.currentTask = "Halting Vagrant VMs..."
  %vagrant "halt"
  progressBar.currentTask = "Starting Vagrant VMs..."
  %vagrant "up"

}

rebootTectonic(pwd/"tectonic", progressBar)

progressBar.finished()

//import display._
//val pb = new ProgressBar(System.out, "RealitySetUp", "Creating reality...")
//pb.start()
//Thread.sleep(2000)
//pb.currentTask = "Dividing matter and antimatter..."
//Thread.sleep(2000)
//pb.currentTask = "Merging atoms to create stars...."
//Thread.sleep(2000)
//pb.currentTask = "Fast-forwarding time..."
//Thread.sleep(1000)
//pb.finished()
//println("Reality set up complete!")
//Thread.sleep(1000)