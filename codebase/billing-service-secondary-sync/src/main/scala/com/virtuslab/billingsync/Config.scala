package com.virtuslab.billingsync

import com.virtuslab.BaseConfig

object Config extends BaseConfig {
  lazy val awsKey = conf.getString("aws.credentials.key")
  lazy val awsSecret = conf.getString("aws.credentials.secret")
}
