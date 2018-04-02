package com.virtuslab.billings.async

import com.virtuslab.BaseConfig

object Config extends BaseConfig {
  lazy val awsKey = conf.getString("aws.credentials.key")
  lazy val awsSecret = conf.getString("aws.credentials.secret")
}
