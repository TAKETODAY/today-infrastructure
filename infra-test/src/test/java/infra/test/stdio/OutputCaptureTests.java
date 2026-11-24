/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.stdio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.PrintStream;

import infra.core.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@ResourceLock("SYSTEM_OUT")
@ResourceLock("SYSTEM_ERR")
class OutputCaptureTests {

  @Test
  void requiresAnActiveSession() {
    OutputCapture capture = new OutputCapture();
    assertThatIllegalStateException().isThrownBy(capture::getAll)
            .withMessageContaining("No system captures found");
    assertThatIllegalStateException().isThrownBy(capture::getOut);
    assertThatIllegalStateException().isThrownBy(capture::getErr);
  }

  @Test
  void capturesStreamsInOrderAndInvalidatesCachedContent() {
    OutputCapture capture = new OutputCapture();
    PrintStream out = System.out;
    PrintStream err = System.err;
    capture.push();
    try {
      assertThat(capture.getAll()).isEmpty();
      System.out.print("one");
      assertThat(capture.getOut()).isEqualTo("one");
      assertThat(capture.getOut()).isEqualTo("one");
      System.err.print("two");
      System.out.write('!');
      System.out.flush();
      System.err.flush();
      assertThat(capture.getAll()).isEqualTo("onetwo!");
      assertThat(capture.getOut()).isEqualTo("one!");
      assertThat(capture.getErr()).isEqualTo("two");
      assertThat(capture.toString()).isEqualTo("onetwo!");
      assertThat(capture.length()).isEqualTo(7);
      assertThat(capture.charAt(3)).isEqualTo('t');
      assertThat(capture.subSequence(3, 6).toString()).isEqualTo("two");
      assertThat(capture.equals(capture)).isTrue();
      assertThat(capture.equals("onetwo!")).isTrue();
      assertThat(capture.equals(new StringBuilder("onetwo!"))).isTrue();
      assertThat(capture.equals("different")).isFalse();
      assertThat(capture.equals(null)).isFalse();
      assertThat(capture.equals(new Object())).isFalse();
      assertThat(capture.hashCode()).isEqualTo("onetwo!".hashCode());
      capture.reset();
      assertThat(capture.getAll()).isEmpty();
      assertThat(capture.getOut()).isEmpty();
      assertThat(capture.getErr()).isEmpty();
      System.err.print("after reset");
      assertThat(capture.getAll()).isEqualTo("after reset");
    }
    finally {
      capture.pop();
    }
    assertThat(System.out).isSameAs(out);
    assertThat(System.err).isSameAs(err);
  }

  @Test
  void nestedSessionsRestoreStreamsAndAnsiState() {
    OutputCapture capture = new OutputCapture();
    AnsiOutput.Enabled saved = AnsiOutput.getEnabled();
    AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    try {
      capture.push();
      try {
        assertThat(AnsiOutput.getEnabled()).isEqualTo(AnsiOutput.Enabled.NEVER);
        System.out.print("outer");
        PrintStream outer = System.out;
        capture.push();
        try {
          System.err.print("inner");
          assertThat(capture.getAll()).isEqualTo("outerinner");
          assertThat(capture.getOut()).isEqualTo("outer");
          assertThat(capture.getErr()).isEqualTo("inner");
        }
        finally {
          capture.pop();
        }
        assertThat(System.out).isSameAs(outer);
        assertThat(AnsiOutput.getEnabled()).isEqualTo(AnsiOutput.Enabled.NEVER);
        assertThat(capture.getAll()).isEqualTo("outer");
        System.out.print("again");
        assertThat(capture.getAll()).isEqualTo("outeragain");
      }
      finally {
        capture.pop();
      }
      assertThat(AnsiOutput.getEnabled()).isEqualTo(AnsiOutput.Enabled.ALWAYS);
    }
    finally {
      AnsiOutput.setEnabled(saved);
    }
  }

}
