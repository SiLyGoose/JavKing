//package javking.rest.websocket;
//
//import io.socket.engineio.server.EngineIoServer;
//import io.socket.engineio.server.EngineIoServerOptions;
//import io.socket.engineio.server.JettyWebSocketHandler;
//import io.socket.socketio.server.SocketIoServer;
//import org.eclipse.jetty.http.pathmap.ServletPathSpec;
//import org.eclipse.jetty.server.Handler;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.handler.HandlerList;
//import org.eclipse.jetty.servlet.ServletContextHandler;
//import org.eclipse.jetty.servlet.ServletHolder;
//import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
//
//import javax.servlet.DispatcherType;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletRequestWrapper;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.EnumSet;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class ServerWrapper {
//    private static AtomicInteger PORT_START = null;
//
//    private final int mPort;
//    private final Server mServer;
//    private final EngineIoServerOptions eioOptions;
//    private final EngineIoServer mEngineIoServer;
//    private final SocketIoServer mSocketIoServer;
//
//    public ServerWrapper(String host, int port, String[] allowedCorsOrigins) {
//        PORT_START = new AtomicInteger(port);
//
//        mPort = PORT_START.getAndIncrement();
//        mServer = new Server(new InetSocketAddress(host, mPort));
//        eioOptions = EngineIoServerOptions.newFromDefault();
//        eioOptions.setAllowedCorsOrigins(allowedCorsOrigins);
//
//        mEngineIoServer = new EngineIoServer(eioOptions);
//        mSocketIoServer = new SocketIoServer(mEngineIoServer);
//
//        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
//        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
//
//        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
//        servletContextHandler.setContextPath("/");
//        servletContextHandler.addFilter(RemoteAddrFilter.class, "/socket.io/*", EnumSet.of(DispatcherType.REQUEST));
//
//        servletContextHandler.addServlet(new ServletHolder(new HttpServlet() {
//            @Override
//            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
//                mEngineIoServer.handleRequest(new HttpServletRequestWrapper(request) {
//                    @Override
//                    public boolean isAsyncSupported() {
//                        return false;
//                    }
//                }, response);
//            }
//        }), "/socket.io/*");
//
//        try {
//            WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configureContext(servletContextHandler);
//            webSocketUpgradeFilter.addMapping(
//                    new ServletPathSpec("/socket.io/*"),
//                    (servletUpgradeRequest, servletUpgradeResponse) -> new JettyWebSocketHandler(mEngineIoServer));
//        } catch (ServletException ex) {
//            ex.printStackTrace();
//        }
//
//        HandlerList handlerList = new HandlerList();
//        handlerList.setHandlers(new Handler[]{servletContextHandler});
//        mServer.setHandler(handlerList);
//    }
//
//    public void startServer() throws Exception {
//        mServer.start();
//    }
//
//    public void stopServer() throws Exception {
//        mServer.stop();
//    }
//
//    public int getPort() {
//        return mPort;
//    }
//
//    public SocketIoServer getSocketIoServer() {
//        return mSocketIoServer;
//    }
//}
