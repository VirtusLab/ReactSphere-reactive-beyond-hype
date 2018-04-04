import $ivy.`com.amazonaws:aws-java-sdk-elasticloadbalancing:1.11.307`
import ammonite.ops._
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription

import scala.collection.JavaConverters._

implicit val wd: Path = pwd

lazy val elbClient = AmazonElasticLoadBalancingClientBuilder.defaultClient()

case object MasterElbNotFound extends RuntimeException("Master ELB was not found, check ELB list on AWS!")

def findMasterElb(): Either[Throwable, LoadBalancerDescription] = {
  elbClient.describeLoadBalancers().getLoadBalancerDescriptions.asScala
    .find(_.getLoadBalancerName.endsWith("con"))
    .toRight(MasterElbNotFound)
}


