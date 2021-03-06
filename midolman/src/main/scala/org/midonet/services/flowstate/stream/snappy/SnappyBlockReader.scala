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

package org.midonet.services.flowstate.stream.snappy

import org.xerial.snappy.{SnappyInputStream => XerialSnappyInputStream}

import org.midonet.util.Clearable
import org.midonet.util.io.stream.{TimedBlockHeader, ByteBufferBlockReader}

/**
  * Compressed input stream. Based on [[org.xerial.snappy.SnappyInputStream]]
  * but not reading any header (as our output stream does not write any).
  *
  * @param input underlying input stream.
  */
class SnappyBlockReader(val input: ByteBufferBlockReader[TimedBlockHeader] with Clearable)
    extends XerialSnappyInputStream(input) with Clearable {

    /**
      * As we don't have a header in our out stream, do nothing.
      */
    override def readHeader(): Unit = {}

    override def reset(): Unit = {
        input.reset()
    }

    /**
      * Clears any resource used by the underlying bytebuffer blok writer.
      */
    override def clear(): Unit = {
        input.clear()
    }

}
