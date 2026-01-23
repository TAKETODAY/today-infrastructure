package infra.web.server;

import org.junit.jupiter.api.Test;

import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/14 11:46
 */
class PortInUseExceptionTests {
  @Test
  void constructorWithPortOnly() {
    int port = 8080;
    PortInUseException exception = new PortInUseException(port);

    assertThat(exception.getPort()).isEqualTo(port);
    assertThat(exception.getMessage()).isEqualTo("Port 8080 is already in use");
    assertThat(exception.getCause()).isNull();
  }

  @Test
  void constructorWithPortAndCause() {
    int port = 8080;
    Throwable cause = new RuntimeException("test cause");
    PortInUseException exception = new PortInUseException(port, cause);

    assertThat(exception.getPort()).isEqualTo(port);
    assertThat(exception.getMessage()).isEqualTo("Port 8080 is already in use");
    assertThat(exception.getCause()).isSameAs(cause);
  }

  @Test
  void throwIfPortBindingExceptionShouldThrowPortInUseExceptionWhenBindExceptionContainsInUse() {
    var bindException = new java.net.BindException("Address already in use");
    IntSupplier portSupplier = () -> 8080;

    assertThatExceptionOfType(PortInUseException.class)
            .isThrownBy(() -> PortInUseException.throwIfPortBindingException(bindException, portSupplier))
            .withMessage("Port 8080 is already in use")
            .withCause(bindException);
  }

  @Test
  void throwIfPortBindingExceptionShouldNotThrowWhenBindExceptionDoesNotContainInUse() {
    var bindException = new java.net.BindException("Address not available");
    IntSupplier portSupplier = () -> 8080;

    assertThatNoException().isThrownBy(() ->
            PortInUseException.throwIfPortBindingException(bindException, portSupplier));
  }

  @Test
  void ifPortBindingExceptionShouldPerformActionWhenBindExceptionContainsInUse() {
    var bindException = new java.net.BindException("Address already in use");
    boolean[] actionPerformed = { false };

    PortInUseException.ifPortBindingException(bindException, (ex) -> actionPerformed[0] = true);

    assertThat(actionPerformed[0]).isTrue();
  }

  @Test
  void ifPortBindingExceptionShouldNotPerformActionWhenBindExceptionDoesNotContainInUse() {
    var bindException = new java.net.BindException();
    boolean[] actionPerformed = { false };

    PortInUseException.ifPortBindingException(bindException, (ex) -> actionPerformed[0] = true);

    assertThat(actionPerformed[0]).isFalse();
  }

  @Test
  void ifCausedByShouldPerformActionWhenExceptionHasMatchingCause() {
    RuntimeException cause = new RuntimeException("root cause");
    Exception exception = new Exception("wrapper", cause);
    boolean[] actionPerformed = { false };

    PortInUseException.ifCausedBy(exception, RuntimeException.class, (ex) -> actionPerformed[0] = true);

    assertThat(actionPerformed[0]).isTrue();
  }

  @Test
  void ifCausedByShouldNotPerformActionWhenExceptionDoesNotHaveMatchingCause() {
    Exception exception = new Exception("test exception");
    boolean[] actionPerformed = { false };

    PortInUseException.ifCausedBy(exception, IllegalStateException.class, (ex) -> actionPerformed[0] = true);

    assertThat(actionPerformed[0]).isFalse();
  }

  @Test
  void exceptionExtendsWebServerException() {
    PortInUseException exception = new PortInUseException(8080);

    assertThat(exception).isInstanceOf(WebServerException.class);
  }

}