package test.lihb.netty.tcp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author lihb
 */
public class NettyTcpServer {

    private static final Logger LOG = LoggerFactory.getLogger(NettyTcpServer.class);

    private static final int SO_BACKLOG = 1024;
    private static final int SO_RCVBUF = 1048576;
    private static final int SO_SNDBUF = 1048576;
    private static final int PORT = 7005;


    private void startTcpServer() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, SO_BACKLOG)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_RCVBUF, SO_RCVBUF)
                .option(ChannelOption.SO_SNDBUF, SO_SNDBUF)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.group(group, childGroup).channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(PORT))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        TcpServerHandler handler = new TcpServerHandler();
                        channel.pipeline().addLast("MessageDecoder", new MessageDecoder());
                        channel.pipeline().addLast("MessageHandler", handler);
                        channel.pipeline().addLast("MessageEncoder", new MessageEncoder());

                        channel.closeFuture().addListener((ChannelFutureListener) future -> {
                            LOG.info("channel: {} is closing...", channel);
                        });
                    }
                });

        LOG.info("tcp server start on port: {}...", PORT);

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        bootstrap.bind().sync();
    }
}
