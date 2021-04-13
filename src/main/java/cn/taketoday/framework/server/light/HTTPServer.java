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

package cn.taketoday.framework.server.light;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import cn.taketoday.framework.Constant;
import cn.taketoday.web.handler.DispatcherHandler;

import static cn.taketoday.framework.server.light.Utils.join;
import static cn.taketoday.framework.server.light.Utils.splitElements;
import static cn.taketoday.framework.server.light.Utils.transfer;

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
 * <p>
 * For an example and a good starting point for learning how to use the API,
 * see the  {@link HTTPServerTests#main(String[])} method at the bottom of the file, and follow
 * the code into the API from there. Alternatively, you can just browse through
 * the classes and utility methods and read their documentation and code.
 *
 * @author Amichai Rothman
 * @see <a href='https://www.freeutils.net/source/jlhttp/'>JLHTTP - Java Lightweight HTTP Server (Web Server)</a>
 * @since 2008-07-24
 */
public class HTTPServer {

  /** A convenience array containing the carriage-return and line feed chars. */
  public static final byte[] CRLF = { 0x0d, 0x0a };

  private DispatcherHandler httpHandler;

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
                  handleConnection(socket.getInputStream(), socket.getOutputStream());
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
              catch (IOException ignore) {}
            }
          }

          executor.execute(new AcceptRunnable(sock));
        }
      }
      catch (IOException ignore) {}
    }
  }

  protected volatile int port;
  protected volatile int socketTimeout = 10000;
  protected volatile boolean secure;
  protected volatile ServerSocketFactory serverSocketFactory;
  protected volatile Executor executor;
  protected volatile ServerSocket serv;
  protected final Map<String, VirtualHost> hosts = new ConcurrentHashMap<>();

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
   * @param port
   *         the port on which this server will accept connections
   */
  public HTTPServer(int port) {
    setPort(port);
    addVirtualHost(new VirtualHost(null)); // add default virtual host
  }

  /**
   * Sets the port on which this server will accept connections.
   *
   * @param port
   *         the port on which this server will accept connections
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
   * @param factory
   *         the server socket factory to use
   */
  public void setServerSocketFactory(ServerSocketFactory factory) {
    this.serverSocketFactory = factory;
    this.secure = factory instanceof SSLServerSocketFactory;
  }

  /**
   * Sets the socket timeout for established connections.
   *
   * @param timeout
   *         the socket timeout in milliseconds
   */
  public void setSocketTimeout(int timeout) {
    this.socketTimeout = timeout;
  }

  /**
   * Sets the executor used in servicing HTTP connections.
   * If null, a default executor is used. The caller is responsible
   * for shutting down the provided executor when necessary.
   *
   * @param executor
   *         the executor to use
   */
  public void setExecutor(Executor executor) {
    this.executor = executor;
  }

  /**
   * Returns the virtual host with the given name.
   *
   * @param name
   *         the name of the virtual host to return,
   *         or null for the default virtual host
   *
   * @return the virtual host with the given name, or null if it doesn't exist
   */
  public VirtualHost getVirtualHost(String name) {
    return hosts.get(name == null ? Constant.BLANK : name);
  }

  /**
   * Returns all virtual hosts.
   *
   * @return all virtual hosts (as an unmodifiable set)
   */
  public Set<VirtualHost> getVirtualHosts() {
    return Collections.unmodifiableSet(new HashSet<VirtualHost>(hosts.values()));
  }

  /**
   * Adds the given virtual host to the server.
   * If the host's name or aliases already exist, they are overwritten.
   *
   * @param host
   *         the virtual host to add
   */
  public void addVirtualHost(VirtualHost host) {
    String name = host.getName();
    hosts.put(name == null ? Constant.BLANK : name, host);
  }

  /**
   * Creates the server socket used to accept connections, using the configured
   * {@link #setServerSocketFactory ServerSocketFactory} and {@link #setPort port}.
   * <p>
   * Cryptic errors seen here often mean the factory configuration details are wrong.
   *
   * @return the created server socket
   *
   * @throws IOException
   *         if the socket cannot be created
   */
  protected ServerSocket createServerSocket() throws IOException {
    try (ServerSocket serv = serverSocketFactory.createServerSocket()) {
      serv.setReuseAddress(true);
      serv.bind(new InetSocketAddress(port));
      return serv;
    }
  }

  /**
   * Starts this server. If it is already started, does nothing.
   * Note: Once the server is started, configuration-altering methods
   * of the server and its virtual hosts must not be used. To modify the
   * configuration, the server must first be stopped.
   *
   * @throws IOException
   *         if the server cannot begin accepting connections
   */
  public synchronized void start() throws IOException {
    if (serv != null)
      return;
    if (serverSocketFactory == null) // assign default server socket factory if needed
      serverSocketFactory = ServerSocketFactory.getDefault(); // plain sockets
    serv = createServerSocket();
    if (executor == null) // assign default executor if needed
      executor = Executors.newCachedThreadPool(); // consumes no resources when idle
    // register all host aliases (which may have been modified)
    for (VirtualHost host : getVirtualHosts())
      for (String alias : host.getAliases())
        hosts.put(alias, host);
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
    catch (IOException ignore) {}
    serv = null;
  }

  /**
   * Handles communications for a single connection over the given streams.
   * Multiple subsequent transactions are handled on the connection,
   * until the streams are closed, an error occurs, or the request
   * contains a "Connection: close" header which explicitly requests
   * the connection be closed after the transaction ends.
   *
   * @param in
   *         the stream from which the incoming requests are read
   * @param out
   *         the stream into which the outgoing responses are written
   *
   * @throws IOException
   *         if an error occurs
   */
  protected void handleConnection(InputStream in, OutputStream out) throws IOException {
    in = new BufferedInputStream(in, 4096);
    out = new BufferedOutputStream(out, 4096);
    LightRequest req;
    Response resp;
    do {
      // create request and response and handle transaction
      req = null;
      resp = new Response(out);
      try {
        req = new LightRequest(in);
        final LightRequestContext context = new LightRequestContext(req);
        handleTransaction(req, resp);
        httpHandler.handle(context);
      }
      catch (Throwable t) {
        // TODO
        // unhandled errors (not normal error responses like 404)
        if (req == null) { // error reading request
          if (t instanceof IOException && t.getMessage().contains("missing request line"))
            break; // we're not in the middle of a transaction - so just disconnect
          resp.getHeaders().add("Connection", "close"); // about to close connection
          if (t instanceof InterruptedIOException) // e.g. SocketTimeoutException
            resp.sendError(408, "Timeout waiting for client request");
          else
            resp.sendError(400, "Invalid request: " + t.getMessage());
        }
        else if (!resp.headersSent()) { // if headers were not already sent, we can send an error response
          resp = new Response(out); // ignore whatever headers may have already been set
          resp.getHeaders().add("Connection", "close"); // about to close connection
          resp.sendError(500, "Error processing request: " + t.getMessage());
        } // otherwise just abort the connection since we can't recover
        break; // proceed to close connection
      }
      finally {
        resp.close(); // close response and flush output
      }
      // consume any leftover body data so next request can be processed
      transfer(req.getBody(), null, -1);
      // RFC7230#6.6: persist connection unless client or server close explicitly (or legacy client)
    }
    while (!"close".equalsIgnoreCase(req.getHeaders().get("Connection"))
            && !"close".equalsIgnoreCase(resp.getHeaders().get("Connection")) && req.getVersion().endsWith("1.1"));
  }

  /**
   * Handles a single transaction on a connection.
   * <p>
   * Subclasses can override this method to perform filtering on the
   * request or response, apply wrappers to them, or further customize
   * the transaction processing in some other way.
   *
   * @param req
   *         the transaction request
   * @param resp
   *         the transaction response (into which the response is written)
   *
   * @throws IOException
   *         if and error occurs
   */
  protected void handleTransaction(LightRequest req, Response resp) throws IOException {
    resp.setClientCapabilities(req);
    if (preprocessTransaction(req, resp))
      handleMethod(req, resp);
  }

  /**
   * Preprocesses a transaction, performing various validation checks
   * and required special header handling, possibly returning an
   * appropriate response.
   *
   * @param req
   *         the request
   * @param resp
   *         the response
   *
   * @return whether further processing should be performed on the transaction
   *
   * @throws IOException
   *         if an error occurs
   */
  protected boolean preprocessTransaction(LightRequest req, Response resp) throws IOException {
    Headers reqHeaders = req.getHeaders();
    // validate request
    String version = req.getVersion();
    if (version.equals("HTTP/1.1")) {
      if (!reqHeaders.contains("Host")) {
        // RFC2616#14.23: missing Host header gets 400
        resp.sendError(400, "Missing required Host header");
        return false;
      }
      // return a continue response before reading body
      String expect = reqHeaders.get("Expect");
      if (expect != null) {
        if (expect.equalsIgnoreCase("100-continue")) {
          Response tempResp = new Response(resp.getOutputStream());
          tempResp.sendHeaders(100);
          resp.getOutputStream().flush();
        }
        else {
          // RFC2616#14.20: if unknown expect, send 417
          resp.sendError(417);
          return false;
        }
      }
    }
    else if (version.equals("HTTP/1.0") || version.equals("HTTP/0.9")) {
      // RFC2616#14.10 - remove connection headers from older versions
      for (String token : splitElements(reqHeaders.get("Connection"), false))
        reqHeaders.remove(token);
    }
    else {
      resp.sendError(400, "Unknown version: " + version);
      return false;
    }
    return true;
  }

  /**
   * Handles a transaction according to the request method.
   *
   * @param req
   *         the transaction request
   * @param resp
   *         the transaction response (into which the response is written)
   *
   * @throws IOException
   *         if and error occurs
   */
  protected void handleMethod(LightRequest req, Response resp) throws IOException {
    String method = req.getMethod();
    Map<String, ContextHandler> handlers = req.getContext().getHandlers();
    // RFC 2616#5.1.1 - GET and HEAD must be supported
    if ("GET".equals(method) || handlers.containsKey(method)) {
      serve(req, resp); // method is handled by context handler (or 404)
    }
    else if (method.equals("HEAD")) { // default HEAD handler
      req.method = "GET"; // identical to a GET
      resp.setDiscardBody(true); // process normally but discard body
      serve(req, resp);
    }
    else if (method.equals("TRACE")) { // default TRACE handler
      handleTrace(req, resp);
    }
    else {
      LinkedHashSet<String> methods = new LinkedHashSet<>();
      Collections.addAll(methods, "GET", "HEAD", "TRACE", "OPTIONS"); // built-in methods
      // "*" is a special server-wide (no-context) request supported by OPTIONS
      boolean isServerOptions = req.getPath().equals("*") && method.equals("OPTIONS");
      methods.addAll(isServerOptions ? req.getVirtualHost().getMethods() : handlers.keySet());

      resp.getHeaders().add("Allow", join(", ", methods));
      if (method.equals("OPTIONS")) { // default OPTIONS handler
        resp.getHeaders().add("Content-Length", "0"); // RFC2616#9.2
        resp.sendHeaders(200);
      }
      else if (req.getVirtualHost().getMethods().contains(method)) {
        resp.sendHeaders(405); // supported by server, but not this context (nor built-in)
      }
      else {
        resp.sendError(501); // unsupported method
      }
    }
  }

  /**
   * Handles a TRACE method request.
   *
   * @param req
   *         the request
   * @param resp
   *         the response into which the content is written
   *
   * @throws IOException
   *         if an error occurs
   */
  public void handleTrace(LightRequest req, Response resp) throws IOException {
    resp.sendHeaders(200, -1, -1, null, "message/http", null);
    OutputStream out = resp.getBody();

    out.write(Utils.getBytes("TRACE ", req.getURI().toString(), " ", req.getVersion()));
    out.write(CRLF);



    req.getHeaders().writeTo(out);

    transfer(req.getBody(), out, -1);
  }

  /**
   * Serves the content for a request by invoking the context
   * handler for the requested context (path) and HTTP method.
   *
   * @param req
   *         the request
   * @param resp
   *         the response into which the content is written
   *
   * @throws IOException
   *         if an error occurs
   */
  protected void serve(LightRequest req, Response resp) throws IOException {
    // get context handler to handle request
    ContextHandler handler = req.getContext().getHandlers().get(req.getMethod());
    if (handler == null) {
      resp.sendError(404);
      return;
    }
    // serve request
    int status = 404;
    // add directory index if necessary
    String path = req.getPath();
    if (path.endsWith("/")) {
      String index = req.getVirtualHost().getDirectoryIndex();
      if (index != null) {
        req.setPath(path + index);
        status = handler.serve(req, resp);
        req.setPath(path);
      }
    }
    if (status == 404)
      status = handler.serve(req, resp);
    if (status > 0)
      resp.sendError(status);
  }

}
