/*
 * Copyright 2017 HM Revenue & Customs
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

package infrastructure

import javax.inject.{Inject, Named}

import com.google.inject.Singleton
import org.joda.time.Duration
import reactivemongo.api.DB
import uk.gov.hmrc.lock.{LockKeeper, LockMongoRepository, LockRepository}

@Singleton
class Lock @Inject() (@Named("lockName") val name: String, @Named("lockTimeout") val timeout: Duration, val db: DB)extends LockKeeper{
  override def repo: LockRepository = LockMongoRepository(() => db)
  override def lockId: String = name
  override val forceLockReleaseAfter: Duration = timeout
}
