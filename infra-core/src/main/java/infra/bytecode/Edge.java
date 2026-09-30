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

package infra.bytecode;

import org.jspecify.annotations.Nullable;

/**
 * An edge in the control flow graph of a method. Each node of this graph is a basic block,
 * represented with the Label corresponding to its first instruction. Each edge goes from one node
 * to another, i.e. from one basic block to another (called the predecessor and successor blocks,
 * respectively). An edge corresponds either to a jump or ret instruction. Exception handlers are
 * not represented with explicit edges.
 *
 * @author Eric Bruneton
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Label
 */
final class Edge {

  /**
   * The stack size at the end of the predecessor block, minus the stack size at its beginning. This
   * field is only used with {@link ClassWriter#COMPUTE_MAXS}.
   */
  public final int stackSizeDelta;

  /** The successor block of this control flow graph edge. */
  public final Label successor;

  /**
   * The next edge in the list of outgoing edges of a basic block. See {@link Label#outgoingEdges}.
   */
  public @Nullable Edge nextEdge;

  /**
   * Constructs a new Edge.
   *
   * @param stackSizeDelta see {@link #stackSizeDelta}.
   * @param successor see {@link #successor}.
   * @param nextEdge see {@link #nextEdge}.
   */
  Edge(final int stackSizeDelta, final Label successor, final @Nullable Edge nextEdge) {
    this.stackSizeDelta = stackSizeDelta;
    this.successor = successor;
    this.nextEdge = nextEdge;
  }
}
