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

import argonaut.{Json, JsonParser}
import play.api._
import play.api.http._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.language.reflectiveCalls

trait ArgonautWriteable { self: ArgonautContentTypeOf =>

  implicit def writeableOf_Json(implicit codec: Codec): Writeable[Json] = {
    Writeable((jval: Json) => codec.encode(jval.nospaces))
  }

}

trait ArgonautContentTypeOf { self: ArgonautWriteable =>

  implicit def contentTypeOf_JsValue(implicit codec: Codec): ContentTypeOf[Json] = {
    ContentTypeOf(Some(ContentTypes.JSON))
  }

}

trait ArgonautParser {

  def tolerantJson(maxLength: Int): BodyParser[Json] = BodyParser("json, maxLength=" + maxLength) { request =>
    play.api.libs.iteratee.Traversable.takeUpTo[Array[Byte]](maxLength).apply(Iteratee.consume[Array[Byte]]().map { bytes =>
      JsonParser.parse(new String(bytes, request.charset.getOrElse("utf-8"))).toEither.left.map{
        e =>
        (Play.maybeApplication.map(_.global.onBadRequest(request, s"Invalid Json $e")).getOrElse(Results.BadRequest), bytes)
      }
    }).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
    .flatMap {
      case Left(b) => Done(Left(b), Input.Empty)
      case Right(it) => it.flatMap {
        case Left((r, in)) => Done(Left(r), Input.El(in))
        case Right(json) => Done(Right(json), Input.Empty)
      }
    }
  }

  def tolerantJson: BodyParser[Json] = tolerantJson(BodyParsers.parse.DEFAULT_MAX_TEXT_LENGTH)

  def acceptTypes = Set("text/json", "application/json")

  def argonaut(maxLength: Int): BodyParser[Json] = BodyParsers.parse.when(
    _.contentType.exists(acceptTypes.contains),
    tolerantJson(maxLength),
    request => Play.maybeApplication.map(_.global.onBadRequest(request, "Expecting text/json or application/json body")).getOrElse(Results.BadRequest)
  )

  def argonaut: BodyParser[Json] = argonaut(BodyParsers.parse.DEFAULT_MAX_TEXT_LENGTH)

}

trait PlayArgonaut extends ArgonautParser with ArgonautWriteable with ArgonautContentTypeOf


