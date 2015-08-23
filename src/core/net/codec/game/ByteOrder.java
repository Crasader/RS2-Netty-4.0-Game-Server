package core.net.codec.game;

/**
 * The enumerated type whose elements represent the possible order in which
 * bytes are written in a multiple-byte value. Also known as "endianness".
 *
 * @author blakeman8192
 */
public enum ByteOrder {
	/**
	 * Least significant byte to most significant byte.
	 */
    LITTLE,
	/**
	 * Most significant byte to least significant byte.
	 */
    BIG,
	/**
	 * Also known as the V1 order.
	 */
    MIDDLE,
	/**
	 * Also known as the V2 order.
	 */
    INVERSE_MIDDLE
}
