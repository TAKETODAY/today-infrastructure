// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode.tree.analysis;

import infra.bytecode.LimitExceededException;

/**
 * Memory and time limits for potentially costly algorithms.
 *
 * @author Eric Bruneton
 */
final class ComputeLimits {

  /** Error message when the memory limit is exceeded. */
  private static final String TOO_MANY_ALLOCATED_BYTES = "Too many allocated bytes";

  /** The size in bytes of the header of an object (class pointer, etc). */
  static final int OBJECT_HEADER_BYTES = 8;

  /** The size in bytes of the header of an array (class pointer, array length, etc). */
  static final int ARRAY_HEADER_BYTES = 16;

  /** The remaining number of bytes which can be allocated by the algorithm using these limits. */
  private int remainingBytes;

  /** The remaining number of "operations" which can be done by the algorithm using these limits. */
  private long remainingOperations;

  /**
   * Constructs new compute limits.
   *
   * @param maxBytes the maximum number of bytes which can be allocated.
   * @param maxOperations the maximum number of "operations" which can be done. The definition of an
   * operation depends on the algorithms using these limits. The number of operations done by an
   * algorithm should be approximatively proportional to its runtime (and deterministic).
   */
  ComputeLimits(final int maxBytes, final long maxOperations) {
    this.remainingBytes = maxBytes;
    this.remainingOperations = maxOperations;
  }

  /**
   * Constructs a copy of the given compute limits.
   *
   * @param limits the limits to copy.
   */
  ComputeLimits(final ComputeLimits limits) {
    this(limits.remainingBytes, limits.remainingOperations);
  }

  /**
   * Creates a new boolean array if this does not exceed the memory limits.
   *
   * @param length the array length (number of elements).
   * @return the newly created array.
   * @throws LimitExceededException if the memory limit is exceeded.
   */
  boolean[] checkNewBooleanArray(final int length) {
    int numBytes = ARRAY_HEADER_BYTES + length;
    if (numBytes < 0 || numBytes > remainingBytes) {
      throw new LimitExceededException(TOO_MANY_ALLOCATED_BYTES);
    }
    remainingBytes -= numBytes;
    return new boolean[length];
  }

  /**
   * Creates a new object array if this does not exceed the memory limits.
   *
   * @param length the array length (number of elements).
   * @return the newly created array.
   * @throws LimitExceededException if the memory limit is exceeded.
   */
  Object[] checkNewObjectArray(final int length) {
    int numBytes = ARRAY_HEADER_BYTES + length * 4;
    if (numBytes < 0 || numBytes > remainingBytes) {
      throw new LimitExceededException(TOO_MANY_ALLOCATED_BYTES);
    }
    remainingBytes -= numBytes;
    return new Object[length];
  }

  /**
   * Checks if some bytes can be allocated without exceeding the limit.
   *
   * @param numBytes the number of bytes to allocate.
   * @throws LimitExceededException if the memory limit is exceeded.
   */
  void checkNewBytes(final int numBytes) {
    if (numBytes < 0 || numBytes > remainingBytes) {
      throw new LimitExceededException(TOO_MANY_ALLOCATED_BYTES);
    }
    remainingBytes -= numBytes;
  }

  /**
   * Checks if some operations can be done without exceeding the limit.
   *
   * @param numOperations the number of operations to perform.
   * @throws LimitExceededException if the operations limit is exceeded.
   */
  void checkNewOperations(final long numOperations) {
    if (numOperations > remainingOperations) {
      throw new LimitExceededException("Too many operations");
    }
    remainingOperations -= numOperations;
  }
}
