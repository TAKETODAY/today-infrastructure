/*
 *  Copyright © 2005-2019 Amichai Rothman
 *
 *  This file is part of JLHTTP - the Java Lightweight HTTP Server.
 *
 *  JLHTTP is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  JLHTTP is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JLHTTP.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  For additional info see http://www.freeutils.net/source/jlhttp/
 */

package cn.taketoday.web.framework.server.light;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.DispatcherHandler;

import static cn.taketoday.web.framework.server.light.Utils.splitElements;
import static cn.taketoday.web.framework.server.light.Utils.transfer;

/**
 * The {@code HTTPServer} class implements a light-weight HTTP server.
 * <p>
 * This server implements all functionality required by RFC 2616 ("Hypertext
 * Transfer Protocol -- HTTP/1.1"), as well as some of the optional
 * functionality (this is termed "conditionally compliant" in the RFC).
 * In fact, a couple of bugs in the RFC itself were discovered
 * (and fixed) during the development of this server.
 * <p>
 * <b>Feature Overview</b>
 * <ul>
 * <li>RFC compliant - correctness is not sacrificed for the sake of size</li>
 * <li>Virtual hosts - multiple domains and subdomains per server</li>
 * <li>File serving - built-in handler to serve files and folders from disk</li>
 * <li>Mime type mappings - configurable via API or a standard mime.types file</li>
 * <li>Directory index generation - enables browsing folder contents</li>
 * <li>Welcome files - configurable default filename (e.g. index.html)</li>
 * <li>All HTTP methods supported - GET/HEAD/OPTIONS/TRACE/POST/PUT/DELETE/custom</li>
 * <li>Conditional statuses - ETags and If-* header support</li>
 * <li>Chunked transfer encoding - for serving dynamically-generated data streams</li>
 * <li>Gzip/deflate compression - reduces bandwidth and download time</li>
 * <li>HTTPS - secures all server communications</li>
 * <li>Partial content - download continuation (a.k.a. byte range serving)</li>
 * <li>File upload - multipart/form-data handling as stream or iterator</li>
 * <li>Multiple context handlers - a different handler method per URL path</li>
 * <li>@Context annotations - auto-detection of context handler methods</li>
 * <li>Parameter parsing - from query string or x-www-form-urlencoded body</li>
 * <li>A single source file - super-easy to integrate into any application</li>
 * <li>Standalone - no dependencies other than the Java runtime</li>
 * <li>Small footprint - standard jar is ~50K, stripped jar is ~35K</li>
 * <li>Extensible design - easy to override, add or remove functionality</li>
 * <li>Reusable utility methods to simplify your custom code</li>
 * <li>Extensive documentation of API and implementation (&gt;40% of source lines)</li>
 * </ul>
 * <p>
 * <b>Use Cases</b>
 * <p>
 * Being a lightweight, standalone, easily embeddable and tiny-footprint
 * server, it is well-suited for
 * <ul>
 * <li>Resource-constrained environments such as embedded devices.
 *     For really extreme constraints, you can easily remove unneeded
 *     functionality to make it even smaller (and use the -Dstripped
 *     maven build option to strip away debug info, license, etc.)</li>
 * <li>Unit and integration tests - fast setup/teardown times, small overhead
 *     and simple context handler setup make it a great web server for testing
 *     client components under various server response conditions.</li>
 * <li>Embedding a web console into any headless application for
 *     administration, monitoring, or a full portable GUI.</li>
 * <li>A full-fledged standalone web server serving static files,
 *     dynamically-generated content, REST APIs, pseudo-streaming, etc.</li>
 * <li>A good reference for learning how HTTP works under the hood.</li>
 * </ul>
 * <p>
 * <b>Implementation Notes</b>
 * <p>
 * The design and implementation of this server attempt to balance correctness,
 * compliance, readability, size, features, extensibility and performance,
 * and often prioritize them in this order, but some trade-offs must be made.
 * <p>
 * This server is multithreaded in its support for multiple concurrent HTTP
 * connections, however most of its constituent classes are not thread-safe and
 * require external synchronization if accessed by multiple threads concurrently.
 * <p>
 * <b>Source Structure and Documentation</b>
 * <p>
 * This server is intentionally written as a single source file, in order to make
 * it as easy as possible to integrate into any existing project - by simply adding
 * this single file to the project sources. It does, however, aim to maintain a
 * structured and flexible design. There are no external package dependencies.
 * <p>
 * This file contains extensive documentation of its classes and methods, as
 * well as implementation details and references to specific RFC sections
 * which clarify the logic behind the code. It is recommended that anyone
 * attempting to modify the protocol-level functionality become acquainted with
 * the RFC, in order to make sure that protocol compliance is not broken.
 * <p>
 * <b>Getting Started</b>
 *
 * @author Amichai Rothman
 * @see <a href='https://www.freeutils.net/source/jlhttp/'>JLHTTP - Java Lightweight HTTP Server (Web Server)</a>
 * @since 2008-07-24
 */
public class HTTPServer {
  private static final Logger log = LoggerFactory.getLogger(HTTPServer.class);

  private DispatcherHandler httpHandler;
  private String host = "0.0.0.0";

  protected volatile int port;
  protected volatile int socketTimeout = 10000;
  protected volatile boolean secure;
  protected volatile ServerSocketFactory serverSocketFactory;
  protected volatile Executor executor;
  protected volatile ServerSocket serv;
  protected LightHttpConfig config = new LightHttpConfig();

  public void setConfig(LightHttpConfig config) {
    Assert.notNull(config, "LightHttpConfig cannot be null");
    this.config = config;
  }

  public LightHttpConfig getConfig() {
    return config;
  }

  public void setHttpHandler(DispatcherHandler httpHandler) {
    this.httpHandler = httpHandler;
  }

  public DispatcherHandler getHttpHandler() {
    return httpHandler;
  }

  /**
   * The {@code SocketHandlerThread} handles accepted sockets.
   */
  protected class SocketHandlerThread extends Thread {
    @Override
    public void run() {
      setName(getClass().getSimpleName() + "-" + port);
      try {
        ServerSocket serv = HTTPServer.this.serv; // keep local to avoid NPE when stopped
        while (serv != null && !serv.isClosed()) {
          final Socket sock = serv.accept();

          final class AcceptRunnable implements Runnable {
            final Socket sock;

            AcceptRunnable(Socket sock) {
              this.sock = sock;
            }

            @Override
            public void run() {
              final Socket socket = sock;
              try {
                try {
                  socket.setSoTimeout(socketTimeout);
                  socket.setTcpNoDelay(true); // we buffer anyway, so improve latency
                  handleConnection(socket);
                }
                finally {
                  try {
                    // RFC7230#6.6 - close socket gracefully
                    // (except SSL socket which doesn't support half-closing)
                    if (!(socket instanceof SSLSocket)) {
                      socket.shutdownOutput(); // half-close socket (only output)
                      transfer(socket.getInputStream(), null, -1); // consume input
                    }
                  }
                  finally {
                    socket.close(); // and finally close socket fully
                  }
                }
              }
              catch (IOException ignore) { }
            }
          }

          executor.execute(new AcceptRunnable(sock));
        }
      }
      catch (IOException ignore) { }
    }
  }

  /**
   * Constructs an HTTPServer which can accept connections on the default HTTP port 80.
   * Note: the {@link #start()} method must be called to start accepting connections.
   */
  public HTTPServer() {
    this(80);
  }

  /**
   * Constructs an HTTPServer which can accept connections on the given port.
   * Note: the {@link #start()} method must be called to start accepting
   * connections.
   *
   * @param port the port on which this server will accept connections
   */
  public HTTPServer(int port) {
    setPort(port);
  }

  /**
   * Sets the port on which this server will accept connections.
   *
   * @param port the port on which this server will accept connections
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Sets the factory used to create the server socket.
   * If null or not set, the default {@link ServerSocketFactory#getDefault()} is used.
   * For secure sockets (HTTPS), use an SSLServerSocketFactory instance.
   * The port should usually also be changed for HTTPS, e.g. port 443 instead of 80.
   * <p>
   * If using the default SSLServerSocketFactory returned by
   * {@link SSLServerSocketFactory#getDefault()}, the appropriate system properties
   * must be set to configure the default JSSE provider, such as
   * {@code javax.net.ssl.keyStore} and {@code javax.net.ssl.keyStorePassword}.
   *
   * @param factory the server socket factory to use
   */
  public void setServerSocketFactory(ServerSocketFactory factory) {
    this.serverSocketFactory = factory;
    this.secure = factory instanceof SSLServerSocketFactory;
  }

  /**
   * Sets the socket timeout for established connections.
   *
   * @param timeout the socket timeout in milliseconds
   */
  public void setSocketTimeout(int timeout) {
    this.socketTimeout = timeout;
  }

  /**
   * Sets the executor used in servicing HTTP connections.
   * If null, a default executor is used. The caller is responsible
   * for shutting down the provided executor when necessary.
   *
   * @param executor the executor to use
   */
  public void setExecutor(Executor executor) {
    this.executor = executor;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getHost() {
    return host;
  }

  /**
   * Creates the server socket used to accept connections, using the configured
   * {@link #setServerSocketFactory ServerSocketFactory} and {@link #setPort port}.
   * <p>
   * Cryptic errors seen here often mean the factory configuration details are wrong.
   *
   * @return the created server socket
   * @throws IOException if the socket cannot be created
   */
  protected ServerSocket createServerSocket() throws IOException {
    ServerSocket serv = serverSocketFactory.createServerSocket();
    serv.setReuseAddress(true);
    final String host = getHost();
    final InetSocketAddress address = new InetSocketAddress(host, port);
    serv.bind(address);
    return serv;
  }

  /**
   * Starts this server. If it is already started, does nothing.
   * Note: Once the server is started, configuration-altering methods
   * of the server and its virtual hosts must not be used. To modify the
   * configuration, the server must first be stopped.
   *
   * @throws IOException if the server cannot begin accepting connections
   */
  public synchronized void start() throws IOException {
    if (serv != null)
      return;
    if (serverSocketFactory == null) // assign default server socket factory if needed
      serverSocketFactory = ServerSocketFactory.getDefault(); // plain sockets
    serv = createServerSocket();
    if (executor == null) // assign default executor if needed
      executor = Executors.newCachedThreadPool(); // consumes no resources when idle
    // start handling incoming connections
    new SocketHandlerThread().start();
  }

  /**
   * Stops this server. If it is already stopped, does nothing.
   * Note that if an {@link #setExecutor Executor} was set, it must be closed separately.
   */
  public synchronized void stop() {
    try {
      if (serv != null)
        serv.close();
    }
    catch (IOException ignore) { }
    serv = null;
  }

  /**
   * Handles communications for a single connection over the given streams.
   * Multiple subsequent transactions are handled on the connection,
   * until the streams are closed, an error occurs, or the request
   * contains a "Connection: close" header which explicitly requests
   * the connection be closed after the transaction ends.
   *
   * @throws IOException if an error occurs
   */
  protected void handleConnection(Socket socket) throws IOException {
//    BufferedInputStream in = new BufferedInputStream(socket.getInputStream(), 4096);
    final ByteArrayOutputStream inString = new ByteArrayOutputStream();
    BufferedInputStream in = new BufferedInputStream(socket.getInputStream(), 4096) {

      @Override
      public synchronized int read() throws IOException {
        final int read = super.read();
        inString.write(read);
        return read;
      }

      @Override
      public synchronized int read(@NonNull byte[] b, int off, int len) throws IOException {
        final int read = super.read(b, off, len);
        inString.write(b, off, len);
        return read;
      }
    };

    final OutputStream socketOutputStream = socket.getOutputStream();
    BufferedOutputStream out = new BufferedOutputStream(socketOutputStream, 4096);
//    final ByteArrayOutputStream out = new ByteArrayOutputStream() {
//      @Override
//      public synchronized void write(byte[] b, int off, int len) {
//        super.write(b, off, len);
//        try {
//          socketOutputStream.write(b, off, len);
//        }
//        catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    };

    HttpRequest req = null;
    HttpResponse resp = new HttpResponse(out);
    InetAddress localAddress = socket.getLocalAddress();
    // create request and response and handle transaction
    try {
      req = new HttpRequest(in, socket, config);
      resp.setClientCapabilities(req);

      if (preprocessTransaction(req, resp)) {
        String method = req.getMethod();
        if ("TRACE".equals(method)) { // default TRACE handler
          handleTrace(req, resp);
        }
        else {
          WebApplicationContext webApplicationContext = httpHandler.getWebApplicationContext();
          final LightRequestContext context = new LightRequestContext(webApplicationContext, req, resp, config, localAddress);
          httpHandler.dispatch(context);
          context.sendIfNotCommitted();
        }
      }
    }
    catch (Throwable t) {
      log.error("Catch throwable", t);
      // TODO
      // unhandled errors (not normal error responses like 404)
      if (req == null) { // error reading request
        if (t instanceof IOException && t.getMessage().contains("missing request line"))
          return; // we're not in the middle of a transaction - so just disconnect
        resp.getHeaders().add("Connection", "close"); // about to close connection
        if (t instanceof InterruptedIOException) // e.g. SocketTimeoutException
          resp.sendError(HttpStatus.REQUEST_TIMEOUT, "Timeout waiting for client request");
        else
          resp.sendError(HttpStatus.BAD_REQUEST, "Invalid request: " + t.getMessage());
      }
      else if (!resp.committed()) { // if headers or status line were not already sent, we can send an error response
        resp = new HttpResponse(out); // ignore whatever headers may have already been set
        resp.getHeaders().add("Connection", "close"); // about to close connection
        resp.sendError(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing request: " + t.getMessage());
      } // otherwise just abort the connection since we can't recover
      return; // proceed to close connection
    }
    finally {
//        System.out.println(out);
//        final String name = StandardCharsets.ISO_8859_1.name();
//        System.out.println(inString.toString(name));
      System.out.println(inString);
      resp.close(); // close response and flush output
    }
  }

  /**
   * Preprocesses a transaction, performing various validation checks
   * and required special header handling, possibly returning an
   * appropriate response.
   *
   * @param req the request
   * @param resp the response
   * @return whether further processing should be performed on the transaction
   * @throws IOException if an error occurs
   */
  protected boolean preprocessTransaction(HttpRequest req, HttpResponse resp) throws IOException {
    HttpHeaders reqHeaders = req.getHeaders();
    // validate request
    String version = req.getVersion();
    if (version.equals("HTTP/1.1")) {
      if (!reqHeaders.containsKey(HttpHeaders.HOST)) {
        // RFC2616#14.23: missing Host header gets 400
        resp.sendError(HttpStatus.BAD_REQUEST, "Missing required Host header");
        return false;
      }
      // return a continue response before reading body
      String expect = reqHeaders.getFirst(HttpHeaders.EXPECT);
      if (expect != null) {
        if (expect.equalsIgnoreCase(HttpHeaders.CONTINUE)) {
          HttpResponse tempResp = new HttpResponse(resp.getOutputStream());
          tempResp.send(HttpStatus.CONTINUE);
          resp.getOutputStream().flush();
        }
        else {
          // RFC2616#14.20: if unknown expect, send 417
          resp.sendError(HttpStatus.EXPECTATION_FAILED);
          return false;
        }
      }
    }
    else if (version.equals("HTTP/1.0") || version.equals("HTTP/0.9")) {
      // RFC2616#14.10 - remove connection headers from older versions
      for (String token : splitElements(reqHeaders.getFirst(HttpHeaders.CONNECTION), false))
        reqHeaders.remove(token);
    }
    else {
      resp.sendError(HttpStatus.BAD_REQUEST, "Unknown version: " + version);
      return false;
    }
    return true;
  }

  static final byte[] TRACE_BYTES = "TRACE ".getBytes(StandardCharsets.UTF_8);

  /**
   * Handles a TRACE method request.
   *
   * @param request the request
   * @param response the response into which the content is written
   * @throws IOException if an error occurs
   */
  public void handleTrace(HttpRequest request, HttpResponse response) throws IOException {
    response.write(null);

    final OutputStream output = response.getBody();
    final Charset utf8 = StandardCharsets.UTF_8;
    output.write(TRACE_BYTES);
    output.write(request.getRequestURI().getBytes(utf8));
    output.write(HttpResponse.BLANK_SPACE);
    output.write(request.getVersion().getBytes(utf8));
    output.write(HttpResponse.CRLF);

    HttpResponse.writeHttpHeaders(request.getHeaders(), output);
    transfer(request.getBody(), output, -1);
  }

}
