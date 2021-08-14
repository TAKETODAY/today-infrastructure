package cn.taketoday.framework.reactive;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.SingletonSupplier;
import cn.taketoday.framework.Constant;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;

/**
 * To help build a {@link NettyRequestContext}
 *
 * <p>
 * User can use this class to customize {@link NettyRequestContext}
 * </p>
 *
 * @author TODAY 2021/3/30 17:46
 */
public class NettyRequestContextConfig {

  /**
   * Should Netty validate HTTP response Header values to ensure they aren't malicious.
   */
  private boolean validateHeaders = false;
  private boolean singleFieldHeaders = true;

  private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

  private Supplier<HttpHeaders> trailingHeaders;

  private ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;
  private ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;

  /**
   * response body initial size
   *
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  private int bodyInitialSize = 64;
  private Supplier<ByteBuf> responseBody;

  /**
   * {@code contextPath} just like {@code HttpServletRequest.getContextPath()}
   *
   * @see javax.servlet.http.HttpServletRequest#getContextPath()
   */
  private String contextPath;
  private Charset postRequestDecoderCharset = Constant.DEFAULT_CHARSET;

  private HttpDataFactory httpDataFactory;

  public NettyRequestContextConfig() {
    this(SingletonSupplier.of(EmptyHttpHeaders.INSTANCE));
  }

  public NettyRequestContextConfig(Supplier<HttpHeaders> trailingHeaders) {
    this(new DefaultHttpDataFactory(true), trailingHeaders);
  }

  public NettyRequestContextConfig(HttpDataFactory httpDataFactory, Supplier<HttpHeaders> trailingHeaders) {
    setTrailingHeaders(trailingHeaders);
    setHttpDataFactory(httpDataFactory);
  }

  /**
   * Should Netty validate Header values to ensure they aren't malicious.
   */
  public void setValidateHeaders(boolean validateHeaders) {
    this.validateHeaders = validateHeaders;
  }

  public void setSingleFieldHeaders(boolean singleFieldHeaders) {
    this.singleFieldHeaders = singleFieldHeaders;
  }

  public boolean isSingleFieldHeaders() {
    return singleFieldHeaders;
  }

  public boolean isValidateHeaders() {
    return validateHeaders;
  }

  public void setTrailingHeaders(Supplier<HttpHeaders> trailingHeaders) {
    Assert.notNull(trailingHeaders, "trailingHeaders Supplier cannot be null");
    this.trailingHeaders = trailingHeaders;
  }

  public Supplier<HttpHeaders> getTrailingHeaders() {
    return trailingHeaders;
  }

  public void setHttpVersion(HttpVersion httpVersion) {
    this.httpVersion = httpVersion;
  }

  public HttpVersion getHttpVersion() {
    return httpVersion;
  }

  public void setCookieDecoder(ServerCookieDecoder cookieDecoder) {
    this.cookieDecoder = cookieDecoder;
  }

  public void setCookieEncoder(ServerCookieEncoder cookieEncoder) {
    this.cookieEncoder = cookieEncoder;
  }

  public ServerCookieDecoder getCookieDecoder() {
    return cookieDecoder == null ? ServerCookieDecoder.STRICT : cookieDecoder;
  }

  public ServerCookieEncoder getCookieEncoder() {
    return cookieEncoder == null ? ServerCookieEncoder.STRICT : cookieEncoder;
  }

  public void setResponseBody(Supplier<ByteBuf> responseBody) {
    this.responseBody = responseBody;
  }

  public Supplier<ByteBuf> getResponseBody() {
    return responseBody;
  }

  /**
   * @return response body initial capacity
   *
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  public int getBodyInitialSize() {
    return bodyInitialSize;
  }

  /**
   * @param bodyInitialSize
   *         response body initial capacity
   *
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  public void setBodyInitialSize(int bodyInitialSize) {
    this.bodyInitialSize = bodyInitialSize;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  /**
   * {@code contextPath} just like {@code HttpServletRequest.getContextPath()}
   *
   * @see javax.servlet.http.HttpServletRequest#getContextPath()
   */
  public String getContextPath() {
    return contextPath;
  }

  public void setPostRequestDecoderCharset(Charset postRequestDecoderCharset) {
    this.postRequestDecoderCharset = postRequestDecoderCharset;
  }

  public Charset getPostRequestDecoderCharset() {
    return postRequestDecoderCharset;
  }

  public void setHttpDataFactory(HttpDataFactory httpDataFactory) {
    Assert.notNull(httpDataFactory, "httpDataFactory cannot be null");
    this.httpDataFactory = httpDataFactory;
  }

  public HttpDataFactory getHttpDataFactory() {
    return httpDataFactory;
  }
}
