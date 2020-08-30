package test.demo.ws;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.framework.reactive.websocket.WebSocket;
import cn.taketoday.framework.reactive.websocket.WebSocketChannel;
import cn.taketoday.framework.reactive.websocket.WebSocketContext;

/**
 * @author WangYi
 * @since 2020/8/30
 */
@Singleton
@WebSocket(value = "/websocket")
public class WebSocketImpl implements WebSocketChannel {
    @Override
    public void onConnect(WebSocketContext webSocketContext) {
        System.out.println("websocket open connect");
    }

    @Override
    public void onMessage(WebSocketContext webSocketContext) {
        System.out.println(webSocketContext.getMessage().text());
    }

    @Override
    public void onClose(WebSocketContext webSocketContext) {
        System.out.println("关闭连接:" + webSocketContext.session().getId());
    }

    @Override
    public void onError(WebSocketContext webSocketContext) {

    }
}
