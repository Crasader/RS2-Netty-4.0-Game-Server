package core.net;
import java.util.logging.Level;
import java.util.logging.Logger;

import core.game.util.LoggerUtils;
import core.net.codec.handshake.HandshakeDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * A {@link ChannelInitializer} for the service pipeline
 * 
 * @author 7Winds
 */
public class ServiceChannelInitializer extends
		ChannelInitializer<SocketChannel> {

	private final ChannelHandler handler;
	
	private static final Logger logger = LoggerUtils.getLogger(ServiceChannelInitializer.class);

	public ServiceChannelInitializer(ChannelHandler handler) {
		this.handler = handler;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();		
		ch.pipeline().addLast("handshakeDecoder", new HandshakeDecoder());
		ch.pipeline().addLast("timeout", new IdleStateHandler(NetworkConstants.IDLE_TIME, 0, 0));
		pipeline.addLast("handler", handler);
		logger.log(Level.INFO, "Connection recieved from " + ch.remoteAddress().getAddress());		
	}

}
