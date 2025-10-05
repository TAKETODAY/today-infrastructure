/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.mail;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 13:38
 */
class ExceptionTests {

  @Nested
  class MailSendExceptionTests {

    @Test
    void exceptionWithoutFailedMessagesReturnsEmptyExceptionArray() {
      var exception = new MailSendException("msg", new RuntimeException());
      assertThat(exception.getMessageExceptions()).isEmpty();
    }

    @Test
    void exceptionWithFailedMessagesContainsAllExceptions() {
      var failedMessages = new LinkedHashMap<Object, Exception>();
      var ex1 = new RuntimeException("failed1");
      var ex2 = new RuntimeException("failed2");
      failedMessages.put("msg1", ex1);
      failedMessages.put("msg2", ex2);

      var exception = new MailSendException(failedMessages);

      assertThat(exception.getMessageExceptions())
              .containsExactly(ex1, ex2);
    }

    @Test
    void failedMessagesMapContainsOriginalMessages() {
      var failedMessages = new LinkedHashMap<Object, Exception>();
      failedMessages.put("msg1", new RuntimeException());
      failedMessages.put("msg2", new RuntimeException());

      var exception = new MailSendException(failedMessages);

      assertThat(exception.getFailedMessages())
              .containsAllEntriesOf(failedMessages);
    }

    @Test
    void messageIncludesBaseMessageAndFailedMessages() {
      Map failedMessages = Map.of(
              "msg1", new RuntimeException("ex1"),
              "msg2", new RuntimeException("ex2")
      );

      var exception = new MailSendException("base message", null, failedMessages);

      assertThat(exception.getMessage())
              .startsWith("base message")
              .contains("Failed messages")
              .contains("ex1")
              .contains("ex2");
    }

    @Test
    void toStringIncludesFailedMessagesCount() {
      Map failedMessages = Map.of(
              "msg1", new RuntimeException("ex1"),
              "msg2", new RuntimeException("ex2")
      );

      var exception = new MailSendException(failedMessages);

      assertThat(exception.toString())
              .contains("message exceptions (2)")
              .contains("Failed message 1")
              .contains("Failed message 2");
    }

    @Test
    void printStackTraceIncludesAllFailedMessages() {
      Map failedMessages = Map.of(
              "msg1", new RuntimeException("ex1"),
              "msg2", new RuntimeException("ex2")
      );
      var exception = new MailSendException(failedMessages);
      var writer = new StringWriter();
      var printWriter = new PrintWriter(writer);

      exception.printStackTrace(printWriter);

      assertThat(writer.toString())
              .contains("message exception details (2)")
              .contains("Failed message 1")
              .contains("Failed message 2");
    }

    @Test
    void exceptionWithoutFailedMessagesConstructedWithMessageAndCause() {
      var cause = new RuntimeException("root cause");
      var exception = new MailSendException("mail failed", cause);

      assertThat(exception.getMessage()).isEqualTo("mail failed");
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getFailedMessages()).isEmpty();
      assertThat(exception.getMessageExceptions()).isEmpty();
    }

    @Test
    void constructorWithMessageCauseAndFailedMessages() {
      var cause = new RuntimeException("root cause");
      Map failedMessages = Map.of(
              "msg1", new RuntimeException("ex1"),
              "msg2", new RuntimeException("ex2"));

      var exception = new MailSendException("mail failed", cause, failedMessages);

      assertThat(exception.getMessage())
              .contains("mail failed")
              .contains("Failed messages")
              .contains("ex1")
              .contains("ex2");
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getFailedMessages()).containsAllEntriesOf(failedMessages);
    }

    @Test
    void messageExceptionsAreOrderedAsInFailedMessages() {
      var ex1 = new RuntimeException("ex1");
      var ex2 = new RuntimeException("ex2");
      var failedMessages = new LinkedHashMap<Object, Exception>();
      failedMessages.put("msg1", ex1);
      failedMessages.put("msg2", ex2);

      var exception = new MailSendException(failedMessages);

      assertThat(exception.getMessageExceptions())
              .containsExactly(ex1, ex2);
    }

    @Test
    void toStringWithoutExceptionsUsesBaseToString() {
      var exception = new MailSendException("mail failed", null);

      assertThat(exception.toString())
              .isEqualTo(MailSendException.class.getName() + ": mail failed");
    }

    @Test
    void printStackTraceWithoutExceptionsUsesBaseStackTrace() {
      var exception = new MailSendException("mail failed", null);
      var writer = new StringWriter();
      var printWriter = new PrintWriter(writer);

      exception.printStackTrace(printWriter);

      assertThat(writer.toString())
              .startsWith(MailSendException.class.getName())
              .contains("mail failed");
    }

    @Test
    void printStackTraceToStreamWithoutExceptionsUsesBaseStackTrace() {
      var exception = new MailSendException("mail failed", null);
      var output = new ByteArrayOutputStream();
      var printStream = new PrintStream(output);

      exception.printStackTrace(printStream);

      assertThat(output.toString())
              .startsWith(MailSendException.class.getName())
              .contains("mail failed");
    }

    @Test
    void nullMessageInConstructorIsAccepted() {
      var exception = new MailSendException(null, new RuntimeException());
      assertThat(exception.getMessage()).isNull();
    }

    @Test
    void emptyFailedMessagesMapIsAccepted() {
      var exception = new MailSendException(Map.of());
      assertThat(exception.getMessageExceptions()).isEmpty();
      assertThat(exception.getFailedMessages()).isEmpty();
    }

    @Test
    void messageWithoutBaseMessageOnlyIncludesFailedMessages() {
      Map failedMessages = Map.of(
              "msg1", new RuntimeException("ex1"),
              "msg2", new RuntimeException("ex2"));

      var exception = new MailSendException(null, null, failedMessages);

      assertThat(exception.getMessage())
              .startsWith("Failed messages")
              .contains("ex1")
              .contains("ex2")
              .doesNotContain("null");
    }

    @Test
    void failedMessagesAreDefensiveCopied() {
      var failedMessages = new LinkedHashMap<Object, Exception>();
      failedMessages.put("msg1", new RuntimeException());
      var exception = new MailSendException(failedMessages);

      failedMessages.clear();

      assertThat(exception.getFailedMessages()).isNotEmpty();
    }

    @Test
    void nullFailedMessagesMapThrowsException() {
      assertThatNullPointerException()
              .isThrownBy(() -> new MailSendException("msg", null, null));
    }

    @Test
    void constructorWithNullCauseAndEmptyFailedMessages() {
      var exception = new MailSendException("msg", null, Map.of());
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getFailedMessages()).isEmpty();
    }

    @Test
    void failedMessagesWithNullKeysAreAccepted() {
      Map failedMessages = new LinkedHashMap<Object, Exception>();
      failedMessages.put(null, new RuntimeException("ex1"));
      var exception = new MailSendException(failedMessages);

      assertThat(exception.getFailedMessages())
              .containsKey(null)
              .hasSize(1);
    }

    @Test
    void printStreamNullCausesPrintStreamNullPointerException() {
      var exception = new MailSendException("msg", null);
      assertThatNullPointerException()
              .isThrownBy(() -> exception.printStackTrace((PrintStream) null));
    }

    @Test
    void printStreamToSystemOut() {
      var exception = new MailSendException("msg", new RuntimeException("cause"));
      var originalOut = System.out;
      var output = new ByteArrayOutputStream();
      System.setOut(new PrintStream(output));

      try {
        exception.printStackTrace(System.out);
        assertThat(output.toString())
                .contains(MailSendException.class.getName())
                .contains("msg")
                .contains("cause");
      }
      finally {
        System.setOut(originalOut);
      }
    }

    @Test
    void printStackTraceToStreamHandlesFailedMessagesWithEmptyExceptions() {
      Map failedMessages = Map.of("msg", new RuntimeException(""));
      var exception = new MailSendException(failedMessages);
      var output = new ByteArrayOutputStream();
      var printStream = new PrintStream(output);

      exception.printStackTrace(printStream);

      assertThat(output.toString())
              .contains("message exception details (1)")
              .contains("Failed message 1")
              .contains(RuntimeException.class.getName());
    }

    @Test
    void printStackTraceToStreamWithLongExceptionMessage() {
      var message = "a".repeat(4096);
      Map failedMessages = Map.of("msg", new RuntimeException(message));
      var exception = new MailSendException(failedMessages);
      var output = new ByteArrayOutputStream();
      var printStream = new PrintStream(output);

      exception.printStackTrace(printStream);

      assertThat(output.toString())
              .contains("message exception details (1)")
              .contains("Failed message 1")
              .contains(message);
    }

  }

  @Nested
  class MailPreparationExceptionTests {

    @Test
    void constructorWithMessageAndCause() {
      var cause = new RuntimeException("root cause");
      var exception = new MailPreparationException("failed to prepare", cause);

      assertThat(exception.getMessage()).isEqualTo("failed to prepare");
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithNullMessageAndCause() {
      var cause = new RuntimeException();
      var exception = new MailPreparationException(null, cause);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithMessageAndNullCause() {
      var exception = new MailPreparationException("failed to prepare", null);

      assertThat(exception.getMessage()).isEqualTo("failed to prepare");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithCauseOnly() {
      var cause = new RuntimeException("root");
      var exception = new MailPreparationException(cause);

      assertThat(exception.getMessage()).isEqualTo("Could not prepare mail");
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithNullCauseOnly() {
      var exception = new MailPreparationException((Throwable) null);

      assertThat(exception.getMessage()).isEqualTo("Could not prepare mail");
      assertThat(exception.getCause()).isNull();
    }
  }

}
