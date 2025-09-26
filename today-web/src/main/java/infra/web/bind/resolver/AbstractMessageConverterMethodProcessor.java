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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import infra.core.GenericTypeResolver;
import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.core.io.ResourceRegion;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpRange;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotWritableException;
import infra.lang.TodayStrategies;
import infra.util.CollectionUtils;
import infra.util.LogFormatUtils;
import infra.util.MimeTypeUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.ErrorResponse;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;
import infra.web.ReturnValueHandler;
import infra.web.accept.ContentNegotiationManager;
import infra.web.util.UriUtils;
import infra.web.util.pattern.PathPattern;

/**
 * Extends {@link infra.web.bind.resolver.AbstractMessageConverterMethodArgumentResolver} with the ability to handle method
 * return values by writing to the response with {@link HttpMessageConverter HttpMessageConverters}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/23 12:30
 */
public abstract class AbstractMessageConverterMethodProcessor extends AbstractMessageConverterMethodArgumentResolver implements ReturnValueHandler {

  /* Extensions associated with the built-in message converters */
  private static final Set<String> SAFE_EXTENSIONS = Set.of(
          "txt", "text", "yml", "properties", "csv",
          "json", "xml", "atom", "rss",
          "png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp"
  );

  private static final Set<String> SAFE_MEDIA_BASE_TYPES = Set.of("audio", "image", "video");

  private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES = List.of(MediaType.ALL, new MediaType("application"));

  private static final Type RESOURCE_REGION_LIST_TYPE = new ParameterizedTypeReference<List<ResourceRegion>>() {

  }.getType();

  private static final List<MediaType> problemMediaTypes = Arrays.asList(
          MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_PROBLEM_XML);

  /**
   * Check if the path has a file extension and whether the extension is either
   * on the list of {@link #SAFE_EXTENSIONS safe extensions} or explicitly
   * {@link infra.web.accept.ContentNegotiationManager#getAllFileExtensions() registered}.
   * If not, and the status is in the 2xx range, a 'Content-Disposition'
   * header with a safe attachment file name ("f.txt") is added to prevent
   * RFD exploits.
   *
   * @see #addContentDispositionHeader(RequestContext, Object)
   * @since 5.0
   */
  private static final boolean preventRFDExploits = TodayStrategies.getFlag("infra.web.prevent-RFD-exploits", true);

  private final ContentNegotiationManager contentNegotiationManager;

  private final HashSet<String> safeExtensions = new HashSet<>();

  @Nullable
  private final ArrayList<ErrorResponse.Interceptor> errorResponseInterceptors;

  /**
   * Constructor with list of converters only.
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters) {
    this(converters, null, null);
  }

  /**
   * Constructor with list of converters and ContentNegotiationManager.
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager contentNegotiationManager) {
    this(converters, contentNegotiationManager, null);
  }

  /**
   * Constructor with list of converters and ContentNegotiationManager as well
   * as request/response body advice instances.
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {

    this(converters, manager, requestResponseBodyAdvice, null);
  }

  /**
   * Variant of {@link #AbstractMessageConverterMethodProcessor(List, ContentNegotiationManager, List)}
   * with additional list of {@link ErrorResponse.Interceptor}s for return
   * value handling.
   *
   * @since 5.0
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable ContentNegotiationManager manager,
          @Nullable List<Object> requestResponseBodyAdvice, @Nullable List<ErrorResponse.Interceptor> interceptors) {
    super(converters, requestResponseBodyAdvice);
    this.contentNegotiationManager = manager != null ? manager : new ContentNegotiationManager();
    this.safeExtensions.addAll(contentNegotiationManager.getAllFileExtensions());
    this.safeExtensions.addAll(SAFE_EXTENSIONS);
    if (CollectionUtils.isNotEmpty(interceptors)) {
      this.errorResponseInterceptors = new ArrayList<>(interceptors);
    }
    else {
      this.errorResponseInterceptors = null;
    }
  }

  // ReturnValueHandler

  /**
   * Writes the given return type to the given output message.
   *
   * @param value the value to write to the output message
   * @param returnType the type of the value
   * @param context the output message to write to and Used to inspect the {@code Accept} header.
   * @throws java.io.IOException thrown in case of I/O errors
   * @throws infra.web.HttpMediaTypeNotAcceptableException thrown when the conditions indicated
   * by the {@code Accept} header on the request cannot be met by the message converters
   * @throws infra.http.converter.HttpMessageNotWritableException thrown if a given message cannot
   * be written by a converter, or if the content-type chosen by the server
   * has no compatible converter.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected <T> void writeWithMessageConverters(@Nullable T value, @Nullable MethodParameter returnType, RequestContext context)
          throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException //
  {

    Object body;
    Type targetType;
    Class<?> valueType;

    if (value instanceof CharSequence) {
      body = value.toString();
      valueType = String.class;
      targetType = String.class;
    }
    else {
      body = value;
      valueType = getReturnValueType(body, returnType);
      if (returnType == null) {
        targetType = ResolvableType.forInstance(body).getType();
      }
      else {
        targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
      }

      if (isResourceType(valueType)) {
        context.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (value != null) {
          String headerRange = context.requestHeaders().getFirst(HttpHeaders.RANGE);
          if (headerRange != null && context.getStatus() == 200) {
            Resource resource = (Resource) value;
            try {
              List<HttpRange> httpRanges = HttpRange.parseRanges(headerRange);
              context.setStatus(HttpStatus.PARTIAL_CONTENT);
              body = HttpRange.toResourceRegions(httpRanges, resource);
              valueType = body.getClass();
              targetType = RESOURCE_REGION_LIST_TYPE;
            }
            catch (IllegalArgumentException ex) {
              context.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
              context.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            }
          }
        }
      }
    }

    MediaType selectedMediaType = null;
    boolean isContentTypePreset = false;
    String contentType = context.getResponseContentType();
    if (contentType != null) {
      MediaType mediaType = MediaType.parseMediaType(contentType);
      isContentTypePreset = mediaType.isConcrete();
      if (isContentTypePreset) {
        if (logger.isDebugEnabled()) {
          logger.debug("Found 'Content-Type:{}' in response", contentType);
        }
        selectedMediaType = mediaType;
      }
    }

    if (!isContentTypePreset) {
      List<MediaType> acceptableTypes;
      try {
        acceptableTypes = getAcceptableMediaTypes(context);
      }
      catch (HttpMediaTypeNotAcceptableException ex) {
        int series = context.getStatus() / 100;
        if (body == null || series == 4 || series == 5) {
          if (logger.isDebugEnabled()) {
            logger.debug("Ignoring error response content (if any). {}", ex.toString());
          }
          return;
        }
        throw ex;
      }

      var producibleTypes = getProducibleMediaTypes(context, valueType, targetType);
      if (body != null && producibleTypes.isEmpty()) {
        throw new HttpMessageNotWritableException(
                "No converter found for return value of type: " + valueType);
      }

      ArrayList<MediaType> compatibleMediaTypes = new ArrayList<>();
      determineCompatibleMediaTypes(acceptableTypes, producibleTypes, compatibleMediaTypes);

      // For ProblemDetail, fall back on RFC 7807 format
      if (compatibleMediaTypes.isEmpty() && ProblemDetail.class.isAssignableFrom(valueType)) {
        determineCompatibleMediaTypes(problemMediaTypes, producibleTypes, compatibleMediaTypes);
      }

      if (compatibleMediaTypes.isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug("No match for {}, supported: {}", acceptableTypes, producibleTypes);
        }
        if (body != null) {
          throw new HttpMediaTypeNotAcceptableException(producibleTypes);
        }
        return;
      }

      MimeTypeUtils.sortBySpecificity(compatibleMediaTypes);

      for (MediaType mediaType : compatibleMediaTypes) {
        if (mediaType.isConcrete()) {
          selectedMediaType = mediaType;
          break;
        }
        else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
          selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
          break;
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Using '{}', given {} and supported {}", selectedMediaType, acceptableTypes, producibleTypes);
      }
    }

    if (selectedMediaType != null) {
      RequestResponseBodyAdviceChain advice = getAdvice();
      selectedMediaType = selectedMediaType.removeQualityValue();
      for (HttpMessageConverter converter : messageConverters) {
        var generic = converter instanceof GenericHttpMessageConverter ? (GenericHttpMessageConverter) converter : null;
        if (generic != null ? generic.canWrite(targetType, valueType, selectedMediaType) : converter.canWrite(valueType, selectedMediaType)) {

          body = advice.beforeBodyWrite(body, returnType, selectedMediaType, converter, context);
          if (body != null) {
            if (logger.isDebugEnabled()) {
              Object theBody = body;
              LogFormatUtils.traceDebug(logger,
                      traceOn -> "Writing [%s]".formatted(LogFormatUtils.formatValue(theBody, !traceOn)));
            }
            if (preventRFDExploits) {
              addContentDispositionHeader(context, body);
            }
            if (generic != null) {
              generic.write(body, targetType, selectedMediaType, context.asHttpOutputMessage());
            }
            else {
              converter.write(body, selectedMediaType, context.asHttpOutputMessage());
            }
          }
          else if (logger.isDebugEnabled()) {
            logger.debug("Nothing to write: null body");
          }
          return;
        }
      }
    }

    if (body != null) {
      HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
      if (matchingMetadata != null) {
        if (isContentTypePreset || CollectionUtils.isNotEmpty(matchingMetadata.getProducibleMediaTypes())) {
          throw new HttpMessageNotWritableException(
                  "No converter for [%s] with preset Content-Type '%s'".formatted(valueType, contentType));
        }
      }
      throw new HttpMediaTypeNotAcceptableException(getSupportedMediaTypes(body.getClass()));
    }
  }

  /**
   * Return the type of the value to be written to the response. Typically this is
   * a simple check via getClass on the value but if the value is null, then the
   * return type needs to be examined possibly including generic type determination
   * (e.g. {@code ResponseEntity<T>}).
   */
  protected Class<?> getReturnValueType(@Nullable Object value, @Nullable MethodParameter returnType) {
    if (value != null) {
      return value.getClass();
    }
    if (returnType != null) {
      return returnType.getParameterType();
    }
    throw new IllegalStateException("return-value and return-type must not be null at same time");
  }

  /**
   * Return whether the returned value or the declared return type extends {@link Resource}.
   */
  protected boolean isResourceType(Class<?> clazz) {
    return clazz != InputStreamResource.class && Resource.class.isAssignableFrom(clazz);
  }

  /**
   * Return the generic type of the {@code returnType} (or of the nested type
   * if it is an {@link HttpEntity}).
   */
  private Type getGenericType(MethodParameter returnType) {
    if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
      return ResolvableType.forType(returnType.getGenericParameterType()).getGeneric().getType();
    }
    else {
      return returnType.getGenericParameterType();
    }
  }

  /**
   * Returns the media types that can be produced. The resulting media types are:
   * <ul>
   * <li>The producible media types specified in the request mappings, or
   * <li>Media types of configured converters that can write the specific return value, or
   * <li>{@link MediaType#ALL}
   * </ul>
   */
  protected Collection<MediaType> getProducibleMediaTypes(RequestContext request, Class<?> valueClass, @Nullable Type targetType) {
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      var mediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (ObjectUtils.isNotEmpty(mediaTypes)) {
        return mediaTypes;
      }
    }

    LinkedHashSet<MediaType> result = new LinkedHashSet<>();
    for (HttpMessageConverter<?> converter : messageConverters) {
      if (converter instanceof GenericHttpMessageConverter<?> generic && targetType != null) {
        if (generic.canWrite(targetType, valueClass, null)) {
          result.addAll(converter.getSupportedMediaTypes(valueClass));
        }
      }
      else if (converter.canWrite(valueClass, null)) {
        result.addAll(converter.getSupportedMediaTypes(valueClass));
      }
    }
    return result.isEmpty() ? Collections.singletonList(MediaType.ALL) : result;
  }

  /**
   * Invoke the configured {@link ErrorResponse.Interceptor}'s.
   *
   * @since 5.0
   */
  protected void invokeErrorResponseInterceptors(ProblemDetail detail, @Nullable ErrorResponse errorResponse) {
    if (errorResponseInterceptors != null) {
      try {
        for (ErrorResponse.Interceptor handler : errorResponseInterceptors) {
          handler.handleError(detail, errorResponse);
        }
      }
      catch (Throwable ex) {
        // ignore
      }
    }
  }

  private List<MediaType> getAcceptableMediaTypes(RequestContext request) throws HttpMediaTypeNotAcceptableException {
    return this.contentNegotiationManager.resolveMediaTypes(request);
  }

  private void determineCompatibleMediaTypes(List<MediaType> acceptableTypes,
          Collection<MediaType> producibleTypes, List<MediaType> mediaTypesToUse) {

    for (MediaType requestedType : acceptableTypes) {
      for (MediaType producibleType : producibleTypes) {
        if (requestedType.isCompatibleWith(producibleType)) {
          mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
        }
      }
    }
  }

  /**
   * Return the more specific of the acceptable and the producible media types
   * with the q-value of the former.
   */
  private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
    MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
    if (acceptType.isLessSpecific(produceTypeToUse)) {
      return produceTypeToUse;
    }
    else {
      return acceptType;
    }
  }

  /**
   * Check if the path has a file extension and whether the extension is either
   * on the list of {@link #SAFE_EXTENSIONS safe extensions} or explicitly
   * {@link infra.web.accept.ContentNegotiationManager#getAllFileExtensions() registered}.
   * If not, and the status is in the 2xx range, a 'Content-Disposition'
   * header with a safe attachment file name ("f.txt") is added to prevent
   * RFD exploits.
   */
  private void addContentDispositionHeader(RequestContext request, Object body) {
    if (request.containsResponseHeader(HttpHeaders.CONTENT_DISPOSITION)) {
      return;
    }

    try {
      int status = request.getStatus();
      if (status < 200 || (status > 299 && status < 400)) {
        return;
      }
    }
    catch (Throwable ex) {
      // ignore
    }
    String filename = null;
    if (body instanceof Resource resource) {
      filename = resource.getName();
    }

    String pathParams = "";
    if (filename == null) {
      String requestUri = request.getRequestURI();

      int index = requestUri.lastIndexOf('/') + 1;
      filename = requestUri.substring(index);

      index = filename.indexOf(';');
      if (index != -1) {
        pathParams = filename.substring(index);
        filename = filename.substring(0, index);
      }

      filename = UriUtils.decode(filename, StandardCharsets.UTF_8);
    }

    String ext = StringUtils.getFilenameExtension(filename);

    pathParams = UriUtils.decode(pathParams, StandardCharsets.UTF_8);
    String extInPathParams = StringUtils.getFilenameExtension(pathParams);

    if (notSafeExtension(request, ext) || notSafeExtension(request, extInPathParams)) {
      request.addHeader(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
    }
  }

  private boolean notSafeExtension(RequestContext request, @Nullable String extension) {
    if (StringUtils.isBlank(extension)) {
      return false;
    }
    extension = extension.toLowerCase(Locale.ROOT);
    if (safeExtensions.contains(extension)) {
      return false;
    }

    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      PathPattern bestMatchingPattern = matchingMetadata.getBestMatchingPattern();
      if (bestMatchingPattern.getPatternString().endsWith("." + extension)) {
        return false;
      }
      if (extension.equals("html")) {
        var mediaTypes = matchingMetadata.getProducibleMediaTypes();
        if (CollectionUtils.isNotEmpty(mediaTypes) && mediaTypes.contains(MediaType.TEXT_HTML)) {
          return false;
        }
      }
    }

    MediaType mediaType = MediaType.fromFileName("file." + extension);
    return mediaType == null || !safeMediaType(mediaType);
  }

  private boolean safeMediaType(MediaType mediaType) {
    return SAFE_MEDIA_BASE_TYPES.contains(mediaType.getType())
            || mediaType.getSubtype().endsWith("+xml");
  }

}
