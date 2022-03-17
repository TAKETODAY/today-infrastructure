/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.codec;

import java.time.Duration;

import cn.taketoday.lang.Nullable;

/**
 * Representation for a Server-Sent Event for use with reactive Web support.
 * {@code Flux<ServerSentEvent>} or {@code Observable<ServerSentEvent>}
 *
 * @param <T> the type of data that this event contains
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see ServerSentEventHttpMessageWriter
 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
 * @since 4.0
 */
public final class ServerSentEvent<T> {

  @Nullable
  private final String id;

  @Nullable
  private final String event;

  @Nullable
  private final Duration retry;

  @Nullable
  private final String comment;

  @Nullable
  private final T data;

  private ServerSentEvent(@Nullable String id, @Nullable String event, @Nullable Duration retry,
                          @Nullable String comment, @Nullable T data) {

    this.id = id;
    this.event = event;
    this.retry = retry;
    this.comment = comment;
    this.data = data;
  }

  /**
   * Return the {@code id} field of this event, if available.
   */
  @Nullable
  public String id() {
    return this.id;
  }

  /**
   * Return the {@code event} field of this event, if available.
   */
  @Nullable
  public String event() {
    return this.event;
  }

  /**
   * Return the {@code retry} field of this event, if available.
   */
  @Nullable
  public Duration retry() {
    return this.retry;
  }

  /**
   * Return the comment of this event, if available.
   */
  @Nullable
  public String comment() {
    return this.comment;
  }

  /**
   * Return the {@code data} field of this event, if available.
   */
  @Nullable
  public T data() {
    return this.data;
  }

  @Override
  public String toString() {
    return "ServerSentEvent [id = '" + this.id + "', event='" + this.event + "', retry=" +
            this.retry + ", comment='" + this.comment + "', data=" + this.data + ']';
  }

  /**
   * Return a builder for a {@code SseEvent}.
   *
   * @param <T> the type of data that this event contains
   * @return the builder
   */
  public static <T> Builder<T> builder() {
    return new BuilderImpl<>();
  }

  /**
   * Return a builder for a {@code SseEvent}, populated with the given {@linkplain #data() data}.
   *
   * @param <T> the type of data that this event contains
   * @return the builder
   */
  public static <T> Builder<T> builder(T data) {
    return new BuilderImpl<>(data);
  }

  /**
   * A mutable builder for a {@code SseEvent}.
   *
   * @param <T> the type of data that this event contains
   */
  public interface Builder<T> {

    /**
     * Set the value of the {@code id} field.
     *
     * @param id the value of the id field
     * @return {@code this} builder
     */
    Builder<T> id(String id);

    /**
     * Set the value of the {@code event} field.
     *
     * @param event the value of the event field
     * @return {@code this} builder
     */
    Builder<T> event(String event);

    /**
     * Set the value of the {@code retry} field.
     *
     * @param retry the value of the retry field
     * @return {@code this} builder
     */
    Builder<T> retry(Duration retry);

    /**
     * Set SSE comment. If a multi-line comment is provided, it will be turned into multiple
     * SSE comment lines as defined in Server-Sent Events W3C recommendation.
     *
     * @param comment the comment to set
     * @return {@code this} builder
     */
    Builder<T> comment(String comment);

    /**
     * Set the value of the {@code data} field. If the {@code data} argument is a multi-line
     * {@code String}, it will be turned into multiple {@code data} field lines as defined
     * in the Server-Sent Events W3C recommendation. If {@code data} is not a String, it will
     * be {@linkplain cn.taketoday.http.codec.json.Jackson2JsonEncoder encoded} into JSON.
     *
     * @param data the value of the data field
     * @return {@code this} builder
     */
    Builder<T> data(@Nullable T data);

    /**
     * Builds the event.
     *
     * @return the built event
     */
    ServerSentEvent<T> build();
  }

  private static class BuilderImpl<T> implements Builder<T> {

    @Nullable
    private String id;

    @Nullable
    private String event;

    @Nullable
    private Duration retry;

    @Nullable
    private String comment;

    @Nullable
    private T data;

    public BuilderImpl() {
    }

    public BuilderImpl(T data) {
      this.data = data;
    }

    @Override
    public Builder<T> id(String id) {
      this.id = id;
      return this;
    }

    @Override
    public Builder<T> event(String event) {
      this.event = event;
      return this;
    }

    @Override
    public Builder<T> retry(Duration retry) {
      this.retry = retry;
      return this;
    }

    @Override
    public Builder<T> comment(String comment) {
      this.comment = comment;
      return this;
    }

    @Override
    public Builder<T> data(@Nullable T data) {
      this.data = data;
      return this;
    }

    @Override
    public ServerSentEvent<T> build() {
      return new ServerSentEvent<>(this.id, this.event, this.retry, this.comment, this.data);
    }
  }

}
