package cn.taketoday.framework.reactive.vertx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.Map;

import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * @author TODAY 2021/5/9 22:54
 * @since 1.0.1
 */
public class VertxRequestContext extends RequestContext {

  private final HttpServerResponse response;
  private final HttpServerRequest request;

  public VertxRequestContext(HttpServerRequest request, HttpServerResponse response) {
    this.request = request;
    this.response = response;
  }

  @Override
  public String getScheme() {
    return request.scheme();
  }

  //  HttpServer
  @Override
  protected String doGetRequestPath() {
    return request.path();
  }

  @Override
  public String getRequestURL() {
    return request.uri();
  }

  @Override
  protected String doGetQueryString() {
    return request.query();
  }

  @Override
  protected HttpCookie[] doGetCookies() {
    final Map<String, Cookie> cookieMap = request.cookieMap();
    final HttpCookie[] httpCookies = new HttpCookie[cookieMap.size()];

    int i = 0;
    for (final Map.Entry<String, Cookie> entry : cookieMap.entrySet()) {
      final Cookie vertxCookie = entry.getValue();
      final HttpCookie cookie = new HttpCookie(vertxCookie.getName(), vertxCookie.getValue());
      cookie.setPath(vertxCookie.getPath());
      cookie.setDomain(vertxCookie.getDomain());
      cookie.setSecure(vertxCookie.isSecure());
      cookie.setHttpOnly(vertxCookie.isHttpOnly());
      httpCookies[i++] = cookie;
    }
    return httpCookies;
  }

  @Override
  protected void postGetParameters(MultiValueMap<String, String> parameters) {
    super.postGetParameters(parameters);
    final Future<Buffer> body = request.body();

  }

  @Override protected String doGetMethod() {
    return null;
  }

  @Override public String remoteAddress() {
    return null;
  }

  @Override public long getContentLength() {
    return 0;
  }

  @Override protected InputStream doGetInputStream() throws IOException {
    return null;
  }

  @Override protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
    return null;
  }

  @Override public String getContentType() {
    return null;
  }

  @Override protected HttpHeaders createRequestHeaders() {
    return null;
  }

  @Override public boolean committed() {
    return false;
  }

  @Override public void sendRedirect(String location) throws IOException {

  }

  @Override public void setStatus(int sc) {

  }

  @Override public void setStatus(int status, String message) {

  }

  @Override public int getStatus() {
    return 0;
  }

  @Override public void sendError(int sc) throws IOException {

  }

  @Override public void sendError(int sc, String msg) throws IOException {

  }

  @Override protected OutputStream doGetOutputStream() throws IOException {
    return null;
  }

  @Override public <T> T nativeRequest() {
    return null;
  }

  @Override public <T> T nativeRequest(Class<T> requestClass) {
    return null;
  }

  @Override public <T> T nativeResponse() {
    return null;
  }

  @Override public <T> T nativeResponse(Class<T> responseClass) {
    return null;
  }
}
