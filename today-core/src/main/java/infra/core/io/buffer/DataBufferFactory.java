/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.io.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A factory for {@link DataBuffer DataBuffers}, allowing for allocation and
 * wrapping of data buffers.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DataBuffer
 * @since 4.0
 */
public abstract class DataBufferFactory {

  /**
   * Allocate a data buffer of a default initial capacity. Depending on the
   * underlying implementation and its configuration, this will be heap-based
   * or direct buffer.
   *
   * @return the allocated buffer
   */
  public abstract DataBuffer allocateBuffer();

  /**
   * Allocate a data buffer of the given initial capacity. Depending on the
   * underlying implementation and its configuration, this will be heap-based
   * or direct buffer.
   *
   * @param initialCapacity the initial capacity of the buffer to allocate
   * @return the allocated buffer
   */
  public abstract DataBuffer allocateBuffer(int initialCapacity);

  /**
   * Creates a new big-endian buffer whose content is the specified
   * {@code string} encoded in the specified {@code charset}.
   * The new buffer's {@code readerIndex} and {@code writerIndex} are
   * {@code 0} and the length of the encoded string respectively.
   *
   * @since 5.0
   */
  public DataBuffer copiedBuffer(CharSequence string) {
    return copiedBuffer(string, StandardCharsets.UTF_8);
  }

  /**
   * Creates a new big-endian buffer whose content is the specified
   * {@code string} encoded in the specified {@code charset}.
   * The new buffer's {@code readerIndex} and {@code writerIndex} are
   * {@code 0} and the length of the encoded string respectively.
   *
   * @since 5.0
   */
  public DataBuffer copiedBuffer(CharSequence string, Charset charset) {
    return wrap(string.toString().getBytes(charset));
  }

  /**
   * Wrap the given {@link ByteBuffer} in a {@code DataBuffer}. Unlike
   * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
   *
   * @param byteBuffer the NIO byte buffer to wrap
   * @return the wrapped buffer
   */
  public abstract DataBuffer wrap(ByteBuffer byteBuffer);

  /**
   * Wrap the given {@code byte} array in a {@code DataBuffer}. Unlike
   * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
   *
   * @param bytes the byte array to wrap
   * @return the wrapped buffer
   */
  public DataBuffer wrap(byte[] bytes) {
    return wrap(bytes, 0, bytes.length);
  }

  /**
   * Wrap the given {@code byte} array in a {@code DataBuffer}. Unlike
   * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
   *
   * @param bytes the byte array to wrap
   * @param offset The offset of the subarray to be used; must be non-negative and
   * no larger than {@code array.length}. The new buffer's position will be set to this value.
   * @param length The length of the subarray to be used;
   * must be non-negative and no larger than {@code array.length - offset}.
   * The new buffer's limit will be set to {@code offset + length}.
   * @return the wrapped buffer
   * @since 5.0
   */
  public abstract DataBuffer wrap(byte[] bytes, int offset, int length);

  /**
   * Return a new {@code DataBuffer} composed of the {@code dataBuffers} elements joined together.
   * Depending on the implementation, the returned buffer may be a single buffer containing all
   * data of the provided buffers, or it may be a true composite that contains references to the
   * buffers.
   * <p>Note that the given data buffers do <strong>not</strong> have to be released, as they are
   * released as part of the returned composite.
   *
   * @param dataBuffers the data buffers to be composed
   * @return a buffer that is composed of the {@code dataBuffers} argument
   */
  public abstract DataBuffer join(List<? extends DataBuffer> dataBuffers);

  /**
   * Return a new {@code DataBuffer} composed of the {@code dataBuffers} elements joined together.
   * Depending on the implementation, the returned buffer may be a single buffer containing all
   * data of the provided buffers, or it may be a true composite that contains references to the
   * buffers.
   * <p>Note that the given data buffers do <strong>not</strong> have to be released, as they are
   * released as part of the returned composite.
   *
   * @param dataBuffers the data buffers to be composed
   * @return a buffer that is composed of the {@code dataBuffers} argument
   * @since 5.0
   */
  public abstract DataBuffer join(DataBuffer... dataBuffers);

  /**
   * Indicates whether this factory allocates direct buffers (i.e. non-heap,
   * native memory).
   *
   * @return {@code true} if this factory allocates direct buffers;
   * {@code false} otherwise
   */
  public abstract boolean isDirect();

}
