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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import infra.core.StringValueResolver;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotationPredicates;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.RepeatableContainers;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.VisibleForTesting;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.annotation.RequestMapping;
import infra.web.service.annotation.HttpExchange;

/**
 * Factory for {@link HttpRequestValues} with values extracted from the type
 * and method-level {@link HttpExchange @HttpRequest} annotations.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/19 20:55
 */
final class HttpRequestValuesInitializer {

  @Nullable
  private final HttpMethod httpMethod;

  @Nullable
  private final String url;

  @Nullable
  private final MediaType contentType;

  @Nullable
  private final List<MediaType> acceptMediaTypes;

  @Nullable
  private final MultiValueMap<String, String> otherHeaders;

  @Nullable
  private final MultiValueMap<String, String> params;

  private final Supplier<HttpRequestValues.Builder> requestValuesSupplier;

  @Nullable
  private final String version;

  HttpRequestValuesInitializer(@Nullable HttpMethod method, @Nullable String url, @Nullable MediaType contentType,
          @Nullable List<MediaType> acceptMediaTypes, @Nullable MultiValueMap<String, String> otherHeaders,
          @Nullable MultiValueMap<String, String> params, Supplier<HttpRequestValues.Builder> requestValuesSupplier, @Nullable String version) {
    this.url = url;
    this.params = params;
    this.httpMethod = method;
    this.contentType = contentType;
    this.otherHeaders = otherHeaders;
    this.acceptMediaTypes = acceptMediaTypes;
    this.requestValuesSupplier = requestValuesSupplier;
    this.version = version;
  }

  public HttpRequestValues.Builder initializeRequestValuesBuilder() {
    HttpRequestValues.Builder requestValues = requestValuesSupplier.get();
    if (httpMethod != null) {
      requestValues.setHttpMethod(httpMethod);
    }
    if (url != null) {
      requestValues.setUriTemplate(url);
    }
    if (contentType != null) {
      requestValues.setContentType(contentType);
    }
    if (acceptMediaTypes != null) {
      requestValues.setAccept(acceptMediaTypes);
    }
    if (params != null) {
      for (var entry : params.entrySet()) {
        requestValues.addRequestParameter(entry.getKey(), StringUtils.toStringArray(entry.getValue()));
      }
    }

    if (otherHeaders != null) {
      for (var entry : otherHeaders.entrySet()) {
        var name = entry.getKey();
        var values = entry.getValue();
        if (values.size() == 1) {
          requestValues.addHeader(name, values.get(0));
        }
        else {
          requestValues.addHeader(name, StringUtils.toStringArray(values));
        }
      }
    }

    if (version != null) {
      requestValues.setApiVersion(version);
    }

    return requestValues;
  }

  /**
   * Introspect the method and create the request factory for it.
   */
  public static HttpRequestValuesInitializer create(Method method, Class<?> containingClass,
          @Nullable StringValueResolver embeddedValueResolver, Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

    List<AnnotationDescriptor> methodHttpExchanges = getAnnotationDescriptors(method);
    Assert.state(!methodHttpExchanges.isEmpty(), () -> "Expected @HttpExchange annotation on method " + method);
    Assert.state(methodHttpExchanges.size() == 1,
            () -> "Multiple @HttpExchange annotations found on method %s, but only one is allowed: %s"
                    .formatted(method, methodHttpExchanges));

    List<AnnotationDescriptor> typeHttpExchanges = getAnnotationDescriptors(containingClass);
    Assert.state(typeHttpExchanges.size() <= 1,
            () -> "Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
                    .formatted(containingClass, typeHttpExchanges));

    MergedAnnotation<?> methodAnnotation = methodHttpExchanges.get(0).httpExchange;
    MergedAnnotation<?> typeAnnotation = !typeHttpExchanges.isEmpty() ? typeHttpExchanges.get(0).httpExchange : null;

    Assert.notNull(methodAnnotation, "Expected HttpRequest annotation");

    HttpMethod httpMethod = initHttpMethod(typeAnnotation, methodAnnotation);
    String url = initURL(typeAnnotation, methodAnnotation, embeddedValueResolver);
    MediaType contentType = initContentType(typeAnnotation, methodAnnotation);
    List<MediaType> acceptableMediaTypes = initAccept(typeAnnotation, methodAnnotation);

    var params = initKeyValues("params", typeAnnotation, methodAnnotation, embeddedValueResolver);
    var headers = initKeyValues("headers", typeAnnotation, methodAnnotation, embeddedValueResolver);

    String version = initVersion(typeAnnotation, methodAnnotation);

    return new HttpRequestValuesInitializer(
            httpMethod, url, contentType, acceptableMediaTypes, headers, params, requestValuesSupplier, version);
  }

  @Nullable
  private static HttpMethod initHttpMethod(@Nullable MergedAnnotation<?> typeAnnot, MergedAnnotation<?> annot) {
    Object value2 = annot.getValue("method");
    if (value2 instanceof String m && StringUtils.hasText(m)) {
      return HttpMethod.valueOf(m);
    }
    else if (value2 instanceof HttpMethod[] m && m.length > 0) {
      return m[0];
    }

    if (typeAnnot != null) {
      Object value1 = typeAnnot.getValue("method");
      if (value1 instanceof String m && StringUtils.hasText(m)) {
        return HttpMethod.valueOf(m);
      }
      else if (value1 instanceof HttpMethod[] m && m.length > 0) {
        return m[0];
      }
    }
    return null;
  }

  @Nullable
  private static String initVersion(@Nullable MergedAnnotation<?> typeAnnotation, MergedAnnotation<?> methodAnnotation) {
    String version = methodAnnotation.getValue("version", String.class);
    if (StringUtils.hasText(version)) {
      return version;
    }
    if (typeAnnotation != null) {
      version = typeAnnotation.getValue("version", String.class);
      if (StringUtils.hasText(version)) {
        return version;
      }
    }
    return null;
  }

  @Nullable
  private static String initURL(@Nullable MergedAnnotation<?> typeAnnot,
          MergedAnnotation<?> annot, @Nullable StringValueResolver embeddedValueResolver) {

    String url1 = null;
    if (typeAnnot != null) {
      url1 = typeAnnot.getValue("url", String.class);
      if (url1 == null) {
        url1 = typeAnnot.getValue("path", String.class);
      }
    }

    String url2 = annot.getValue("url", String.class);
    if (url2 == null) {
      url2 = annot.getValue("path", String.class, "");
    }

    if (embeddedValueResolver != null) {
      if (url1 != null) {
        url1 = embeddedValueResolver.resolveStringValue(url1);
      }
      url2 = embeddedValueResolver.resolveStringValue(url2);
    }

    boolean hasUrl1 = StringUtils.hasText(url1);
    boolean hasUrl2 = StringUtils.hasText(url2);

    if (hasUrl1 && hasUrl2) {
      return url1 + (!url1.endsWith("/") && !url2.startsWith("/") ? "/" : "") + url2;
    }

    if (!hasUrl1 && !hasUrl2) {
      return null;
    }

    return hasUrl2 ? url2 : url1;
  }

  @Nullable
  private static MediaType initContentType(@Nullable MergedAnnotation<?> typeAnnot, MergedAnnotation<?> annot) {
    String value1 = null;
    if (typeAnnot != null) {
      value1 = typeAnnot.getValue("contentType", String.class);
      if (value1 == null) {
        value1 = typeAnnot.getValue("consumes", String.class);
      }
    }

    String value2 = annot.getValue("contentType", String.class);
    if (value2 == null) {
      value2 = annot.getValue("consumes", String.class);
    }

    if (StringUtils.hasText(value2)) {
      return MediaType.parseMediaType(value2);
    }

    if (StringUtils.hasText(value1)) {
      return MediaType.parseMediaType(value1);
    }

    return null;
  }

  @Nullable
  private static List<MediaType> initAccept(@Nullable MergedAnnotation<?> typeAnnot, MergedAnnotation<?> annot) {
    String[] value1 = null;
    if (typeAnnot != null) {
      value1 = typeAnnot.getValue("accept", String[].class);
      if (value1 == null) {
        value1 = typeAnnot.getValue("produces", String[].class);
      }
    }

    String[] value2 = annot.getValue("accept", String[].class);
    if (value2 == null) {
      value2 = annot.getValue("produces", String[].class);
    }

    if (ObjectUtils.isNotEmpty(value2)) {
      return MediaType.parseMediaTypes(Arrays.asList(value2));
    }

    if (ObjectUtils.isNotEmpty(value1)) {
      return MediaType.parseMediaTypes(Arrays.asList(value1));
    }

    return null;
  }

  @Nullable
  @VisibleForTesting
  static MultiValueMap<String, String> initKeyValues(String attributeName, @Nullable MergedAnnotation<?> typeAnnotation,
          MergedAnnotation<?> methodAnnotation, @Nullable StringValueResolver embeddedValueResolver) {

    MultiValueMap<String, String> paramsMap = null;
    if (typeAnnotation != null) {
      String[] params = typeAnnotation.getValue(attributeName, String[].class);
      if (ObjectUtils.isNotEmpty(params)) {
        paramsMap = parseKeyValuePair(params, embeddedValueResolver);
      }
    }

    String[] params = methodAnnotation.getValue(attributeName, String[].class);
    if (ObjectUtils.isNotEmpty(params)) {
      var methodLevelParams = parseKeyValuePair(params, embeddedValueResolver);
      if (paramsMap != null) {
        paramsMap.setAll(methodLevelParams);
      }
      else {
        paramsMap = methodLevelParams;
      }
    }

    if (CollectionUtils.isNotEmpty(paramsMap)) {
      return paramsMap;
    }
    return null;
  }

  private static MultiValueMap<String, String> parseKeyValuePair(String[] array, @Nullable StringValueResolver embeddedValueResolver) {
    MultiValueMap<String, String> kv = new LinkedMultiValueMap<>();
    for (String string : array) {
      String[] kvPair = StringUtils.split(string, "=");
      if (kvPair != null) {
        String name = kvPair[0].trim();
        Set<String> parsedValues = StringUtils.commaDelimitedListToSet(kvPair[1]);
        if (parsedValues.isEmpty()) {
          kv.add(name, "");
        }
        else {
          for (String value : parsedValues) {
            if (embeddedValueResolver != null) {
              value = embeddedValueResolver.resolveStringValue(value);
            }
            if (value != null) {
              kv.add(name, value.trim());
            }
          }
        }
      }
      else if (StringUtils.hasText(string)) {
        kv.add(string, "");
      }
    }
    return kv;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <T extends Annotation> List<AnnotationDescriptor> getAnnotationDescriptors(AnnotatedElement element) {
    MergedAnnotations annotations = MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());
    Stream<MergedAnnotation<T>> concat = Stream.concat(
            (Stream) annotations.stream(RequestMapping.class), (Stream) annotations.stream(HttpExchange.class));
    return concat.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
            .map(AnnotationDescriptor::new)
            .distinct()
            .toList();
  }

  private static class AnnotationDescriptor {

    private final MergedAnnotation<?> httpExchange;

    private final MergedAnnotation<?> root;

    AnnotationDescriptor(MergedAnnotation<?> mergedAnnotation) {
      this.httpExchange = mergedAnnotation;
      this.root = mergedAnnotation.getRoot();
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof AnnotationDescriptor that && this.httpExchange.equals(that.httpExchange));
    }

    @Override
    public int hashCode() {
      return this.httpExchange.hashCode();
    }

    @Override
    public String toString() {
      return this.root.synthesize().toString();
    }
  }

}
