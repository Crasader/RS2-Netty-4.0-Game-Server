package core.net;

import java.util.List;
import java.util.logging.Logger;

import core.game.util.LoggerUtils;
import core.net.codec.login.LoginEncoder;
import core.net.codec.update.UpdateDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class HandshakeDecoder extends ByteToMessageDecoder {
	
	private static final Logger logger = LoggerUtils.getLogger(HandshakeDecoder.class);

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer,
			List<Object> out) throws Exception {
		if (!buffer.isReadable()) {
			return;
		}
		
		int id = buffer.readUnsignedByte();
		
		switch (id) {

		case HandshakeConstants.SERVICE_GAME:
			ctx.pipeline().addFirst("loginEncoder", new LoginEncoder());
			ctx.pipeline().addAfter("handshakeDecoder", "loginDecoder", new UpdateDecoder());
			break;
		
		default:
			ByteBuf data = buffer.readBytes(buffer.readableBytes());
			logger.info(String.format("Unexpected handshake request received: %d data: %s", id, data.toString()));
			return;			
		}
		ctx.pipeline().remove(this);
		out.add(new HandshakeMessage(id));		
	}
}
