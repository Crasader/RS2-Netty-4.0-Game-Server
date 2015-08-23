package core.net.codec.game;

/**
 * The enumerated type whose values represent the possible custom RuneScape
 * value types. Type {@code A} is to add 128 to the value, type {@code C} is to
 * invert the value, and type {@code S} is to subtract the value from 128. Of
 * course, {@code STANDARD} is just the normal data value.
 *
 * @author blakeman8192
 */
public enum ValueType {
	/**
	 * No transformation is done.
	 */
    STANDARD,
	/**
	 * Adds 128 to the value when it is written, takes 128 from the value when it is read (also known as type-A).
	 */
    A,
	/**
	 * Negates the value (also known as type-C).
	 */
    C,
	/**
	 * Subtracts the value from 128 (also known as type-S).
	 */
    S
}