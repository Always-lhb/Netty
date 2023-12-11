package test.netty.udp.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author lihb
 */
public class NettyUdpServer {

    private static final Logger LOG = LoggerFactory.getLogger(NettyUdpServer.class);

    private static final int SO_RCVBUF = 10000000;
    private static final int SO_SNDBUF = 524288;
    private static final int PORT = 6006;

    private Channel[] channels;

    public void startUdpServer(int bindCount) throws InterruptedException {
        bindCount = bindCount <= 0 ? 1 : bindCount;

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.SO_RCVBUF, SO_RCVBUF);
        bootstrap.option(ChannelOption.SO_SNDBUF, SO_SNDBUF);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        EventLoopGroup group;
        if (Epoll.isAvailable()) {
            LOG.info("epoll is available, IS_SUPPORTING_SENDMMSG: {}",
                    Native.IS_SUPPORTING_SENDMMSG);
            group = new EpollEventLoopGroup(bindCount);
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bootstrap.group(group)
                    .channel(EpollDatagramChannel.class)
                    .localAddress(new InetSocketAddress(PORT))
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new ChannelOutboundHandlerAdapter());
                            channel.pipeline().addLast(new ChannelInboundHandlerAdapter());
                        }
                    });
        } else {
            LOG.info("epoll is unavailable, just use java nio channel");
            bindCount = 1;
            group = new NioEventLoopGroup(bindCount);
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .localAddress(new InetSocketAddress(PORT))
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new ChannelOutboundHandlerAdapter());
                            channel.pipeline().addLast(new ChannelInboundHandlerAdapter());
                        }
                    });
        }

        Channel[] tmpChannels = new Channel[bindCount];
        for (int i = 0; i < bindCount; i++) {
            ChannelFuture channelFuture = bootstrap.bind().sync();
            tmpChannels[i] = channelFuture.channel();
        }

        channels = tmpChannels;
    }
}

