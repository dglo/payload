/*
 * class: IPayload
 *
 * Version $Id: IPayload.java 17760 2020-03-12 14:29:31Z dglo $
 *
 * Date: September 17 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.payload;

import java.nio.ByteBuffer;

/**
 * Basic interface defining a trigger primitive payload
 *
 * @version $Id: IPayload.java 17760 2020-03-12 14:29:31Z dglo $
 * @author hellwig, dwharton
 */
public interface IPayload
    extends ICopyable
{
    /**
     * Returns the ByteBuffer which backs this payload
     * if it has one.
     * @return the backing of this payload if it has one.
     *                        this will be null if it is not 'backed'.
     * NOTE: dbw: this is for Chuck McParland for testing...
     */
    ByteBuffer getPayloadBacking();

    /**
     * returns the Payload type
     * @return type from PayloadRegistry
     */
    int getPayloadType();

    /**
     * gets the UTC time tag of a payload
     * @return time
     */
    IUTCTime getPayloadTimeUTC();

    /**
     * gets the UTC time for this payload as a long value
     * @return time
     */
    long getUTCTime();

    /**
     * returns the length in bytes of this payload
     * @return payload length
     */
    int length();

    /**
     * Set the buffer cache for this payload.
     *
     * @param cache ByteBuffer cache
     */
    void setCache(IByteBufferCache cache);
}
