package cn.taketoday.framework.reactive.websocket;

import cn.taketoday.framework.reactive.NettyRequestContext;

/**
 * @author WangYi
 * @since 2020/8/13
 */
public class WebSocketContext {
    private final NettyRequestContext requestContext;
    private final WebSocketSession session;
    private final WebSocketChannel channel;
    private Message message;
    private Error error;

    private WebSocketContext(NettyRequestContext requestContext,
                             WebSocketSession session, WebSocketChannel channel) {
        this.requestContext = requestContext;
        this.session = session;
        this.channel = channel;
    }

    public static WebSocketContext create(NettyRequestContext requestContext,
                                          WebSocketSession webSocketSession,
                                          WebSocketChannel webSocketChannel) {
        return new WebSocketContext(requestContext, webSocketSession, webSocketChannel);
    }

    public WebSocketSession session() {
        return session;
    }

    public WebSocketChannel channel() {
        return channel;
    }

    public NettyRequestContext requestContext() {
        return requestContext;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public static class Message {
        private final String text;
        private final WebSocketContext context;

        Message(String text, WebSocketContext context) {
            this.text = text;
            this.context = context;
        }

        public String text() {
            return text;
        }

        public WebSocketContext context() {
            return context;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    public static class Error {
        private final Throwable cause;
        private final WebSocketContext context;

        Error(Throwable cause, WebSocketContext context) {
            this.cause = cause;
            this.context = context;
        }

        public Throwable cause() {
            return cause;
        }

        public WebSocketContext context() {
            return context;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "cause=" + cause +
                    '}';
        }
    }
}
