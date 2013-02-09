/*
 * Copyright 2012 Toshiyuki Takahashi
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

package playArgonaut

import org.specs2.mutable._

import play.api._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scalaz._, Scalaz._
import argonaut._, Argonaut._

case class Person(id: Long, name: String, age: Int)

object TestApplication extends Controller with PlayArgonaut{

  implicit val DecodePerson: DecodeJson[Person] =
    jdecode3L((id: Double, name: String, age: Double) => Person(id.toLong, name, age.toInt))("id", "name", "age")
//    jdecode3L(Person(_: Long, _: String, _: Int))("id", "name", "age")

  implicit val EncodePerson: EncodeJson[Person] =
    jencode3L((p: Person) => (p.id: Double, p.name, p.age: Double))("id", "name", "age")
//    jencode3L((p: Person) => (p.id, p.name, p.age))("id", "name", "age")

  def get = Action { implicit request =>
    Ok(Person(1, "ぱみゅぱみゅ", 20).jencode)
  }

  def post = Action(argonaut) { implicit request =>
    request.body.jdecode[Person].fold(
      {case (msg, history) => BadRequest((msg,history.show).toString)},
      p => Ok(p.name)
    )
  }

}


class PlayArgonautSpec extends Specification with PlayArgonaut {
  diffs(show=false)

  val testJson = """{"id":1.0,"name":"ぱみゅぱみゅ","age":20.0}"""
//  val testJson = """{"id":1,"name":"ぱみゅぱみゅ","age":20}"""

  "PlayArgonaut" should {

    "allow you to use argonaut json value as response" in {
      val res = TestApplication.get(FakeRequest("GET", ""))
      status(res) must_== OK
      contentType(res) must beEqualTo (Some("application/json"))
      contentAsString(res) must beEqualTo (testJson)
    }

    "accept argonaut json request" in {
      val header = FakeHeaders(Seq(("Content-Type" -> Seq("application/json"))))
      val res = TestApplication.post(FakeRequest("POST", "", header, JsonParser.parse(testJson).getOrElse(sys.error("parse fail"))))
      status(res) must_== OK
      contentAsString(res) must beEqualTo ("ぱみゅぱみゅ")
    }

  }

}

