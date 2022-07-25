package cn.taketoday.web.socket.jetty;


import cn.taketoday.web.socket.WebSocketExtension;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.common.JettyExtensionConfig;

/**
 * Adapter class to convert a {@link WebSocketExtension} to a Jetty
 * {@link ExtensionConfig}.
 *
 * @author Rossen Stoyanchev
 * @author Harry Yang 2021/11/12 17:53
 * @since 4.0
 */
public class WebSocketToJettyExtensionConfigAdapter extends JettyExtensionConfig {

  public WebSocketToJettyExtensionConfigAdapter(WebSocketExtension extension) {
    super(extension.getName());
    extension.getParameters().forEach(super::setParameter);
  }

}

