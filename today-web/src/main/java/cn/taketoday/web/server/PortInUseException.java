/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.server;

import java.net.BindException;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import cn.taketoday.lang.Nullable;

/**
 * A {@code PortInUseException} is thrown when a web server fails to start due to a port
 * already being in use.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PortInUseException extends WebServerException {

  private final int port;

  /**
   * Creates a new port in use exception for the given {@code port}.
   *
   * @param port the port that was in use
   */
  public PortInUseException(int port) {
    this(port, null);
  }

  /**
   * Creates a new port in use exception for the given {@code port}.
   *
   * @param port the port that was in use
   * @param cause the cause of the exception
   */
  public PortInUseException(int port, @Nullable Throwable cause) {
    super("Port %d is already in use".formatted(port), cause);
    this.port = port;
  }

  /**
   * Returns the port that was in use.
   *
   * @return the port
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Throw a {@link PortInUseException} if the given exception was caused by a "port in
   * use" {@link BindException}.
   *
   * @param ex the source exception
   * @param port a suppler used to provide the port
   */
  public static void throwIfPortBindingException(Exception ex, IntSupplier port) {
    ifPortBindingException(ex, (bindException) -> {
      throw new PortInUseException(port.getAsInt(), ex);
    });
  }

  /**
   * Perform an action if the given exception was caused by a "port in use"
   * {@link BindException}.
   *
   * @param ex the source exception
   * @param action the action to perform
   */
  public static void ifPortBindingException(Exception ex, Consumer<BindException> action) {
    ifCausedBy(ex, BindException.class, (bindException) -> {
      // bind exception can be also thrown because an address can't be assigned
      if (bindException.getMessage().toLowerCase().contains("in use")) {
        action.accept(bindException);
      }
    });
  }

  /**
   * Perform an action if the given exception was caused by a specific exception type.
   *
   * @param <E> the cause exception type
   * @param ex the source exception
   * @param causedBy the required cause type
   * @param action the action to perform
   */
  @SuppressWarnings("unchecked")
  public static <E extends Exception> void ifCausedBy(Exception ex, Class<E> causedBy, Consumer<E> action) {
    Throwable candidate = ex;
    while (candidate != null) {
      if (causedBy.isInstance(candidate)) {
        action.accept((E) candidate);
        return;
      }
      candidate = candidate.getCause();
    }
  }

}
