/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.docker;

/**
 * Listener for update events published from the {@link DockerApi}.
 *
 * @param <E> the update event type
 * @author Phillip Webb
 * @since 4.0
 */
@FunctionalInterface
public interface UpdateListener<E extends UpdateEvent> {

  /**
   * A no-op update listener.
   *
   * @see #none()
   */
  UpdateListener<UpdateEvent> NONE = (event) -> {
  };

  /**
   * Called when the operation starts.
   */
  default void onStart() {
  }

  /**
   * Called when an update event is available.
   *
   * @param event the update event
   */
  void onUpdate(E event);

  /**
   * Called when the operation finishes (with or without error).
   */
  default void onFinish() {
  }

  /**
   * A no-op update listener that does nothing.
   *
   * @param <E> the event type
   * @return a no-op update listener
   */
  @SuppressWarnings("unchecked")
  static <E extends UpdateEvent> UpdateListener<E> none() {
    return (UpdateListener<E>) NONE;
  }

}
