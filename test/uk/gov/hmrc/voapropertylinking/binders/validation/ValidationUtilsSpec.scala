/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.voapropertylinking.binders
package validation

import basespecs.BaseUnitSpec
import binders.Params
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}

class ValidationUtilsSpec extends BaseUnitSpec {

  object Colour extends Enumeration {
    type TestEnum = Value
    val RED = Value("red")
    val GREEN = Value("green")
    val MIXED_SHADE = Value("mixedShade")
  }

  trait Setup extends ValidationUtils

  "validating a string is mappable to an enum" should {
    "consider all valid enum values as valid, case-sensitive" in new Setup {
      val validValues = Seq("red", "mixedShade", "green")
      forAll(validValues) { v =>
        enumValue(Colour)(v)("colour") shouldBe Valid(Colour.withName(v))
      }
    }

    "reject values that are not mappable to the enum" in new Setup {
      val invalidValues = Seq("blue", "greenish", "mixed_shade", "")
      forAll(invalidValues) { v =>
        inside(enumValue(Colour)(v)("colour")) {
          case Invalid(errors) =>
            val e = errors.head
            e.value shouldBe v
            e.key shouldBe "colour"
            e.acceptableValues should contain theSameElementsAs Seq("red", "green", "mixedShade")
        }
      }
    }
  }

  "validating a missing mandatory component" should {
    "pass validation" in new Setup {
      val params: Params = Map("mandatory" -> Seq("something"))
      read("mandatory", params) shouldBe Valid("something")
    }
    "fail validation" in new Setup {
      val params: Params = Map("somethingElse" -> Seq("foo"))
      inside(read("mandatory", params)) {
        case Invalid(errors) =>
          errors.head.key shouldBe "mandatory"
      }
    }
  }

  "validating a missing optional component" should {
    "be always valid" when {
      "the component is present" in new Setup {
        val params: Params = Map("optional" -> Seq("something"))
        readWithDefault("default")("optional", params) shouldBe Valid("something")
      }
      "the component is missing but we can provide a default value" in new Setup {
        val params: Params = Map()
        readWithDefault("default")("optional", params) shouldBe Valid("default")
      }
      "the component is missing but we are mapping to an Option" in new Setup {
        val params: Params = Map()
        readOption("optional", params) shouldBe Valid(Option.empty[String])
      }
    }
  }


  "validating an optional component" should {
    "be valid" when {
      "the component is present and considered VALID" in new Setup {
        implicit val key: String = "optionalLong"
        implicit val params: Params = Map("optionalLong" -> Seq("123"))
        (readOption ifPresent asLong) shouldBe Valid(Some(123L))
        (readOption ifPresent asInt) shouldBe Valid(Some(123))
      }

      "the component is missing" in new Setup {
        implicit val key: String = "optionalLong"
        implicit val params: Params = Map()
        (readOption ifPresent asLong) shouldBe Valid(None)
      }
    }

    "be invalid" when {
      "the component is provided but considered INVALID" in new Setup {
        implicit val key: String = "optionalLong"
        implicit val params: Params = Map("optionalLong" -> Seq("foobar"))
        (readOption ifPresent asLong) should not be 'valid
      }
    }
  }

  "validating a mandatory boolean parameter" should {
    "accept boolean value encoded as 'true' or 'false'" in new Setup {
      implicit val key: String = "boolean"
      read("boolean", Map("boolean" -> Seq("true"))) andThen asBoolean shouldBe Valid(true)
      read("boolean", Map("boolean" -> Seq("false"))) andThen asBoolean shouldBe Valid(false)
    }
    "reject boolean value if it's neither 'true' nor 'false'" in new Setup {
      implicit val key: String = "boolean"
      read("boolean", Map("boolean" -> Seq(""))) andThen asBoolean shouldBe a[Invalid[_]]
      read("boolean", Map("boolean" -> Seq("something else"))) andThen asBoolean shouldBe a[Invalid[_]]
      read("boolean", Map("boolean" -> Seq("0"))) andThen asBoolean shouldBe a[Invalid[_]]
    }
  }

  "validating a LocalDate" should {
    "accept valid local date" when {
      "valid string of yyyy-mm-dd format is provided" in new Setup {
        implicit val params: Params = Map("date" -> Seq("2019-09-17"))
        implicit val key: String = "date"
        read andThen asLocalDate shouldBe 'valid
      }
    }

    "reject invalid local date" when {
      "string of yyyy-mm-dd format is provided but date is nonsense" in new Setup {
        implicit val params: Params = Map("date" -> Seq("2019-50-60"))
        implicit val key: String = "date"
        inside(read andThen asLocalDate) {
          case Invalid(NonEmptyList(InvalidTypeError(k, clazz), _)) =>
            k shouldBe key
            clazz.getSimpleName shouldBe "LocalDate"
        }
      }
    }
  }

  "validating non-blank strings" should {
    "accept a non-blank string" in new Setup {
      nonBlankString("foobar")("key") shouldBe 'valid
    }
    "reject a string containing only blanks" in new Setup {
      inside(nonBlankString("    ")("key")) {
        case Invalid(NonEmptyList(BlankQueryParameterError(k), _)) =>
          k shouldBe "key"
      }
    }
    "reject an empty string" in new Setup {
      inside(nonBlankString("")("key")) {
        case Invalid(NonEmptyList(BlankQueryParameterError(k), _)) =>
          k shouldBe "key"
      }
    }
  }

  "validating min/max string length" should {
    "accept strings within valid range" in new Setup {
      val s = "foo"
      implicit val key: String = "key"
      minLength(1)(s) andThen maxLength(5) shouldBe 'valid
    }

    "reject strings outside valid range" in new Setup {
      val s = "foo"
      implicit val key: String = "key"
      inside(minLength(4)(s) andThen maxLength(5)) {
        case Invalid(NonEmptyList(UnderMinLengthError(k, limit), _)) =>
          k shouldBe key
          limit shouldBe 4
      }

      inside(minLength(1)(s) andThen maxLength(2)) {
        case Invalid(NonEmptyList(OverMaxLengthError(k, limit), _)) =>
          k shouldBe key
          limit shouldBe 2
      }
    }
  }

  "validating the range of numeric values" should {

    trait IntRangeTest extends ValidationUtils {
      implicit val key: String = "number"
      val min = 1
      val max = 10

      def rangeTest(value: String): Validated[NonEmptyList[ValidationError], Int] =
        read(key, Map(key -> Seq(value))) andThen asInt andThen min(min) andThen max(max)
    }

    "accept values within valid range" in new IntRangeTest {
      val values: Seq[String] = (-9 to 20).map(n => n.toString)
      values.map(rangeTest).flatMap(_.toOption) should contain theSameElementsInOrderAs (min to max)
    }
    "work as expected around boundaries" in new IntRangeTest {
      rangeTest(min.toString) shouldBe Valid(min)
      inside(rangeTest((min - 1).toString)) {
        case Invalid(errors) => errors.head shouldBe UnderLimitError(key, min)(min - 1)
      }

      rangeTest(max.toString) shouldBe Valid(max)
      inside(rangeTest((max + 1).toString)) {
        case Invalid(errors) => errors.head shouldBe OverLimitError(key, max)(max + 1)
      }
    }
  }

  "validating that a value matches a regular expression" should {
    trait RegExpTest extends ValidationUtils {
      implicit val key: String = "fieldName"
      val pattern = """^([A-Za-z]{2}\d{1,2})$""".r

      def regExpTest(value: String): Validated[NonEmptyList[ValidationError], String] =
        read(key, Map(key -> Seq(value))) andThen regex(pattern)
    }

    "accept values that match the regular expression" in new RegExpTest {
      val values: Seq[String] = Seq("AA1", "bb1", "Ca12", "dD23")

      values foreach { value =>
        regExpTest(value) shouldBe Valid(value)
      }
    }

    "reject values that do not match the regular expression" in new RegExpTest {
      val values: Seq[String] = Seq("A1", "A1", "bb", "Ca123", "dD 23", " dD23", "dD23 ")

      values foreach { value =>
        inside(regExpTest(value)) {
          case Invalid(errors) => errors.head shouldBe InvalidFormat(key)
        }
      }
    }
  }

  "validating that a postcode has the correct format" should {
    trait PostcodeTest extends ValidationUtils {
      implicit val key: String = "postcode"

      def postcodeTest(value: String): Validated[NonEmptyList[ValidationError], String] =
        read(key, Map(key -> Seq(value))) andThen validPostcode
    }

    "accept valid postcodes" in new PostcodeTest {
      val outwardCodeOnly: Seq[String] = Seq("N1", "N12", "WC1", "WC1", "SE16", "WC1A")
      val fullPostcodes: Seq[String] = Seq("N1 1AA", "N12 1AA", "WC1 1AA", "WC1 1AA", "SE16 1AA", "WC1A 1AA")
      val mixedCasePostcodes: Seq[String] = Seq("n1 1Aa", "n12 1aa", "wc1 1aA", "Wc1 1Aa", "se16 1AA", "WC1A 1aa")

      outwardCodeOnly ++ fullPostcodes ++ mixedCasePostcodes foreach { value =>
        postcodeTest(value) shouldBe Valid(value)
      }
    }

    "reject invalid postcodes" in new PostcodeTest {
      val badOutwardCodes: Seq[String] = Seq("N", "2", "WC", "ABC", "A123", "WC123")
      val badStartAndEndValues: Seq[String] = Seq(" N1", "N1 ")
      val incorrectSpaceValues: Seq[String] = Seq("N11AA", "Se136sD", "N1  2AA")
      val badInwardCodes: Seq[String] = Seq("N1 A1A", "N1 12A", "N1 AAA", "N1 1A", "N1 1AAA", "N1 12AA")

      badOutwardCodes ++ badStartAndEndValues ++ incorrectSpaceValues ++ badInwardCodes foreach { value =>
        inside(postcodeTest(value)) {
          case Invalid(errors) => errors.head shouldBe InvalidFormat(key)
        }
      }
    }
  }

  "validating that a property link submission id has the correct format" should {
    trait PropertyLinkSubmissionIdTest extends ValidationUtils {
      implicit val key: String = "propertyLinkSubmissionId"

      def propertyLinkIdTest(value: String): Validated[NonEmptyList[ValidationError], String] =
        read(key, Map(key -> Seq(value))) andThen validPropertyLinkSubmissionId
    }

    "accept valid property link submission ids" in new PropertyLinkSubmissionIdTest {
      val valid: Seq[String] = Seq("PL1", "PL123", "ABCA3S5D", "ACCEPT45CHARACTERS-ACCEPT45CHARACTERS-1234567")

      valid foreach { value =>
        propertyLinkIdTest(value) shouldBe Valid(value)
      }
    }

    "reject invalid property link submission ids" in new PropertyLinkSubmissionIdTest {
      val invalid: Seq[String] =
        Seq("P", ".P", "P P", ".", "..", "FAILON46CHARACTERS_FAILON46CHARACTERS_12345678", "INVALID%PROPERTY%%")

      invalid foreach { value =>
        inside(propertyLinkIdTest(value)) {
          case Invalid(errors) => errors.head shouldBe InvalidFormat(key)
        }
      }
    }
  }
}
