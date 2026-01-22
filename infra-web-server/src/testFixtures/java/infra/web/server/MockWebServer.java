package infra.web.server;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 16:25
 */
public class MockWebServer implements WebServer {

  private final int port;

  public MockWebServer(int port) {
    this.port = port;
  }

  @Override
  public void start() throws WebServerException {

  }

  @Override
  public void stop() throws WebServerException {

  }

  @Override
  public int getPort() {
    return port;
  }

}
