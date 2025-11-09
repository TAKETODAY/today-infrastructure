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

package infra.http.codec.multipart;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.http.MediaType;
import infra.http.ReactiveHttpInputMessage;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.LoggingCodecSupport;
import infra.lang.Assert;
import infra.util.LinkedMultiValueMap;
import infra.util.LogFormatUtils;
import infra.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageReader} for reading {@code "multipart/form-data"} requests
 * into a {@code MultiValueMap<String, Part>}.
 *
 * <p>Note that this reader depends on access to an
 * {@code HttpMessageReader<Part>} for the actual parsing of multipart content.
 * The purpose of this reader is to collect the parts into a map.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MultipartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<MultiValueMap<String, Part>> {

  private static final ResolvableType MULTIPART_VALUE_TYPE =
          ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);

  static final List<MediaType> MIME_TYPES = List.of(
          MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED, MediaType.MULTIPART_RELATED);

  private final HttpMessageReader<Part> partReader;

  public MultipartHttpMessageReader(HttpMessageReader<Part> partReader) {
    Assert.notNull(partReader, "'partReader' is required");
    this.partReader = partReader;
  }

  /**
   * Return the configured parts reader.
   */
  public HttpMessageReader<Part> getPartReader() {
    return this.partReader;
  }

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return MIME_TYPES;
  }

  @Override
  public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
    if (MULTIPART_VALUE_TYPE.isAssignableFrom(elementType)) {
      if (mediaType == null) {
        return true;
      }
      for (MediaType supportedMediaType : MIME_TYPES) {
        if (supportedMediaType.isCompatibleWith(mediaType)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Flux<MultiValueMap<String, Part>> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
    return Flux.from(readMono(elementType, message, hints));
  }

  @Override
  public Mono<MultiValueMap<String, Part>> readMono(
          ResolvableType elementType, ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {

    Map<String, Object> allHints = Hints.merge(hints, Hints.SUPPRESS_LOGGING_HINT, true);

    if (logger.isDebugEnabled()) {
      return partReader.read(elementType, inputMessage, allHints)
              .collectMultimap(Part::name)
              .doOnNext(map -> {
                LogFormatUtils.traceDebug(
                        logger, traceOn -> Hints.getLogPrefix(hints) + "Parsed " +
                                (isEnableLoggingRequestDetails()
                                        ? LogFormatUtils.formatValue(map, !traceOn)
                                        : "parts " + map.keySet() + " (content masked)"));
              })
              .map(this::toMultiValueMap);
    }

    return partReader.read(elementType, inputMessage, allHints)
            .collectMultimap(Part::name)
            .map(this::toMultiValueMap);
  }

  private LinkedMultiValueMap<String, Part> toMultiValueMap(Map<String, Collection<Part>> map) {
    LinkedMultiValueMap<String, Part> ret = MultiValueMap.forLinkedHashMap(map.size());
    for (Map.Entry<String, Collection<Part>> entry : map.entrySet()) {
      ret.put(entry.getKey(), toList(entry.getValue()));
    }
    return ret;
  }

  private List<Part> toList(Collection<Part> collection) {
    return collection instanceof List ? (List<Part>) collection : new ArrayList<>(collection);
  }

}
