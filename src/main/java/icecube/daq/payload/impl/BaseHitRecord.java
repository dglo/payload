package icecube.daq.payload.impl;

import icecube.daq.payload.IEventHitRecord;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.PayloadException;
import icecube.daq.util.IDOMRegistry;

import java.nio.ByteBuffer;

/**
 * Base hit record representation
 */
public abstract class BaseHitRecord
    implements Comparable, IEventHitRecord
{
    /** Offset of length field */
    private static final int OFFSET_LENGTH = 0;
    /** Offset of record type field */
    private static final int OFFSET_TYPE = 2;
    /** Offset of flags field */
    private static final int OFFSET_FLAGS = 3;
    /** Offset of channel ID field */
    private static final int OFFSET_CHANNELID = 4;
    /** Offset of relative time field */
    private static final int OFFSET_RELTIME = 6;
    /** Offset of raw data field */
    private static final int OFFSET_RAWDATA = 10;

    /** delta-compressed flags */
    private byte flags;
    /** channel ID */
    private short chanId;
    /** hit time */
    private long time;
    /** raw data */
    private byte[] rawData;

    /**
     * Generic constructor
     */
    BaseHitRecord()
    {
    }

    /**
     * Create a base hit record
     * @param flags delta-compressed flags
     * @param chanId channel ID
     * @param time hit time
     * @param rawData raw data bytes
     */
    BaseHitRecord(byte flags, short chanId, long time, byte[] rawData)
    {
        this.flags = flags;
        this.chanId = chanId;
        this.time = time;
        this.rawData = new byte[rawData.length];
        System.arraycopy(rawData, 0, this.rawData, 0, rawData.length);
    }

    /**
     * Compare this record against another object
     *
     * @param obj object
     *
     * @return the usual values
     */
    @Override
    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        } else if (!(obj instanceof IEventHitRecord)) {
            return getClass().getName().compareTo(obj.getClass().getName());
        }

        IEventHitRecord hr = (IEventHitRecord) obj;

        int val;
        val = chanId - hr.getChannelID();
        if (val == 0) {
            val = (int)(time - hr.getHitTime());
        }

        return val;
    }

    /**
     * Is the specified object equal to this object?
     * @param obj object being compared
     * @return <tt>true</tt> if the objects are equal
     */
    @Override
    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    /**
     * Get this hit's channel ID
     * @return channel ID
     */
    @Override
    public short getChannelID()
    {
        return chanId;
    }

    /**
     * Get this hit's UTC time
     * @return value
     */
    @Override
    public long getHitTime()
    {
        return time;
    }

    /**
     * Get the raw data array
     * @return raw data array
     */
    public byte[] getRawData()
    {
        return rawData;
    }

    /**
     * Get a debugging string for the hit record's raw data.
     * @return debugging string
     */
    public String getRawDataString()
    {
        return " rawData*" + rawData.length;
    }

    /**
     * Get the name of this hit type (used in base class error messages)
     * @return name
     */
    abstract String getTypeName();

    /**
     * Return this object's hash code
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        return (((int)chanId & 0xffff) << 16) +
            ((int)(time & 0xffff));
    }

    /**
     * Get the record length
     * @return number of bytes
     */
    @Override
    public int length()
    {
        return OFFSET_RAWDATA + rawData.length;
    }

    /**
     * Load this hit record from the byte buffer
     * @param buf byte buffer
     * @param offset index of first byte
     * @param baseTime base time used to expand relative times
     * @param validType expected type of this hit record
     * @throws PayloadException if there is a problem
     */
    int loadRecord(ByteBuffer buf, int offset, long baseTime, int validType)
        throws PayloadException
    {
        final int len = buf.getShort(offset + OFFSET_LENGTH);
        if (offset + len > buf.capacity()) {
            throw new PayloadException(getTypeName() + " hit record requires " +
                                       len + " bytes, but only " +
                                       (buf.capacity() - offset) +
                                       " (of " + buf.capacity() +
                                       ") are available");
        }

        final int type = (int) buf.get(offset + OFFSET_TYPE);
        if (type != validType) {
            throw new PayloadException(getTypeName() + " hit type should be " +
                                       validType + ", not " + type);
        }

        flags = buf.get(offset + OFFSET_FLAGS);
        chanId = buf.getShort(offset + OFFSET_CHANNELID);
        time = (long) buf.getInt(offset + OFFSET_RELTIME) + baseTime;

        rawData = new byte[len - OFFSET_RAWDATA];

        final int origPos = buf.position();
        buf.position(offset + OFFSET_RAWDATA);
        buf.get(rawData);
        buf.position(origPos);

        return len;
    }

    /**
     * Return <tt>true</tt> if the specified hit matches this hit record
     * @param domRegistry used to map each hit's DOM ID to the channel ID
     * @param hit hit to compare
     * @return <tt>true</tt> if this hit record matches the hit
     */
    @Override
    public boolean matches(IDOMRegistry domRegistry, IHitPayload hit)
    {
        if (time != hit.getUTCTime()) {
            return false;
        }

        short hitChanId;
        if (hit.hasChannelID()) {
            hitChanId = hit.getChannelID();
        } else {
            hitChanId = domRegistry.getChannelId(hit.getDOMID().longValue());
        }

        return chanId == hitChanId;
    }

    /**
     * Write this hit record to the byte buffer
     * @param buf byte buffer
     * @param offset index of first byte
     * @param baseTime base time used to compute relative times
     * @param type type of this hit record
     * @return number of bytes written
     * @throws PayloadException if there is a problem
     */
    public int writeRecord(ByteBuffer buf, int offset, long baseTime, int type)
        throws PayloadException
    {
        final int len = length();
        if (offset + len > buf.capacity()) {
            throw new PayloadException(getTypeName() + " hit record requires " +
                                       len + " bytes, but only " +
                                       (buf.capacity() - offset) +
                                       " (of " + buf.capacity() +
                                       ") are available");
        }

        buf.putShort(offset + OFFSET_LENGTH, (short) len);
        buf.put(offset + OFFSET_TYPE, (byte) type);
        buf.put(offset + OFFSET_FLAGS, flags);
        buf.putShort(offset + OFFSET_CHANNELID, chanId);
        buf.putInt(offset + OFFSET_RELTIME, (int) (time - baseTime));

        final int origPos = buf.position();
        buf.position(offset + OFFSET_RAWDATA);
        buf.put(rawData);
        buf.position(origPos);

        return len;
    }

    /**
     * Get a debugging string representing this object.
     * @return debugging string
     */
    @Override
    public String toString()
    {
        return getTypeName() + "HitRecord[flags " + flags + " chan " + chanId +
            " time " + time + getRawDataString() + "]";
    }
}
