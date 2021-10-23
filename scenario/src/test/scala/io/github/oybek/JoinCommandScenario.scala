package io.github.oybek

import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

class JoinCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to join dedicated counter strike server")

  Feature("/join command") {
    Scenario("User gives command '/join'") {
    }
  }
}
