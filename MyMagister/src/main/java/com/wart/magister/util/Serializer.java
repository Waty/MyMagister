package com.wart.magister.util;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Serializer {

    public static final String TAG = "Serializer";
    static final byte[] ROHeader = {82, 79, 49, 48, 55};
    private static final double TIME_OFFSET_MEDIUS = 25569.0D;
    private static int daylightOffset = -1;
    private static int lastTimezoneOffset = -1;
    private static TimeZone timezone = null;
    private static int timezoneOffset = -1;
    public int pos = 0;
    private byte[] buffer = null;

    public Serializer() {
        buffer = null;
    }

    public Serializer(byte[] buffer) {
        this.buffer = buffer;
    }

    public Serializer(int size) {
        buffer = new byte[size];
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static int DateTimeAsIntValue(Date value) {
        if (!value.equals(Global.NODATE)) {
            initializeOffsets(value);
            return (int) Math.floor(TIME_OFFSET_MEDIUS + (value.getTime() + daylightOffset) / 86400000L);
        }
        return 0;
    }

    private static void initializeOffsets(Date date) {
        Calendar cal = Calendar.getInstance();
        if (lastTimezoneOffset != (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000) {
            lastTimezoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
            cal.setTime(date);
            timezone = cal.getTimeZone();
            timezoneOffset = cal.get(15);
            daylightOffset = cal.get(16);
        }
    }

    static VariantType VarType(Object value) {
        if (value == null) return VariantType.Null;
        if (value instanceof Date) return VariantType.Date;
        if (value instanceof String) return VariantType.String;
        if (value instanceof Boolean) return VariantType.Boolean;
        if (value instanceof Short) return VariantType.ShortInt;
        if (value instanceof Byte) return VariantType.Byte;
        if (value instanceof Short) return VariantType.SmallInt;
        if (value instanceof Integer) return VariantType.Integer;
        if (value instanceof Long) return VariantType.Int64;
        if (value instanceof Float) return VariantType.Single;
        if (value instanceof Double) return VariantType.Double;
        return VariantType.Object;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        pos = 0;
        this.buffer = buffer;
    }

    public int getBufferLength() {
        if (buffer == null) return 0;
        return buffer.length;
    }

    private byte getFieldType(Type type) {
        Type[] intTypes = {Integer.class, Integer.TYPE, Long.TYPE, Short.TYPE, Byte.TYPE};

        if (type == Boolean.TYPE || type == Boolean.class) return 1;
        else if (type == String.class) return 2;
        else if (type == Date.class) return 3;
        else if (type == Float.TYPE || type == Double.class) return 5;

        for (Type intType : intTypes)
            if (type == intType) return 4;
        return 0;
    }

    public String getLastError() {
        try {
            return Global.toDBString(readString());
        } catch (Exception localException) {
            System.out.println("Foutmelding: " + localException.getMessage());
        }
        return "";
    }

    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public byte readByte() throws IOException {
        pos++;
        return buffer[pos - 1];
    }

    public DataTable readDataTable() {
        DataTable result = new DataTable();
        try {
            result.TableName = readString();
            if (Global.isNullOrEmpty(result.TableName)) result.TableName = "Unknown";

            int fields = readInteger();
            if (fields != 0) {
                for (int i = 0; i < fields; i++) {
                    String fieldName = readString();
                    Type fieldType = readType();
                    readInteger();
                    readString();
                    readInteger();
                    readByte();
                    readByte();
                    readString();
                    result.Columns.add(new DataColumn(fieldName, fieldType));
                }
                int rows = readInteger();
                for (int i = 0; i < rows; i++) {
                    DataRow row = new DataRow();
                    readInteger();
                    for (int j = 0; j < fields; j++)
                        row.put(result.Columns.get(j).Name, readVariant());
                    result.add(row);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not read table");
            e.printStackTrace();
        }
        return result;
    }

    public Date readDateTime() throws IOException {
        long ticks = (long) Math.ceil(86400000.0D * (readDouble() - 25569.0D));
        initializeOffsets(new Date(ticks));
        long l2 = ticks - timezoneOffset;
        Date d = new Date(l2);
        if (timezone.inDaylightTime(d)) d = new Date(l2 - daylightOffset);
        return d;
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readInt64());
    }

    public int readInt32() throws IOException {
        pos = 4 + pos;
        return 0xFF & buffer[-4 + pos] | (0xFF & buffer[-3 + pos]) << 8 | (0xFF & buffer[-2 + pos]) << 16 | (0xFF & buffer[-1 + pos]) << 24;
    }

    public long readInt64() throws IOException {
        return readInt32() << 32 | readInt32();
    }

    public int readInteger() throws IOException {
        return readInt32();
    }

    public boolean readROBoolean() throws IOException {
        return readInt32() != 0;
    }

    public boolean readROHeader(final byte[] array, final String s, final String s2) throws ROException, IOException {
        if (buffer[pos] != Serializer.ROHeader[0] || buffer[1 + pos] != Serializer.ROHeader[1] || buffer[2 + pos] != Serializer.ROHeader[2] || buffer[3 + pos] != Serializer.ROHeader[3])
            return false;

        if ((0x1 & buffer[5 + pos]) != 0x0)
            throw new ROException("Compressie wordt niet ondersteund.");

        final byte b = buffer[6 + pos];
        pos += 28;
        switch (b) {
            default:
                throw new ROException("Unknown messagetype");

            case 0:
                final String string = readString();
                final String string2 = readString();
                if (s.equalsIgnoreCase(string) && (String.valueOf(s2) + "Response").equalsIgnoreCase(string2))
                    return true;
                throw new ROException(String.format("Wrong interface responded: expected: %s.%s got: %s.%s", s, s2, string, string2));

            case 1: {
                final String string3 = readString();
                String s3 = readString();
                if (string3.equalsIgnoreCase("emaestroexception") && s3.length() > 3)
                    throw new ROException(s3);
            }
        }
        return false;
    }

    public short readShortInt() throws IOException {
        return readWord();
    }

    public float readSingle() throws IOException {
        return Float.intBitsToFloat(readInt32());
    }

    public String readString() throws IOException {
        int length = readInt32();
        if (length > 0) {
            pos += length;
            return new String(buffer, pos - length, length);
        }
        return "";
    }

    protected Type readType() throws Exception {
        switch (readByte()) {
            case 1:
                return Boolean.TYPE;
            case 2:
                return String.class;
            case 3:
                return Date.class;
            case 4:
                return Integer.TYPE;
            case 5:
            case 6:
                return Double.TYPE;
        }
        throw new Exception("Onbekend ROType");
    }

    public Object readVariant() throws IOException {
        short definition = readWord();
        VariantType varType = VariantType.shortBitsToVariantType(definition);
        if (VariantType.isArrayType(definition)) {
            int n = readInt32();
            if (varType == VariantType.Byte) {
                pos += n;
                final byte[] array2 = new byte[n];
                System.arraycopy(buffer, pos - n, array2, 0, n);
                return array2;
            }
            Object[] result = new Object[n];
            for (int i = 0; i < n; i++)
                result[i] = readVariant();
            return result;
        }
        switch (varType) {
            case Boolean:
                return readBoolean();
            case Byte:
                return readByte();
            case Char:
                return (char) readByte();
            case Currency:
                break;
            case Date:
                return readDateTime();
            case Decimal:
                break;
            case Double:
                return readDouble();
            case Empty:
                break;
            case Error:
                break;
            case Int64:
                return readInt64();
            case Integer:
                return readInt32();
            case LongWord:
                return readInt32();
            case Null:
                return DBNull.Value;
            case ShortInt:
                return readShortInt();
            case Single:
                return readSingle();
            case String:
                return readString();
            case UInt64:
                break;
            case Undefined:
                break;
            case Word:
                return readWord();
            default:
                break;
        }
        return null;
    }

    public short readWord() throws IOException {
        pos = 2 + pos;
        return (short) ((short) (0xFF & buffer[-1 + pos]) << 8 | (short) (0xFF & buffer[-2 + pos]));
    }

    void ReserveSpace(int size) {
        if (size + pos < buffer.length) return;
        buffer = concat(

                buffer, new byte[4096 * (1 + (size + pos - buffer.length) / 4096)]);
    }

    public boolean SkipROBinary() throws IOException {
        boolean result = false;
        if (buffer.length > pos) {
            result = 1 == readByte();
            if (result) readInt32();
        }
        return result;
    }

    /**
     * Simple Patternfinder, moves the pos to where the pattern is
     *
     * @param pattern the pattern to find
     */
    public void SkipTo(final byte[] pattern) {
        int idx = 0;
        while (idx < pattern.length && pos < buffer.length) {
            if (buffer[pos] == pattern[idx]) idx++;
            else idx = 0;
            pos++;
        }
    }

    public void write(boolean value) {
        writeVariant(value);
    }

    void write(Object hObject) {
        if (hObject == null) writeNull();
        else if (hObject instanceof DataTable) writeDataTable((DataTable) hObject);
        else if (hObject instanceof Boolean) writeBoolean((Boolean) hObject);
        else if (hObject instanceof Date) writeDateTime((Date) hObject);
        else if (hObject instanceof Integer) writeInteger((Integer) hObject);
        else if (hObject instanceof String) writeString((String) hObject);
        else if (hObject instanceof Byte) writeByte((Byte) hObject);
    }

    public void writeArray(Object[] value) {
        writeVariantArray(value);
    }

    public void writeBinary(byte[] value) {
        if (value != null) {
            int length = value.length;
            writeInt32(length);
            ReserveSpace(length);
            if (length > 0) {
                System.arraycopy(value, 0, buffer, pos, length);
                pos += length;
            }
        } else writeNull();
    }

    public void writeBoolean(boolean value) {
        byte bool = 0;
        if (value) bool = 1;

        ReserveSpace(1);
        buffer[pos] = bool;
        pos++;
    }

    public void writeByte(byte value) {
        ReserveSpace(1);
        buffer[pos] = value;
        pos++;
    }

    public void writeDataTable(final DataTable table) {
        if (table.TableName == null) writeString("Unnamed");
        else writeString(table.TableName);

        final int size = table.Columns.size();
        writeInteger(size);
        for (DataColumn column : table.Columns) {
            writeString(column.Name);
            writeByte(getFieldType(column.DataType));
            writeInt32(255);
            writeString(column.Name);
            writeInt32(0);
            writeBoolean(true);
            writeBoolean(false);
            writeString("");
        }
        writeInteger(table.size());
        if (size > 0) {
            for (DataRow row : table) {
                writeInteger(row.size());
                for (DataColumn column : table.Columns)
                    writeVariant(row.get(column.Name));
            }
        }
    }

    public void writeDateTime(final Date date) {
        if (date.equals(Global.NODATE)) {
            writeDouble(0.0);
            return;
        }
        initializeOffsets(date);
        final long n = date.getTime() + Serializer.timezoneOffset;
        int daylightOffset;
        if (Serializer.timezone.inDaylightTime(date)) daylightOffset = 0;

        else daylightOffset = Serializer.daylightOffset;

        writeDouble(25569.0 + (n + daylightOffset) / 86400000L);
    }

    public void writeDouble(double value) {
        writeInt64(Double.doubleToRawLongBits(value));
    }

    public void writeFloat(Float value) {
        writeInteger(Float.floatToIntBits(value));
    }

    public void writeInt32(int value) {
        ReserveSpace(4);
        buffer[pos + 3] = (byte) (value >> 24);
        buffer[pos + 2] = (byte) ((0xFF & value) >> 16);
        buffer[pos + 1] = (byte) ((0xFF & value) >> 8);
        buffer[pos] = (byte) (value & 0xFF);
        pos += 4;
    }

    public void writeInt64(long value) {
        ReserveSpace(8);
        writeInt32((int) (value));
        writeInt32((int) (value >>> 32));
    }

    public void writeInteger(int iValue) {
        writeInt32(iValue);
    }

    public void writeNull() {
        writeByte((byte) 0);
    }

    public void writePlaceholderWithSize(int placeholder) {
        int i = pos - placeholder;
        buffer[placeholder - 1] = (byte) (i >> 24);
        buffer[placeholder - 2] = (byte) (0xFF & i >> 16);
        buffer[placeholder - 3] = (byte) (0xFF & i >> 8);
        buffer[placeholder - 4] = (byte) (i & 0xFF);
    }

    public void writeROBinary(Object[] values) {
        writeROBinaryInternal(true, values);
    }

    public void writeROBinaryInternal(boolean b, Object... array) {
        writeByte((byte) 1);
        writeInt32(0);

        if (array != null && array.length > 0) {
            for (Object o : array)
                if (b) writeVariant(o);
                else write(o);
        } else writeVariantType(VariantType.Empty);
        writePlaceholderWithSize(pos);
    }

    public void writeROBinaryWithObjects(Object... values) {
        writeROBinaryInternal(false, values);
    }

    public void writeROBinaryWithVariantArray(Object[] values) {
        writeByte((byte) 1);
        writeInt32(0);
        int i = pos;
        if (values != null) writeVariantArray(values);
        writePlaceholderWithSize(i);
    }

    public void writeROBoolean(boolean value) {
        writeInteger(value ? 1 : 0);
    }

    public void writeROHeader(byte[] clientID, String Interface, String Method) {
        System.arraycopy(ROHeader, 0, buffer, pos, ROHeader.length);
        pos += 12;
        System.arraycopy(clientID, 0, buffer, pos, clientID.length);
        pos += 16;
        writeString(Interface);
        writeString(Method);
    }

    public void writeShortInt(short value) {
        writeWord(value);
    }

    public void writeSmallInt(short value) {
        writeWord(value);
    }

    public void writeString(String value) {
        try {
            writeBinary(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    public void writeValues(Object[] values) {
        writeInt32(0);
        int pos = this.pos;
        for (Object o : values)
            write(o);
        writePlaceholderWithSize(pos);
    }

    public void writeVariant(Object object) {
        if (object instanceof DataTable) {
            writeDataTable((DataTable) object);
            return;
        }
        VariantType localVariantType = VarType(object);
        writeVariantType(localVariantType);

        switch (localVariantType) {
            case Date:
                writeDateTime((Date) object);
                return;
            case String:
                writeString((String) object);
                return;
            case Boolean:
                writeBoolean((Boolean) object);
                return;
            case ShortInt:
                writeShortInt((Short) object);
                return;
            case Byte:
                writeByte((Byte) object);
                return;
            case SmallInt:
                writeSmallInt((Short) object);
                return;
            case Integer:
                writeInteger((Integer) object);
                return;
            case Int64:
                writeInt64((Long) object);
                return;
            case Single:
                writeFloat((Float) object);
            case Double:
                writeDouble((Double) object);
                return;
            default:
                Log.i(TAG, "writeVariant didn't write " + localVariantType);
        }
    }

    public void writeVariantArray(final Object... array) {
        writeWord((short) 8204);
        writeInteger(array.length);
        for (Object o : array)
            writeVariant(o);
    }

    void writeVariantType(VariantType type) {
        writeWord((short) type.ordinal());
    }

    public void writeWord(short value) {
        ReserveSpace(2);
        buffer[pos] = (byte) (value & 0xFF);
        buffer[1 + pos] = (byte) (value >> 8);
        pos = 2 + pos;
    }

    public static enum VariantType {
        Empty, Null, SmallInt, Integer, Single, Double, Currency, Date, String, Error, Boolean, Object, Decimal, ShortInt, Byte, Word, LongWord, Int64, UInt64, Char, Undefined;

        public static final String TAG = "VariantType";

        public static boolean isArrayType(short paramShort) {
            return (paramShort & 0x2000) != 0;
        }

        public static VariantType shortBitsToVariantType(short paramShort) {
            int param = (short) (paramShort & 0xFFF);

            for (VariantType variantType : VariantType.values()) {
                if (variantType.ordinal() == param) return variantType;
                else Log.w(TAG, "param was " + param + ", variantType was " + variantType);
            }
            return Undefined;
        }

    }
}