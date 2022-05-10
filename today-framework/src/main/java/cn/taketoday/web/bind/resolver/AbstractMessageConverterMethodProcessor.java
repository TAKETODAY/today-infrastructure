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

package cn.taketoday.web.bind.resolver;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceRegion;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHttpOutputMessage;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.util.UriUtils;
import cn.taketoday.web.util.pattern.PathPattern;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Extends {@link AbstractMessageConverterParameterResolver} with the ability to handle method
 * return values by writing to the response with {@link HttpMessageConverter HttpMessageConverters}.
 * <p>
 * write {@link ActionMappingAnnotationHandler} return value
 * </p>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ActionMappingAnnotationHandler
 * @since 4.0 2022/1/23 12:30
 */
public abstract class AbstractMessageConverterMethodProcessor
        extends AbstractMessageConverterParameterResolver implements ReturnValueHandler {
  private static final Logger log = LoggerFactory.getLogger(AbstractMessageConverterMethodProcessor.class);
  private static final boolean isDebugEnabled = log.isDebugEnabled();

  /* Extensions associated with the built-in message converters */
  private static final Set<String> SAFE_EXTENSIONS = Set.of(
          "txt", "text", "yml", "properties", "csv",
          "json", "xml", "atom", "rss",
          "png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp"
  );

  private static final Set<String> SAFE_MEDIA_BASE_TYPES = Set.of("audio", "image", "video");
  private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES = List.of(MediaType.ALL, new MediaType("application"));

  private static final Type RESOURCE_REGION_LIST_TYPE =
          new TypeReference<List<ResourceRegion>>() { }.getType();

  private final ContentNegotiationManager contentNegotiationManager;

  private final HashSet<String> safeExtensions = new HashSet<>();

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
    super(converters, requestResponseBodyAdvice);
    this.contentNegotiationManager = manager != null ? manager : new ContentNegotiationManager();
    this.safeExtensions.addAll(contentNegotiationManager.getAllFileExtensions());
    this.safeExtensions.addAll(SAFE_EXTENSIONS);
  }

  // ReturnValueHandler

  @Override
  public final boolean supportsHandler(Object handler) {
    if (handler instanceof HandlerMethod handlerMethod) {
      return supportsHandlerMethod(handlerMethod);
    }
    else if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      return supportsHandlerMethod(annotationHandler.getMethod());
    }
    return false;
  }

  // test HandlerMethod
  protected abstract boolean supportsHandlerMethod(HandlerMethod handlerMethod);

  @Override
  public final void handleReturnValue(
          RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      handleReturnValue(context, handlerMethod, returnValue);
    }
    else if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      handleReturnValue(context, annotationHandler.getMethod(), returnValue);
    }
  }

  protected abstract void handleReturnValue(
          RequestContext context, HandlerMethod handler, @Nullable Object returnValue) throws Exception;

  /**
   * Writes the given return type to the given output message.
   *
   * @param value the value to write to the output message
   * @param returnType the type of the value
   * @param context the output message to write to and Used to inspect the {@code Accept} header.
   * @throws java.io.IOException thrown in case of I/O errors
   * @throws cn.taketoday.web.HttpMediaTypeNotAcceptableException thrown when the conditions indicated
   * by the {@code Accept} header on the request cannot be met by the message converters
   * @throws cn.taketoday.http.converter.HttpMessageNotWritableException thrown if a given message cannot
   * be written by a converter, or if the content-type chosen by the server
   * has no compatible converter.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected <T> void writeWithMessageConverters(@Nullable T value, MethodParameter returnType, RequestContext context)
          throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

    Object body;
    Class<?> valueType;
    Type targetType;

    if (value instanceof CharSequence) {
      body = value.toString();
      valueType = String.class;
      targetType = String.class;
    }
    else {
      body = value;
      valueType = getReturnValueType(body, returnType);
      targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
    }

    HttpHeaders requestHeaders = context.requestHeaders();
    HttpHeaders responseHeaders = context.responseHeaders();
    if (isResourceType(value, returnType)) {
      responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
      if (value != null && requestHeaders.getFirst(HttpHeaders.RANGE) != null && context.getStatus() == 200) {
        Resource resource = (Resource) value;
        try {
          List<HttpRange> httpRanges = requestHeaders.getRange();
          context.setStatus(HttpStatus.PARTIAL_CONTENT.value());
          body = HttpRange.toResourceRegions(httpRanges, resource);
          valueType = body.getClass();
          targetType = RESOURCE_REGION_LIST_TYPE;
        }
        catch (IllegalArgumentException ex) {
          responseHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
          context.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
        }
      }
    }

    MediaType selectedMediaType = null;
    MediaType contentType = responseHeaders.getContentType();
    boolean isContentTypePreset = contentType != null && contentType.isConcrete();
    if (isContentTypePreset) {
      if (isDebugEnabled) {
        log.debug("Found 'Content-Type:{}' in response", contentType);
      }
      selectedMediaType = contentType;
    }
    else {
      List<MediaType> acceptableTypes;
      try {
        acceptableTypes = getAcceptableMediaTypes(context);
      }
      catch (HttpMediaTypeNotAcceptableException ex) {
        int series = context.getStatus() / 100;
        if (body == null || series == 4 || series == 5) {
          if (isDebugEnabled) {
            log.debug("Ignoring error response content (if any). {}", ex.toString());
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

      ArrayList<MediaType> mediaTypesToUse = new ArrayList<>();
      for (MediaType requestedType : acceptableTypes) {
        for (MediaType producibleType : producibleTypes) {
          if (requestedType.isCompatibleWith(producibleType)) {
            mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
          }
        }
      }
      if (mediaTypesToUse.isEmpty()) {
        if (isDebugEnabled) {
          log.debug("No match for {}, supported: {}", acceptableTypes, producibleTypes);
        }
        if (body != null) {
          throw new HttpMediaTypeNotAcceptableException(producibleTypes);
        }
        return;
      }

      MimeTypeUtils.sortBySpecificity(mediaTypesToUse);

      for (MediaType mediaType : mediaTypesToUse) {
        if (mediaType.isConcrete()) {
          selectedMediaType = mediaType;
          break;
        }
        else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
          selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
          break;
        }
      }

      if (isDebugEnabled) {
        log.debug("Using '{}', given {} and supported {}", selectedMediaType, acceptableTypes, producibleTypes);
      }
    }

    if (selectedMediaType != null) {
      RequestResponseBodyAdviceChain advice = getAdvice();
      selectedMediaType = selectedMediaType.removeQualityValue();
      for (HttpMessageConverter<?> converter : messageConverters) {
        var generic = converter instanceof GenericHttpMessageConverter
                      ? (GenericHttpMessageConverter) converter : null;
        if (generic != null ? generic.canWrite(targetType, valueType, selectedMediaType)
                            : converter.canWrite(valueType, selectedMediaType)) {

          body = advice.beforeBodyWrite(
                  body, returnType, selectedMediaType, converter, context);
          if (body != null) {
            if (isDebugEnabled) {
              Object theBody = body;
              LogFormatUtils.traceDebug(log,
                      traceOn -> "Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");
            }
            addContentDispositionHeader(context);
            if (generic != null) {
              generic.write(
                      body, targetType, selectedMediaType, new RequestContextHttpOutputMessage(context));
            }
            else {
              ((HttpMessageConverter) converter).write(
                      body, selectedMediaType, new RequestContextHttpOutputMessage(context));
            }
          }
          else {
            if (isDebugEnabled) {
              log.debug("Nothing to write: null body");
            }
          }
          return;
        }
      }
    }

    if (body != null) {
      HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
      if (matchingMetadata != null) {
        MediaType[] producibleMediaTypes = matchingMetadata.getProducibleMediaTypes();
        if (isContentTypePreset || ObjectUtils.isNotEmpty(producibleMediaTypes)) {
          throw new HttpMessageNotWritableException(
                  "No converter for [" + valueType + "] with preset Content-Type '" + contentType + "'");
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
  protected Class<?> getReturnValueType(@Nullable Object value, MethodParameter returnType) {
    return value != null ? value.getClass() : returnType.getParameterType();
  }

  /**
   * Return whether the returned value or the declared return type extends {@link Resource}.
   */
  protected boolean isResourceType(@Nullable Object value, MethodParameter returnType) {
    Class<?> clazz = getReturnValueType(value, returnType);
    return clazz != InputStreamResource.class && Resource.class.isAssignableFrom(clazz);
  }

  /**
   * Return the generic type of the {@code returnType} (or of the nested type
   * if it is an {@link HttpEntity}).
   */
  private Type getGenericType(MethodParameter returnType) {
    if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
      return ResolvableType.fromType(returnType.getGenericParameterType()).getGeneric().getType();
    }
    else {
      return returnType.getGenericParameterType();
    }
  }

  /**
   * Returns the media types that can be produced.
   *
   * @see #getProducibleMediaTypes(RequestContext, Class, Type)
   */
  @SuppressWarnings("unused")
  protected List<MediaType> getProducibleMediaTypes(RequestContext request, Class<?> valueClass) {
    return getProducibleMediaTypes(request, valueClass, null);
  }

  /**
   * Returns the media types that can be produced. The resulting media types are:
   * <ul>
   * <li>The producible media types specified in the request mappings, or
   * <li>Media types of configured converters that can write the specific return value, or
   * <li>{@link MediaType#ALL}
   * </ul>
   */
  protected List<MediaType> getProducibleMediaTypes(
          RequestContext request, Class<?> valueClass, @Nullable Type targetType) {

    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      MediaType[] mediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (ObjectUtils.isNotEmpty(mediaTypes)) {
        return Arrays.asList(mediaTypes);
      }
    }
    ArrayList<MediaType> result = new ArrayList<>();
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

  private List<MediaType> getAcceptableMediaTypes(RequestContext request)
          throws HttpMediaTypeNotAcceptableException {
    return this.contentNegotiationManager.resolveMediaTypes(request);
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
   * {@link cn.taketoday.web.accept.ContentNegotiationManager#getAllFileExtensions() registered}.
   * If not, and the status is in the 2xx range, a 'Content-Disposition'
   * header with a safe attachment file name ("f.txt") is added to prevent
   * RFD exploits.
   */
  private void addContentDispositionHeader(RequestContext request) {
    HttpHeaders headers = request.responseHeaders();
    if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
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
    String requestUri = request.getRequestPath();

    int index = requestUri.lastIndexOf('/') + 1;
    String filename = requestUri.substring(index);
    String pathParams = "";

    index = filename.indexOf(';');
    if (index != -1) {
      pathParams = filename.substring(index);
      filename = filename.substring(0, index);
    }

    filename = UriUtils.decode(filename, StandardCharsets.UTF_8);
    String ext = StringUtils.getFilenameExtension(filename);

    pathParams = UriUtils.decode(pathParams, StandardCharsets.UTF_8);
    String extInPathParams = StringUtils.getFilenameExtension(pathParams);

    if (notSafeExtension(request, ext) || notSafeExtension(request, extInPathParams)) {
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
    }
  }

  private boolean notSafeExtension(RequestContext request, @Nullable String extension) {
    if (!StringUtils.hasText(extension)) {
      return false;
    }
    extension = extension.toLowerCase(Locale.ENGLISH);
    if (safeExtensions.contains(extension)) {
      return false;
    }

    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata == null) {
      return false;
    }

    PathPattern bestMatchingPattern = matchingMetadata.getBestMatchingPattern();
    if (bestMatchingPattern != null && bestMatchingPattern.getPatternString().endsWith("." + extension)) {
      return false;
    }
    if (extension.equals("html")) {
      MediaType[] mediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (ObjectUtils.isNotEmpty(mediaTypes) && ObjectUtils.containsElement(mediaTypes, MediaType.TEXT_HTML)) {
        return false;
      }
    }
    MediaType mediaType = resolveMediaType(request, extension);
    return (mediaType == null || !safeMediaType(mediaType));
  }

  @Nullable
  private MediaType resolveMediaType(RequestContext request, String extension) {
    MediaType result = null;
    if (ServletDetector.runningInServlet(request)) {
      String rawMimeType = ServletDelegate.getMimeType(request, extension);
      if (StringUtils.hasText(rawMimeType)) {
        result = MediaType.parseMediaType(rawMimeType);
      }
    }
    if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
      result = MediaType.fromFileName("file." + extension);
    }
    return result;
  }

  private boolean safeMediaType(MediaType mediaType) {
    return SAFE_MEDIA_BASE_TYPES.contains(mediaType.getType())
            || mediaType.getSubtype().endsWith("+xml");
  }

  static class ServletDelegate {

    static String getMimeType(RequestContext request, String extension) {
      HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);
      return servletRequest.getServletContext().getMimeType("file." + extension);
    }

  }

}
