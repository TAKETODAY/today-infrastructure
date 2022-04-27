/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.codec.multipart;

import org.reactivestreams.Publisher;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Flux;

/**
 * Represents an event for a "multipart/form-data" request.
 * Can be a {@link FormPartEvent} or a {@link FilePartEvent}.
 *
 * <h2>Server Side</h2>
 *
 * Each part in a multipart HTTP message produces at least one
 * {@code PartEvent} containing both {@link #headers() headers} and a
 * {@linkplain PartEvent#content() buffer} with content of the part.
 * <ul>
 * <li>Form field will produce a <em>single</em> {@link FormPartEvent},
 * containing the {@linkplain FormPartEvent#value() value} of the field.</li>
 * <li>File uploads will produce <em>one or more</em> {@link FilePartEvent}s,
 * containing the {@linkplain FilePartEvent#filename() filename} used when
 * uploading. If the file is large enough to be split across multiple buffers,
 * the first {@code FilePartEvent} will be followed by subsequent events.</li>
 * </ul>
 * The final {@code PartEvent} for a particular part will have
 * {@link #isLast()} set to {@code true}, and can be followed by
 * additional events belonging to subsequent parts.
 * The {@code isLast()} property is suitable as a predicate for the
 * {@link Flux#windowUntil(Predicate)} operator, in order to split events from
 * all parts into windows that each belong to a single part.
 * From that, the {@link Flux#switchOnFirst(BiFunction)} operator allows you to
 * see whether you are handling a form field or file upload.
 * For example:
 *
 * <pre class=code>
 * Flux&lt;PartEvent&gt; allPartsEvents = ... // obtained via @RequestPayload or request.bodyToFlux(PartEvent.class)
 * allPartsEvents.windowUntil(PartEvent::isLast)
 *   .concatMap(p -> p.switchOnFirst((signal, partEvents) -> {
 *       if (signal.hasValue()) {
 *           PartEvent event = signal.get();
 *           if (event instanceof FormPartEvent formEvent) {
 *               String value = formEvent.value();
 *               // handle form field
 *           }
 *           else if (event instanceof FilePartEvent fileEvent) {
 *               String filename filename = fileEvent.filename();
 *               Flux&lt;DataBuffer&gt; contents = partEvents.map(PartEvent::content);
 *               // handle file upload
 *           }
 *           else {
 *               return Mono.error("Unexpected event: " + event);
 *           }
 *       }
 *       else {
 *         return partEvents; // either complete or error signal
 *       }
 *   }))
 * </pre>
 * Received part events can also be relayed to another service by using the
 * {@link cn.taketoday.web.reactive.function.client.WebClient WebClient}.
 * See below.
 *
 * <p><strong>NOTE</strong> that the {@linkplain PartEvent#content() body contents}
 * must be completely consumed, relayed, or released to avoid memory leaks.
 *
 * <h2>Client Side</h2>
 * On the client side, {@code PartEvent}s can be created to represent a file upload.
 * <ul>
 * <li>Form fields can be created via {@link FormPartEvent#create(String, String)}.</li>
 * <li>File uploads can be created via {@link FilePartEvent#create(String, Path)}.</li>
 * </ul>
 * The streams returned by these static methods can be concatenated via
 * {@link Flux#concat(Publisher[])} to create a request for the
 * {@link cn.taketoday.web.reactive.function.client.WebClient WebClient}:
 * For instance, this sample will POST a multipart form containing a form field
 * and a file.
 *
 * <pre class=code>
 * Resource resource = ...
 * Mono&lt;String&gt; result = webClient
 *   .post()
 *   .uri("https://example.com")
 *   .body(Flux.concat(
 *     FormEventPart.create("field", "field value"),
 *     FilePartEvent.create("file", resource)
 *   ), PartEvent.class)
 *   .retrieve()
 *   .bodyToMono(String.class);
 * </pre>
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FormPartEvent
 * @see FilePartEvent
 * @see PartEventHttpMessageReader
 * @see PartEventHttpMessageWriter
 * @since 4.0 2022/4/22 9:10
 */
public interface PartEvent {

  /**
   * Return the name of the event, as provided through the
   * {@code Content-Disposition name} parameter.
   *
   * @return the name of the part, never {@code null} or empty
   */
  default String name() {
    String name = headers().getContentDisposition().getName();
    Assert.state(name != null, "No name available");
    return name;
  }

  /**
   * Return the headers of the part that this event belongs to.
   */
  HttpHeaders headers();

  /**
   * Return the content of this event. The returned buffer must be consumed or
   * {@linkplain cn.taketoday.core.io.buffer.DataBufferUtils#release(DataBuffer) released}.
   */
  DataBuffer content();

  /**
   * Indicates whether this is the last event of a particular
   * part.
   */
  boolean isLast();

}
