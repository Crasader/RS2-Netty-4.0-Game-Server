package core.net.codec.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import core.game.util.StatefulFrameDecoder;

public class LoginDecoder extends StatefulFrameDecoder<LoginDecoderState> {

	public LoginDecoder() {
		super(LoginDecoderState.LOGIN_HANDSHAKE);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out, LoginDecoderState state) throws Exception {
		
	}
	
}
