package icecube.daq.payload;

import icecube.daq.util.IDOMRegistry;

import java.nio.ByteBuffer;

/**
 * Hit data recorded in an event.
 */
public interface IEventHitRecord
{
    /**
     * Get this hit's channel ID
     * @return channel ID
     */
    short getChannelID();
    /**
     * Get the time for this hit.
     * @return hit time
     */
    long getHitTime();
    /**
     * Return the number of bytes in this record.
     * @return number of bytes
     */
    int length();
    /**
     * Return <tt>true</tt> if <tt>hitData</tt> matches this hit.
     * @param domRegistry used to map each hit's DOM ID to the channel ID
     * @param hitData hit data being compared
     * @return <tt>true</tt> if the hits match
     */
    boolean matches(IDOMRegistry domRegistry, IHitPayload hitData);
    /**
     * Write this hit record.
     * @param buf byte buffer
     * @param offset index of first hit record byte in the byte buffer
     * @param baseTime base timestamp from which relative times are computed
     * @return number of bytes written
     * @throws PayloadException if there is a problem
     */
    int writeRecord(ByteBuffer buf, int offset, long baseTime)
        throws PayloadException;
}
