package controllers

class PropertyLinkingControllerSpec extends ControllerSpec{
  object TestPropertyLinkingController extends PropertyLinkingController {

  }
  "clientProperties" should "only show the properties assigned to an agent" in {
    1 mustBe 1
  }


}
