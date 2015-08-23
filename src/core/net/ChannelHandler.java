package core.net;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * An implementation of {@link ChannelInboundHandlerAdapter} which handles incoming upstream events from Netty.
 * 
 * @author 7Winds
 */
public class ChannelHandler extends ChannelInboundHandlerAdapter {
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) {
		System.out.println("User Connected: " + ctx.channel().remoteAddress());
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		System.out.println("[DEREGISTERED] : " + ctx.channel().remoteAddress());
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object message) {
		System.out.println("Message from: " + ctx.channel().remoteAddress() + " Message: " + message);
	}	
}
