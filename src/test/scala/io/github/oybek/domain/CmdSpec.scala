package io.github.oybek.domain

import java.io.File

import org.scalatest.FunSuite

class CmdSpec extends FunSuite {

  test("Convert cmd to expr") {
    assert(
      CmdStartCSDS(new File("/tmp/"))("cs_mansion", 27015).expr ==
        "./hlds_run -game cstrike +ip 0.0.0.0 +port 27015 +maxplayers 12 +map cs_mansion"
    )
  }
}
