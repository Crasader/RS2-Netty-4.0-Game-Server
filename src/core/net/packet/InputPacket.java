package core.net.packet;

/**
 * An implementation of a packet that has been sent from the client and
 * decoded by the PacketDecoder
 */
public class InputPacket implements Packet{
	
	/**
	 * The opcode of this packet.
	 */
	private final int opcode;
	
	/**
	 * The size of this packet.
	 */
	private final int size;
	
	/**
	 * The payload of this packet.
	 */
	private final PacketBuilder payload;
	
	public InputPacket(int opcode, int size, PacketBuilder payload) {
		this.opcode = opcode;
		this.size = size;
		this.payload = payload;
	}

	public int getOpcode() {
		return opcode;
	}

	public int getSize() {
		return size;
	}

	public PacketBuilder getPayload() {
		return payload;
	}
}
