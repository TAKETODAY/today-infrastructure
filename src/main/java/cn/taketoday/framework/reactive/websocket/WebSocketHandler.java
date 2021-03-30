package cn.taketoday.framework.reactive.websocket;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.reactive.NettyRequestContext;
import cn.taketoday.framework.reactive.NettyRequestContextConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;

/**
 * Processing WebSocket Request
 *
 * @author WangYi
 * @since 2020/8/13
 */
@Deprecated
public class WebSocketHandler
        extends SimpleChannelInboundHandler<Object> implements ApplicationContextAware {

  private final Map<String, WebSocketChannel> webSocketChannelMap = new ConcurrentHashMap<>(16);
  private WebSocketServerHandshaker handShaker;
  private WebSocketChannel webSocketChannel;
  private WebSocketSession webSocketSession;
  private NettyRequestContext requestContext;
  private NettyRequestContextConfig config = new NettyRequestContextConfig();

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      handleHttpRequest(ctx, (HttpRequest) msg);
    }
    else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }
    else {
      ReferenceCountUtil.retain(msg);
      ctx.fireChannelRead(msg);
    }
  }

  /**
   * Handle websocket request
   *
   * @param ctx
   *         ChannelHandlerContext
   * @param frame
   *         WebSocket Request
   */
  private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    if (frame instanceof CloseWebSocketFrame) {
      handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
      CompletableFuture.completedFuture(webSocketContext()).thenAcceptAsync(webSocketChannel::onClose);
      return;
    }

    if (frame instanceof PingWebSocketFrame) {
      ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
      return;
    }

    if (!(frame instanceof TextWebSocketFrame)) {
      final RuntimeException throwable =
              new UnsupportedOperationException("Unsupported frame type [" + frame.getClass().getName() + "]");

      this.handShaker.close(ctx.channel(), new CloseWebSocketFrame());

      WebSocketContext webSocketContext = webSocketContext();
      webSocketContext.setError(new WebSocketContext.Error(throwable, webSocketContext()));
      CompletableFuture.completedFuture(webSocketContext)
              .thenAcceptAsync(webSocketChannel::onError, ctx.executor());
      return;
    }
    WebSocketContext webSocketContext = webSocketContext();
    webSocketContext.setMessage(new WebSocketContext
            .Message(((TextWebSocketFrame) frame).text(), webSocketContext()));

    CompletableFuture.completedFuture(webSocketContext)
            .thenAcceptAsync(webSocketChannel::onMessage, ctx.executor());
  }

  /**
   * Handle http request
   *
   * @param ctx
   *         ChannelHandlerContext
   * @param request
   *         HttpRequest
   */
  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
    final DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest
            (request.protocolVersion(), request.method(), request.uri());
    this.requestContext = new NettyRequestContext("", ctx, defaultFullHttpRequest, config);
    if (isWebSocketRequest(request)) {
      final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
              request.uri(), null, true);
      this.handShaker = wsFactory.newHandshaker(request);
      if (handShaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
      }
      else {
        handShaker.handshake(ctx.channel(), request);

        long id = ThreadLocalRandom.current().nextLong();
        this.webSocketSession = new WebSocketSession(ctx, String.valueOf(id)
                .replaceFirst("-", ""));
        CompletableFuture.completedFuture(webSocketContext())
                .thenAcceptAsync(session -> webSocketChannel
                        .onConnect(session), ctx.executor());
      }
    }
    else {
      ReferenceCountUtil.retain(request);
      ctx.fireChannelRead(request);
    }
  }

  /**
   * Get WebSocket context
   *
   * @return WebSocketContext
   */
  private WebSocketContext webSocketContext() {
    return WebSocketContext.create(requestContext, webSocketSession, webSocketChannel);
  }

  /**
   * Determine if it is a websocket request
   *
   * @param req
   *         An HTTP request.
   *
   * @return Is it websocket request
   */
  private boolean isWebSocketRequest(HttpRequest req) {
    String uri = subUrlBefore(req.uri(), "?");
    return ObjectUtils.isNotEmpty(uri)
            && Objects.nonNull((this.webSocketChannel = this.webSocketChannelMap.get(uri)))
            && req.decoderResult().isSuccess()
            && "websocket".equals(req.headers().get("Upgrade"));
  }

  /**
   * Calls {@link ChannelHandlerContext#fireUserEventTriggered(Object)} to forward
   * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
   * <p>
   * Sub-classes may override this method to change behavior.
   */
  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    if (null != webSocketSession && null != webSocketChannel) {
      WebSocketContext webSocketContext = webSocketContext();
      webSocketContext.setError(new WebSocketContext.Error(cause, webSocketContext()));
      CompletableFuture.completedFuture(webSocketContext)
              .thenAcceptAsync(webSocketChannel::onError, ctx.executor());
    }
    ctx.close();
  }

  public String subUrlBefore(String str, String separator) {
    if (!StringUtils.isEmpty(str) && separator != null) {
      if (separator.length() == 0) {
        return "";
      }
      else {
        int pos = str.indexOf(separator);
        return pos == -1 ? str : str.substring(0, pos);
      }
    }
    else {
      return str;
    }
  }

  /**
   * Set the ApplicationContext that this object runs in.Normally this call will
   * be used to initialize the object.
   *
   * @param context
   */
  @SneakyThrows
  @Override
  public void setApplicationContext(ApplicationContext context) {
    List<Object> webSocketBeans = context.getAnnotatedBeans(WebSocket.class);
    if (webSocketBeans.isEmpty()) {
      return;
    }
    for (Object object : webSocketBeans) {
      Class<?> cls = object.getClass();
      Method[] declaredMethods = cls.getDeclaredMethods();
      if (!cls.isAnnotationPresent(WebSocket.class)
              || declaredMethods.length == 0) continue;
      addWebSocket(cls);
    }
  }

  /**
   * Add Web Socket implementation class
   *
   * @param websocketChannel
   */
  private void addWebSocket(Class<?> websocketChannel) throws Exception {
    WebSocket annotation = websocketChannel.getAnnotation(WebSocket.class);
    String webSocketPath = annotation.value();
    if (webSocketPath.equals("")) {
      webSocketPath = "/";
    }
    List<Class<?>> implInterface = new ArrayList<>(Arrays.asList(websocketChannel.getInterfaces()));
    if (this.webSocketChannelMap.containsKey(webSocketPath)) {
      throw new RuntimeException("Registered websocket channel URL is duplicated : " + webSocketPath);
    }
    else {
      if (implInterface.contains(WebSocketChannel.class)) {
        WebSocketChannel channel = (WebSocketChannel) websocketChannel.newInstance();
        webSocketChannelMap.put(webSocketPath, channel);
      }
      else {
        final WebSocketWrapper wrapper = new WebSocketWrapper();
        wrapper.initialize(websocketChannel);
        this.webSocketChannelMap.put(webSocketPath, wrapper);
      }
    }
  }
}
