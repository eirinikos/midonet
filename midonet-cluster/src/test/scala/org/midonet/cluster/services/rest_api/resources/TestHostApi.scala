/*
 * Copyright 2016 Midokura SARL
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
package org.midonet.cluster.services.rest_api.resources

import org.junit.Test

import org.midonet.cluster.rest_api.rest_api._
import org.midonet.cluster.services.rest_api.RestApiScalaTestBase

class TestHostApi extends RestApiScalaTestBase(FuncTest.getBuilder.build()) {

    @Test
    def testCreateWithDuplicatedId(): Unit = {
        val h1 = create(makeHost())
        val h2 = makeHost(id = h1.id)

        // This should return HttpConflict
        postAndAssertConflict(h2, app.getHosts)
    }
}
