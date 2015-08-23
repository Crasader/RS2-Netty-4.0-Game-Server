package core.net.codec.update;

import java.util.List;

import core.game.cache.FileDescriptor;
import core.net.codec.update.OnDemandRequest.Priority;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * A {@link ByteToMessageDecoder} for the 'on-demand' protocol.
 *
 * @author Graham
 */
public final class UpdateDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
		if (buffer.readableBytes() >= 4) {
			int type = buffer.readUnsignedByte() + 1;
			int file = buffer.readUnsignedShort();
			Priority priority = Priority.valueOf(buffer.readUnsignedByte());

			FileDescriptor desc = new FileDescriptor(type, file);
			out.add(new OnDemandRequest(desc, priority));
		}
	}
	
}
