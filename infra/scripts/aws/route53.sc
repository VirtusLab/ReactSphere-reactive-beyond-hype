import java.util

import $file.elb
import $ivy.`com.amazonaws:aws-java-sdk-route53:1.11.307`
import ammonite.ops._
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder
import com.amazonaws.services.route53.model.{AliasTarget, _}
import elb.findMasterElb

import scala.collection.JavaConverters._

implicit val wd: Path = pwd

lazy val route53Client = AmazonRoute53ClientBuilder.defaultClient()
lazy val bthHostedZoneId = getBeyondTheHypeHostedZoneId

def getBeyondTheHypeHostedZoneId: String = {
  route53Client.listHostedZones()
    .getHostedZones.asScala.toIterator
    .find(!_.getConfig.getPrivateZone)
    .get.getId
}

def getHostedZoneById(id: String): HostedZone = {
  route53Client.getHostedZone(new GetHostedZoneRequest(id)).getHostedZone
}

def createSubdomain(subdomain: String): Unit = {
  val Right(masterElb) = findMasterElb()
  val masterElbUrl = masterElb.getDNSName
  val masterElbZone = masterElb.getCanonicalHostedZoneNameID

  val hostedZone = getHostedZoneById(bthHostedZoneId)
  val hostedZoneId = hostedZone.getId

  val alias = new AliasTarget(masterElbZone, masterElbUrl)
  alias.setEvaluateTargetHealth(true)

  val recordSet = new ResourceRecordSet(s"$subdomain.beyondthehype.pl", RRType.A)
  recordSet.setAliasTarget(alias)

  val change = new Change(ChangeAction.UPSERT, recordSet)
  val changes = new util.ArrayList[Change]()
  changes.add(change)

  val changeBatch = new ChangeBatch(changes)
  val request = new ChangeResourceRecordSetsRequest(hostedZoneId, changeBatch)

  route53Client.changeResourceRecordSets(request)
}