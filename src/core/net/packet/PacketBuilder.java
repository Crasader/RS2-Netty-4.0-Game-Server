package core.net.packet;

import core.net.ByteOrder;
import core.net.ValueType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * The {@link Message} implementation that functions as a dynamic buffer wrapper
 * backed by a {@link ByteBuf} that is used for reading and writing data.
 *
 * @author lare96 <http://github.com/lare96>
 * @author blakeman8192
 */
public final class PacketBuilder implements Packet {

    /**
     * An array of the bit masks used for writing bits.
     */
    private static final int[] BIT_MASK = { 0, 0x1, 0x3, 0x7, 0xf, 0x1f, 0x3f, 0x7f, 0xff, 0x1ff, 0x3ff, 0x7ff, 0xfff, 0x1fff, 0x3fff,
            0x7fff, 0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff, 0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff,
            0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, -1 };

    /**
     * The default capacity of this buffer.
     */
    private static final int DEFAULT_CAP = 128;

    /**
     * The backing byte buffer used to read and write data.
     */
    private ByteBuf buf;

    /**
     * The position of the buffer when a variable length message is created.
     */
    private int varLengthIndex = 0;

    /**
     * The current bit position when writing bits.
     */
    private int bitIndex = 0;

    /**
     * Creates a new {@link PacketBuilder} with the {@code buf} backing buffer.
     *
     * @param buf
     *            the backing buffer used to read and write data.
     */
    private PacketBuilder(ByteBuf buf) {
        this.buf = buf;
    }

    /**
     * Creates a new {@link PacketBuilder} with the {@code buf} backing buffer.
     *
     * @param buf
     *            the backing buffer used to read and write data.
     * @return the newly created buffer.
     */
    public static PacketBuilder create(ByteBuf buf) {
        return new PacketBuilder(buf);
    }

    /**
     * Creates a new {@link PacketBuilder} with the {@code cap} as the
     * capacity.
     *
     * @param cap
     *            the capacity of the buffer.
     * @return the newly created buffer.
     */
    public static PacketBuilder create(int cap) {
        return PacketBuilder.create(Unpooled.buffer(cap));
    }

    /**
     * Creates a new {@link PacketBuilder} with the default capacity.
     *
     * @return the newly created buffer.
     */
    public static PacketBuilder create() {
        return PacketBuilder.create(DEFAULT_CAP);
    }

    /**
     * Prepares the buffer for writing bits.
     */
    public void startBitAccess() {
        bitIndex = buf.writerIndex() * 8;
    }

    /**
     * Prepares the buffer for writing bytes.
     */
    public void endBitAccess() {
        buf.writerIndex((bitIndex + 7) / 8);
    }

    /**
     * Builds a new message header.
     *
     * @param opcode
     *            the opcode of the message.
     * @return an instance of this message builder.
     */
    public PacketBuilder newMessage(int opcode) {
        put(opcode);
        return this;
    }

    /**
     * Builds a new message header for a variable length message. Note that the
     * corresponding {@code endVarMessage()} method must be called to finish the
     * message.
     *
     * @param opcode
     *            the opcode of the message.
     * @return an instance of this message builder.
     */
    public PacketBuilder newVarMessage(int opcode) {
        newMessage(opcode);
        varLengthIndex = buf.writerIndex();
        put(0);
        return this;
    }

    /**
     * Builds a new message header for a variable length message, where the
     * length is written as a {@code short} instead of a {@code byte}. Note that
     * the corresponding {@code endVarShortMessage()} method must be called to
     * finish the message.
     *
     * @param opcode
     *            the opcode of the message.
     * @return an instance of this message builder.
     */
    public PacketBuilder newVarShortMessage(int opcode) {
        newMessage(opcode);
        varLengthIndex = buf.writerIndex();
        putShort(0);
        return this;
    }

    /**
     * Finishes a variable message header by writing the actual message length
     * at the length {@code byte} position. Call this when the construction of
     * the actual variable length message is complete.
     *
     * @return an instance of this message builder.
     */
    public PacketBuilder endVarMessage() {
        buf.setByte(varLengthIndex, (byte) (buf.writerIndex() - varLengthIndex - 1));
        return this;
    }

    /**
     * Finishes a variable message header by writing the actual message length
     * at the length {@code short} position. Call this when the construction of
     * the actual variable length message is complete.
     *
     * @return an instance of this message builder.
     */
    public PacketBuilder endVarShortMessage() {
        buf.setShort(varLengthIndex, (short) (buf.writerIndex() - varLengthIndex - 2));
        return this;
    }

    /**
     * Writes the bytes from the argued buffer into this buffer. This method
     * does not modify the argued buffer, and please do not flip the buffer
     * beforehand.
     *
     * @param from
     *            the argued buffer that bytes will be written from.
     * @return an instance of this message builder.
     */
    public PacketBuilder putBytes(ByteBuf from) {
        for (int i = 0; i < from.writerIndex(); i++) {
            put(from.getByte(i));
        }
        return this;
    }

    /**
     * Writes the bytes from the argued buffer into this buffer.
     *
     * @param from
     *            the argued buffer that bytes will be written from.
     * @return an instance of this message builder.
     */
    public PacketBuilder putBytes(byte[] from, int size) {
        buf.writeBytes(from, 0, size);
        return this;
    }

    /**
     * Writes the bytes from the argued byte array into this buffer, in reverse.
     *
     * @param data
     *            the data to write to this buffer.
     */
    public PacketBuilder putBytesReverse(byte[] data) {
        for (int i = data.length - 1; i >= 0; i--) {
            put(data[i]);
        }
        return this;
    }

    /**
     * Writes the value as a variable amount of bits.
     *
     * @param amount
     *            the amount of bits to write.
     * @param value
     *            the value of the bits.
     * @return an instance of this message builder.
     * @throws IllegalArgumentException
     *             if the number of bits is not between {@code 1} and {@code 32}
     *             inclusive.
     */
    public PacketBuilder putBits(int amount, int value) {
        if (amount < 0 || amount > 32)
            throw new IllegalArgumentException("Number of bits must be " + "between 1 and 32 inclusive.");
        int bytePos = bitIndex >> 3;
        int bitOffset = 8 - (bitIndex & 7);
        bitIndex = bitIndex + amount;
        int requiredSpace = bytePos - buf.writerIndex() + 1;
        requiredSpace += (amount + 7) / 8;
        if (buf.writableBytes() < requiredSpace) {
            ByteBuf old = buf;
            buf = Unpooled.buffer(old.capacity() + requiredSpace);
            buf.writeBytes(old);
        }
        for (; amount > bitOffset; bitOffset = 8) {
            byte tmp = buf.getByte(bytePos);
            tmp &= ~BIT_MASK[bitOffset];
            tmp |= (value >> (amount - bitOffset)) & BIT_MASK[bitOffset];
            buf.setByte(bytePos++, tmp);
            amount -= bitOffset;
        }
        if (amount == bitOffset) {
            byte tmp = buf.getByte(bytePos);
            tmp &= ~BIT_MASK[bitOffset];
            tmp |= value & BIT_MASK[bitOffset];
            buf.setByte(bytePos, tmp);
        } else {
            byte tmp = buf.getByte(bytePos);
            tmp &= ~(BIT_MASK[amount] << (bitOffset - amount));
            tmp |= (value & BIT_MASK[amount]) << (bitOffset - amount);
            buf.setByte(bytePos, tmp);
        }
        return this;
    }

    /**
     * Writes a boolean bit flag.
     *
     * @param flag
     *            the flag to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder putBit(boolean flag) {
        putBits(1, flag ? 1 : 0);
        return this;
    }

    /**
     * Writes a value as a {@code byte}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @return an instance of this message builder.
     */
    public PacketBuilder put(int value, ValueType type) {
        switch (type) {
        case A:
            value += 128;
            break;
        case C:
            value = -value;
            break;
        case S:
            value = 128 - value;
            break;
        case STANDARD:
            break;
        }
        buf.writeByte((byte) value);
        return this;
    }

    /**
     * Writes a value as a normal {@code byte}.
     *
     * @param value
     *            the value to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder put(int value) {
        put(value, ValueType.STANDARD);
        return this;
    }

    /**
     * Writes a value as a {@code short}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return an instance of this message builder.
     * @throws IllegalArgumentExcpetion
     *             if middle or inverse-middle value types are selected.
     */
    public PacketBuilder putShort(int value, ValueType type, ByteOrder order) {
        switch (order) {
        case BIG:
            put(value >> 8);
            put(value, type);
            break;
        case MIDDLE:
            throw new IllegalArgumentException("Middle-endian short is " + "impossible!");
        case INVERSE_MIDDLE:
            throw new IllegalArgumentException("Inverse-middle-endian " + "short is impossible!");
        case LITTLE:
            put(value, type);
            put(value >> 8);
            break;
        }
        return this;
    }

    /**
     * Writes a value as a normal big-endian {@code short}.
     *
     * @param value
     *            the value to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder putShort(int value) {
        putShort(value, ValueType.STANDARD, ByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code short}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @return an instance of this message builder.
     */
    public PacketBuilder putShort(int value, ValueType type) {
        putShort(value, type, ByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code short}.
     *
     * @param value
     *            the value to write.
     * @param order
     *            the byte order.
     * @return an instance of this message builder.
     */
    public PacketBuilder putShort(int value, ByteOrder order) {
        putShort(value, ValueType.STANDARD, order);
        return this;
    }

    /**
     * Writes a value as an {@code int}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return an instance of this message builder.
     */
    public PacketBuilder putInt(int value, ValueType type, ByteOrder order) {
        switch (order) {
        case BIG:
            put(value >> 24);
            put(value >> 16);
            put(value >> 8);
            put(value, type);
            break;
        case MIDDLE:
            put(value >> 8);
            put(value, type);
            put(value >> 24);
            put(value >> 16);
            break;
        case INVERSE_MIDDLE:
            put(value >> 16);
            put(value >> 24);
            put(value, type);
            put(value >> 8);
            break;
        case LITTLE:
            put(value, type);
            put(value >> 8);
            put(value >> 16);
            put(value >> 24);
            break;
        }
        return this;
    }

    /**
     * Writes a value as a standard big-endian {@code int}.
     *
     * @param value
     *            the value to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder putInt(int value) {
        putInt(value, ValueType.STANDARD, ByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code int}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @return an instance of this message builder.
     */
    public PacketBuilder putInt(int value, ValueType type) {
        putInt(value, type, ByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code int}.
     *
     * @param value
     *            the value to write.
     * @param order
     *            the byte order.
     * @return an instance of this message builder.
     */
    public PacketBuilder putInt(int value, ByteOrder order) {
        putInt(value, ValueType.STANDARD, order);
        return this;
    }

    /**
     * Writes a value as a {@code long}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return an instance of this message builder.
     * @throws UnsupportedOperationException
     *             if middle or inverse-middle value types are selected.
     */
    public PacketBuilder putLong(long value, ValueType type, ByteOrder order) {
        switch (order) {
        case BIG:
            put((int) (value >> 56));
            put((int) (value >> 48));
            put((int) (value >> 40));
            put((int) (value >> 32));
            put((int) (value >> 24));
            put((int) (value >> 16));
            put((int) (value >> 8));
            put((int) value, type);
            break;
        case MIDDLE:
            throw new UnsupportedOperationException("Middle-endian long " + "is not implemented!");
        case INVERSE_MIDDLE:
            throw new UnsupportedOperationException("Inverse-middle-endian long is not implemented!");
        case LITTLE:
            put((int) value, type);
            put((int) (value >> 8));
            put((int) (value >> 16));
            put((int) (value >> 24));
            put((int) (value >> 32));
            put((int) (value >> 40));
            put((int) (value >> 48));
            put((int) (value >> 56));
            break;
        }
        return this;
    }

    /**
     * Writes a value as a standard big-endian {@code long}.
     *
     * @param value
     *            the value to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder putLong(long value) {
        putLong(value, ValueType.STANDARD, ByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code long}.
     *
     * @param value
     *            the value to write.
     * @param type
     *            the value type.
     * @return an instance of this message builder.
     */
    public PacketBuilder putLong(long value, ValueType type) {
        putLong(value, type, ByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code long}.
     *
     * @param value
     *            the value to write.
     * @param order
     *            the byte order to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder putLong(long value, ByteOrder order) {
        putLong(value, ValueType.STANDARD, order);
        return this;
    }

    /**
     * Writes a RuneScape {@code String} value.
     *
     * @param string
     *            the string to write.
     * @return an instance of this message builder.
     */
    public PacketBuilder putString(String string) {
        for (byte value : string.getBytes()) {
            put(value);
        }
        put(10);
        return this;
    }

    /**
     * Reads a value as a {@code byte}.
     *
     * @param signed
     *            if the byte is signed.
     * @param type
     *            the value type.
     * @return the value of the byte.
     */
    public int get(boolean signed, ValueType type) {
        int value = buf.readByte();
        switch (type) {
        case A:
            value = value - 128;
            break;
        case C:
            value = -value;
            break;
        case S:
            value = 128 - value;
            break;
        case STANDARD:
            break;
        }
        return signed ? value : value & 0xff;
    }

    /**
     * Reads a standard signed {@code byte}.
     *
     * @return the value of the byte.
     */
    public int get() {
        return get(true, ValueType.STANDARD);
    }

    /**
     * Reads a standard {@code byte}.
     *
     * @param signed
     *            if the byte is signed.
     * @return the value of the byte.
     */
    public int get(boolean signed) {
        return get(signed, ValueType.STANDARD);
    }

    /**
     * Reads a signed {@code byte}.
     *
     * @param type
     *            the value type.
     * @return the value of the byte.
     */
    public int get(ValueType type) {
        return get(true, type);
    }

    /**
     * Reads a {@code short} value.
     *
     * @param signed
     *            if the short is signed.
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return the value of the short.
     * @throws UnsupportedOperationException
     *             if middle or inverse-middle value types are selected.
     */
    public int getShort(boolean signed, ValueType type, ByteOrder order) {
        int value = 0;
        switch (order) {
        case BIG:
            value |= get(false) << 8;
            value |= get(false, type);
            break;
        case MIDDLE:
            throw new UnsupportedOperationException("Middle-endian short " + "is impossible!");
        case INVERSE_MIDDLE:
            throw new UnsupportedOperationException("Inverse-middle-endian short is impossible!");
        case LITTLE:
            value |= get(false, type);
            value |= get(false) << 8;
            break;
        }
        return signed ? value : value & 0xffff;
    }

    /**
     * Reads a standard signed big-endian {@code short}.
     *
     * @return the value of the short.
     */
    public int getShort() {
        return getShort(true, ValueType.STANDARD, ByteOrder.BIG);
    }

    /**
     * Reads a standard big-endian {@code short}.
     *
     * @param signed
     *            if the short is signed.
     * @return the value of the short.
     */
    public int getShort(boolean signed) {
        return getShort(signed, ValueType.STANDARD, ByteOrder.BIG);
    }

    /**
     * Reads a signed big-endian {@code short}.
     *
     * @param type
     *            the value type.
     * @return the value of the short.
     */
    public int getShort(ValueType type) {
        return getShort(true, type, ByteOrder.BIG);
    }

    /**
     * Reads a big-endian {@code short}.
     *
     * @param signed
     *            if the short is signed.
     * @param type
     *            the value type.
     * @return the value of the short.
     */
    public int getShort(boolean signed, ValueType type) {
        return getShort(signed, type, ByteOrder.BIG);
    }

    /**
     * Reads a signed standard {@code short}.
     *
     * @param order
     *            the byte order.
     * @return the value of the short.
     */
    public int getShort(ByteOrder order) {
        return getShort(true, ValueType.STANDARD, order);
    }

    /**
     * Reads a standard {@code short}.
     *
     * @param signed
     *            if the short is signed.
     * @param order
     *            the byte order.
     * @return the value of the short.
     */
    public int getShort(boolean signed, ByteOrder order) {
        return getShort(signed, ValueType.STANDARD, order);
    }

    /**
     * Reads a signed {@code short}.
     *
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return the value of the short.
     */
    public int getShort(ValueType type, ByteOrder order) {
        return getShort(true, type, order);
    }

    /**
     * Reads an {@code int}.
     *
     * @param signed
     *            if the integer is signed.
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return the value of the integer.
     */
    public int getInt(boolean signed, ValueType type, ByteOrder order) {
        long value = 0;
        switch (order) {
        case BIG:
            value |= get(false) << 24;
            value |= get(false) << 16;
            value |= get(false) << 8;
            value |= get(false, type);
            break;
        case MIDDLE:
            value |= get(false) << 8;
            value |= get(false, type);
            value |= get(false) << 24;
            value |= get(false) << 16;
            break;
        case INVERSE_MIDDLE:
            value |= get(false) << 16;
            value |= get(false) << 24;
            value |= get(false, type);
            value |= get(false) << 8;
            break;
        case LITTLE:
            value |= get(false, type);
            value |= get(false) << 8;
            value |= get(false) << 16;
            value |= get(false) << 24;
            break;
        }
        return (int) (signed ? value : value & 0xffffffffL);
    }

    /**
     * Reads a signed standard big-endian {@code int}.
     *
     * @return the value of the integer.
     */
    public int getInt() {
        return getInt(true, ValueType.STANDARD, ByteOrder.BIG);
    }

    /**
     * Reads a standard big-endian {@code int}.
     *
     * @param signed
     *            if the integer is signed.
     * @return the value of the integer.
     */
    public int getInt(boolean signed) {
        return getInt(signed, ValueType.STANDARD, ByteOrder.BIG);
    }

    /**
     * Reads a signed big-endian {@code int}.
     *
     * @param type
     *            the value type.
     * @return the value of the integer.
     */
    public int getInt(ValueType type) {
        return getInt(true, type, ByteOrder.BIG);
    }

    /**
     * Reads a big-endian {@code int}.
     *
     * @param signed
     *            if the integer is signed.
     * @param type
     *            the value type.
     * @return the value of the integer.
     */
    public int getInt(boolean signed, ValueType type) {
        return getInt(signed, type, ByteOrder.BIG);
    }

    /**
     * Reads a signed standard {@code int}.
     *
     * @param order
     *            the byte order.
     * @return the value of the integer.
     */
    public int getInt(ByteOrder order) {
        return getInt(true, ValueType.STANDARD, order);
    }

    /**
     * Reads a standard {@code int}.
     *
     * @param signed
     *            if the integer is signed.
     * @param order
     *            the byte order.
     * @return the value of the integer.
     */
    public int getInt(boolean signed, ByteOrder order) {
        return getInt(signed, ValueType.STANDARD, order);
    }

    /**
     * Reads a signed {@code int}.
     *
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return the value of the integer.
     */
    public int getInt(ValueType type, ByteOrder order) {
        return getInt(true, type, order);
    }

    /**
     * Reads a signed {@code long} value.
     *
     * @param type
     *            the value type.
     * @param order
     *            the byte order.
     * @return the value of the long.
     * @throws UnsupportedOperationException
     *             if middle or inverse-middle value types are selected.
     */
    public long getLong(ValueType type, ByteOrder order) {
        long value = 0;
        switch (order) {
        case BIG:
            value |= (long) get(false) << 56L;
            value |= (long) get(false) << 48L;
            value |= (long) get(false) << 40L;
            value |= (long) get(false) << 32L;
            value |= (long) get(false) << 24L;
            value |= (long) get(false) << 16L;
            value |= (long) get(false) << 8L;
            value |= get(false, type);
            break;
        case INVERSE_MIDDLE:
        case MIDDLE:
            throw new UnsupportedOperationException("Middle and " + "inverse-middle value types not supported!");
        case LITTLE:
            value |= get(false, type);
            value |= (long) get(false) << 8L;
            value |= (long) get(false) << 16L;
            value |= (long) get(false) << 24L;
            value |= (long) get(false) << 32L;
            value |= (long) get(false) << 40L;
            value |= (long) get(false) << 48L;
            value |= (long) get(false) << 56L;
            break;
        }
        return value;
    }

    /**
     * Reads a signed standard big-endian {@code long}.
     *
     * @return the value of the long.
     */
    public long getLong() {
        return getLong(ValueType.STANDARD, ByteOrder.BIG);
    }

    /**
     * Reads a signed big-endian {@code long}.
     *
     * @param type
     *            the value type
     * @return the value of the long.
     */
    public long getLong(ValueType type) {
        return getLong(type, ByteOrder.BIG);
    }

    /**
     * Reads a signed standard {@code long}.
     *
     * @param order
     *            the byte order
     * @return the value of the long.
     */
    public long getLong(ByteOrder order) {
        return getLong(ValueType.STANDARD, order);
    }

    /**
     * Reads a RuneScape {@code String} value.
     *
     * @return the value of the string.
     */
    public String getString() {
        byte temp;
        StringBuilder b = new StringBuilder();
        while ((temp = (byte) get()) != 10) {
            b.append((char) temp);
        }
        return b.toString();
    }

    /**
     * Reads the amount of bytes into the array, starting at the current
     * position.
     *
     * @param amount
     *            the amount to read.
     * @return a buffer filled with the data.
     */
    public byte[] getBytes(int amount) {
        return getBytes(amount, ValueType.STANDARD);
    }

    /**
     * Reads the amount of bytes into a byte array, starting at the current
     * position.
     *
     * @param amount
     *            the amount of bytes.
     * @param type
     *            the value type of each byte.
     * @return a buffer filled with the data.
     */
    public byte[] getBytes(int amount, ValueType type) {
        byte[] data = new byte[amount];
        for (int i = 0; i < amount; i++) {
            data[i] = (byte) get(type);
        }
        return data;
    }

    /**
     * Reads the amount of bytes from the buffer in reverse, starting at
     * {@code current_position + amount} and reading in reverse until the
     * current position.
     *
     * @param amount
     *            the amount of bytes to read.
     * @param type
     *            the value type of each byte.
     * @return a buffer filled with the data.
     */
    public byte[] getBytesReverse(int amount, ValueType type) {
        byte[] data = new byte[amount];
        int dataPosition = 0;
        for (int i = buf.readerIndex() + amount - 1; i >= buf.readerIndex(); i--) {
            int value = buf.getByte(i);
            switch (type) {
            case A:
                value -= 128;
                break;
            case C:
                value = -value;
                break;
            case S:
                value = 128 - value;
                break;
            case STANDARD:
                break;
            }
            data[dataPosition++] = (byte) value;
        }
        return data;
    }

    /**
     * Gets the backing byte buffer used to read and write data.
     *
     * @return the backing byte buffer.
     */
    public ByteBuf buffer() {
        return buf;
    }
}
