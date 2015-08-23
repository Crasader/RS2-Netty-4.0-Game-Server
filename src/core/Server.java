package core;
import java.util.logging.Level;
import java.util.logging.Logger;

import core.net.ChannelHandler;
import core.net.NetworkConstants;
import core.net.ServiceChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * The core class for an RS2 server
 * 
 * @author 7Winds
 */
public class Server {

	/**
	 * Logging system used to log messages for the server
	 */
	private static final Logger logger = Logger.getLogger(Server.class
			.getName());

	/**
	 * Main method for the server application
	 * 
	 * @param args
	 *            The command-line arguments
	 */
	public static void main(String[] args) {
		try {
			new Server().init();
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Error while starting the server.", t);
		}
	}

	/**
	 * Creating an empty constructor
	 */
	public Server() throws Exception {
		logger.log(Level.INFO, "RS2 Server initializing...");
	}

	/**
	 * Initializes the Server Channel Handler
	 * 
	 * @throws InterruptedException
	 */
	public void init() throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {

			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ServiceChannelInitializer(new ChannelHandler()))
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

			logger.log(Level.INFO, "Binding to port " + NetworkConstants.PORT);
			ChannelFuture f = bootstrap.bind(NetworkConstants.PORT).sync();
			logger.log(Level.INFO, "Server Online and bound to port "
					+ NetworkConstants.PORT);
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}	
}
