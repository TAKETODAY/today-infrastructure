/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.client;

import org.reactivestreams.Publisher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.ResolvableTypeProvider;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.multipart.Part;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;

/**
 * Prepare the body of a multipart request, resulting in a
 * {@code MultiValueMap<String, HttpEntity>}. Parts may be concrete values or
 * via asynchronous types such as Reactor {@code Mono}, {@code Flux}, and
 * others registered in the
 * {@link cn.taketoday.core.ReactiveAdapterRegistry ReactiveAdapterRegistry}.
 *
 * <p>Below are examples of using this builder:
 * <pre>{@code
 *
 * // Add form field
 * MultipartBodyBuilder builder = new MultipartBodyBuilder();
 * builder.part("form field", "form value").header("foo", "bar");
 *
 * // Add file part
 * Resource image = new ClassPathResource("image.jpg");
 * builder.part("image", image).header("foo", "bar");
 *
 * // Add content (e.g. JSON)
 * Account account = ...
 * builder.part("account", account).header("foo", "bar");
 *
 * // Add content from Publisher
 * Mono<Account> accountMono = ...
 * builder.asyncPart("account", accountMono).header("foo", "bar");
 *
 * // Build and use
 * MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
 *
 * Mono<Void> result = webClient.post()
 *     .uri("...")
 *     .body(multipartBody)
 *     .retrieve()
 *     .bodyToMono(Void.class)
 * }</pre>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>
 * @since 4.0 2021/11/5 23:00
 */
public final class MultipartBodyBuilder {
  private final DefaultMultiValueMap<String, DefaultPartBuilder> parts = MultiValueMap.forLinkedHashMap();

  /**
   * Add a part where the Object may be:
   * <ul>
   * <li>String -- form field
   * <li>{@link cn.taketoday.core.io.Resource Resource} -- file part
   * <li>Object -- content to be encoded (e.g. to JSON)
   * <li>{@link HttpEntity} -- part content and headers although generally it's
   * easier to add headers through the returned builder
   * <li>{@link Part} -- a part from a server request
   * </ul>
   *
   * @param name the name of the part to add
   * @param part the part data
   * @return builder that allows for further customization of part headers
   */
  public PartBuilder part(String name, Object part) {
    return part(name, part, null);
  }

  /**
   * Variant of {@link #part(String, Object)} that also accepts a MediaType.
   *
   * @param name the name of the part to add
   * @param part the part data
   * @param contentType the media type to help with encoding the part
   * @return builder that allows for further customization of part headers
   */
  public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
    Assert.hasLength(name, "'name' must not be empty");
    Assert.notNull(part, "'part' is required");

    if (part instanceof Part partObject) {
      PartBuilder builder = asyncPart(name, partObject.content(), DataBuffer.class);
      if (!partObject.headers().isEmpty()) {
        builder.headers(headers -> {
          headers.putAll(partObject.headers());
          String filename = headers.getContentDisposition().getFilename();
          // reset to parameter name
          headers.setContentDispositionFormData(name, filename);
        });
      }
      if (contentType != null) {
        builder.contentType(contentType);
      }
      return builder;
    }

    if (part instanceof PublisherEntity<?, ?> publisherEntity) {
      PublisherPartBuilder<?, ?> builder = new PublisherPartBuilder<>(name, publisherEntity);
      if (contentType != null) {
        builder.contentType(contentType);
      }
      this.parts.add(name, builder);
      return builder;
    }

    Object partBody;
    HttpHeaders partHeaders = null;
    if (part instanceof HttpEntity<?> httpEntity) {
      partBody = httpEntity.getBody();
      partHeaders = HttpHeaders.create();
      partHeaders.putAll(httpEntity.getHeaders());
    }
    else {
      partBody = part;
    }

    if (partBody instanceof Publisher) {
      throw new IllegalArgumentException("""
              Use asyncPart(String, Publisher, Class) \
              or asyncPart(String, Publisher, TypeReference) \
              or MultipartBodyBuilder.PublisherEntity""");
    }

    DefaultPartBuilder builder = new DefaultPartBuilder(name, partHeaders, partBody);
    if (contentType != null) {
      builder.contentType(contentType);
    }
    this.parts.add(name, builder);
    return builder;
  }

  /**
   * Add a part from {@link Publisher} content.
   *
   * @param name the name of the part to add
   * @param publisher a Publisher of content for the part
   * @param elementClass the type of elements contained in the publisher
   * @return builder that allows for further customization of part headers
   */
  public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, Class<T> elementClass) {
    Assert.hasLength(name, "'name' must not be empty");
    Assert.notNull(publisher, "'publisher' is required");
    Assert.notNull(elementClass, "'elementClass' is required");

    PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, elementClass);
    this.parts.add(name, builder);
    return builder;
  }

  /**
   * Variant of {@link #asyncPart(String, Publisher, Class)} with a
   * {@link TypeReference} for the element type information.
   *
   * @param name the name of the part to add
   * @param publisher the part contents
   * @param typeReference the type of elements contained in the publisher
   * @return builder that allows for further customization of part headers
   */
  public <T, P extends Publisher<T>> PartBuilder asyncPart(
          String name, P publisher, TypeReference<T> typeReference) {

    Assert.hasLength(name, "'name' must not be empty");
    Assert.notNull(publisher, "'publisher' is required");
    Assert.notNull(typeReference, "'typeReference' is required");

    PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, typeReference);
    this.parts.add(name, builder);
    return builder;
  }

  /**
   * Return a {@code MultiValueMap} with the configured parts.
   */
  public MultiValueMap<String, HttpEntity<?>> build() {
    DefaultMultiValueMap<String, HttpEntity<?>> result = MultiValueMap.forLinkedHashMap(this.parts.size());
    for (Map.Entry<String, List<DefaultPartBuilder>> entry : this.parts.entrySet()) {
      for (DefaultPartBuilder builder : entry.getValue()) {
        HttpEntity<?> entity = builder.build();
        result.add(entry.getKey(), entity);
      }
    }
    return result;
  }

  /**
   * Builder that allows for further customization of part headers.
   */
  public interface PartBuilder {

    /**
     * Set the {@linkplain MediaType media type} of the part.
     *
     * @param contentType the content type
     * @see HttpHeaders#setContentType(MediaType)
     */
    PartBuilder contentType(MediaType contentType);

    /**
     * Set the filename parameter for a file part. This should not be
     * necessary with {@link cn.taketoday.core.io.Resource Resource}
     * based parts that expose a filename but may be useful for
     * {@link Publisher} parts.
     *
     * @param filename the filename to set on the Content-Disposition
     */
    PartBuilder filename(String filename);

    /**
     * Add part header values.
     *
     * @param headerName the part header name
     * @param headerValues the part header value(s)
     * @return this builder
     * @see HttpHeaders#addAll(Object, Collection)
     */
    PartBuilder header(String headerName, String... headerValues);

    /**
     * Manipulate the part headers through the given consumer.
     *
     * @param headersConsumer consumer to manipulate the part headers with
     * @return this builder
     */
    PartBuilder headers(Consumer<HttpHeaders> headersConsumer);
  }

  private static class DefaultPartBuilder implements PartBuilder {

    private final String name;

    @Nullable
    protected HttpHeaders headers;

    @Nullable
    protected final Object body;

    public DefaultPartBuilder(String name, @Nullable HttpHeaders headers, @Nullable Object body) {
      this.name = name;
      this.headers = headers;
      this.body = body;
    }

    @Override
    public PartBuilder contentType(MediaType contentType) {
      initHeadersIfNecessary().setContentType(contentType);
      return this;
    }

    @Override
    public PartBuilder filename(String filename) {
      initHeadersIfNecessary().setContentDispositionFormData(this.name, filename);
      return this;
    }

    @Override
    public PartBuilder header(String headerName, String... headerValues) {
      initHeadersIfNecessary().addAll(headerName, Arrays.asList(headerValues));
      return this;
    }

    @Override
    public PartBuilder headers(Consumer<HttpHeaders> headersConsumer) {
      headersConsumer.accept(initHeadersIfNecessary());
      return this;
    }

    private HttpHeaders initHeadersIfNecessary() {
      if (this.headers == null) {
        this.headers = HttpHeaders.create();
      }
      return this.headers;
    }

    public HttpEntity<?> build() {
      return new HttpEntity<>(this.body, this.headers);
    }
  }

  private static class PublisherPartBuilder<S, P extends Publisher<S>> extends DefaultPartBuilder {

    private final ResolvableType resolvableType;

    public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body, Class<S> elementClass) {
      super(name, headers, body);
      this.resolvableType = ResolvableType.fromClass(elementClass);
    }

    public PublisherPartBuilder(
            String name, @Nullable HttpHeaders headers, P body, TypeReference<S> typeRef) {

      super(name, headers, body);
      this.resolvableType = ResolvableType.fromType(typeRef);
    }

    public PublisherPartBuilder(String name, PublisherEntity<S, P> other) {
      super(name, other.getHeaders(), other.getBody());
      this.resolvableType = other.getResolvableType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpEntity<?> build() {
      P publisher = (P) this.body;
      Assert.state(publisher != null, "Publisher is required");
      return new PublisherEntity<>(this.headers, publisher, this.resolvableType);
    }
  }

  /**
   * Specialization of {@link HttpEntity} for use with a
   * {@link Publisher}-based body, for which we also need to keep track of
   * the element type.
   *
   * @param <T> the type contained in the publisher
   * @param <P> the publisher
   */
  static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P>
          implements ResolvableTypeProvider {

    private final ResolvableType resolvableType;

    PublisherEntity(
            @Nullable MultiValueMap<String, String> headers, P publisher, ResolvableType resolvableType) {

      super(publisher, headers);
      Assert.notNull(publisher, "'publisher' is required");
      Assert.notNull(resolvableType, "'resolvableType' is required");
      this.resolvableType = resolvableType;
    }

    /**
     * Return the element type for the {@code Publisher} body.
     */
    @Override
    @NonNull
    public ResolvableType getResolvableType() {
      return this.resolvableType;
    }
  }

}
