package core.net;

import io.netty.util.AttributeKey;
import core.net.packet.InputPacketListener;

/**
 * Class which consists of network-related constants
 * @author 7Winds
 */
public class NetworkConstants {
	
	public static final int PORT = 43594;
	
    /**
     * An array of message opcodes mapped to their respective sizes.
     */
    public static final int PACKET_SIZES[] = new int[257];
    
    /**
     * An array of the message opcodes mapped to their respective listeners.
     */
    public static final InputPacketListener[] PACKETS = new InputPacketListener[257];
    
    /**
     * The {@link AttributeKey} value that is used to retrieve the session
     * instance from the attribute map of a {@link Channel}.
     */
    public static final AttributeKey<PlayerIO> SESSION_KEY = AttributeKey.valueOf("session.KEY");
	
}
