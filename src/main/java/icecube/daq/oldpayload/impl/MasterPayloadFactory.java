package icecube.daq.oldpayload.impl;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.PayloadRegistry;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

/**
 * This PayloadFactory is designed to detect Payload's by type
 * and use the correct PayloadFactory for the identified Payload type.
 * This Object acts as a proxy for the correctly bound PayloadFactory
 * dependent on the Payload Type. This uses the PayloadRegistry to
 * determin the correct context for a Payload/Spliceable.
 *
 * @author dwharton
 */
public class MasterPayloadFactory extends PayloadFactory {
    private boolean mbCreateSeperateBuffers;

    private IByteBufferCache mtBufferCache;
    private HitPayloadFactory hitFactory;
    private ReadoutRequestPayloadFactory rReqFactory;
    private TriggerRequestPayloadFactory tReqFactory;
    private ReadoutDataPayloadFactory rDataFactory;
    private DeltaCompressedFormatHitDataPayloadFactory cHitDataFactory;

    /**
     * Standard constructor.
     */
    public MasterPayloadFactory() {
        this(null);
    }

    /**
     * Constructor which uses an installable IByteBufferCache
     * @param tBufferCache IByteBufferCache which will serve as the recycling destination
     *                            for the ByteBuffer's which are used as the basis of the Payloads
     *                            which are created by the individual PayloadFactories managed by this
     *                            factory.
     */
    public MasterPayloadFactory(IByteBufferCache tBufferCache) {
        mtBufferCache =  tBufferCache;
    }

    /**
     *  Get a PayloadFactory from the installed registry of the following types.
     *  PayloadRegistry.
     *
     * @see icecube.daq.payload.PayloadRegistry
     */
    public PayloadFactory getPayloadFactory(int iType) {
        PayloadFactory factory;
        switch (iType) {
        case PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT:
            if (hitFactory == null) {
                hitFactory = new HitPayloadFactory();
            }

            factory = hitFactory;
            break;
        case PayloadRegistry.PAYLOAD_ID_READOUT_REQUEST:
            if (rReqFactory == null) {
                rReqFactory = new ReadoutRequestPayloadFactory();
            }

            factory = rReqFactory;
            break;
        case PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST:
            if (tReqFactory == null) {
                tReqFactory = new TriggerRequestPayloadFactory();
            }

            factory = tReqFactory;
            break;
        case PayloadRegistry.PAYLOAD_ID_READOUT_DATA:
            if (rDataFactory == null) {
                rDataFactory = new ReadoutDataPayloadFactory();
            }

            factory = rDataFactory;
            break;
        case PayloadRegistry.PAYLOAD_ID_COMPRESSED_HIT_DATA:
            if (cHitDataFactory == null) {
                cHitDataFactory =
                    new DeltaCompressedFormatHitDataPayloadFactory();
            }

            factory = cHitDataFactory;
            break;
        default:
            throw new Error("Unimplemented (iType="+iType+")");
        }

        if (mtBufferCache != null) {
            factory.setByteBufferCache(mtBufferCache);
        }

        return factory;
    }
    /**
     * Method to set the creation of seperate
     * ByteBuffer's for each payload to on or off.
     * Note: When this is on, a seperate backing buffer is created for each
     *       payload. This is helpfull (but wasteful) for debugging the results of
     *       buffered payload handling and comparing the results when using Seperate
     *       and combined buffers.
     * @return the previous value
     */
    public boolean setCreateSeperateBuffers(boolean bOn) {
        boolean bBefore = mbCreateSeperateBuffers;
        mbCreateSeperateBuffers = bOn;
        return bBefore;
    }


    /**
     *  This method must be implemented by the non-abstract class
     *  to create the specific payload.
     *  @param iOffset The offset in the ByteBuffer from which to create the payload/spliceable
     *  @param tPayloadBuffer ByteBuffer from which to construct the Payload
     *                           which implements BOTH IPayload and Spliceable
     *  @return the Payload object specific to this class which is
     *                     specific to the class which is derived from PayloadFactory.
     */
    public Payload createPayload(int iOffset, ByteBuffer tPayloadBuffer) throws DataFormatException {
        //-just call the create routine using the internal setting
        return createPayload(iOffset,tPayloadBuffer, mbCreateSeperateBuffers);
    }
    /**
     *  This method must be implemented by the non-abstract class
     *  to create the specific payload.
     *  @param iOffset The offset in the ByteBuffer from which to create the payload/spliceable
     *  @param tPayloadBuffer ByteBuffer from which to construct the Payload
     *                           which implements BOTH IPayload and Spliceable
     * @param bCreateSeperateBuffers boolean indicating the a new ByteBuffer will be
     *                                   allocated and used for Payload creation.
     *      1) If there is an IByteBufferCache installed this will be used to create
     *         a new ByteBuffer.
     *      2) If there is no IByteBufferCache installed then a ByteBuffer will be
     *         allocated normally outside of any caching system.
     *
     *  @return the Payload object specific to this class which is
     *                     specific to the class which is derived from PayloadFactory.
     */
    public Payload createPayload(int iOffset, ByteBuffer tPayloadBuffer, boolean bCreateSeperateBuffers) throws DataFormatException {
        Payload tPayload = null;
        int pType =
            PayloadEnvelope.readPayloadType(iOffset, tPayloadBuffer);
        PayloadFactory tFactory = getPayloadFactory(pType);
        if (tFactory == null) {
            throw new DataFormatException("Factory associated with payload #" +
                                          pType + " not found");
        }

        int pLen =
            PayloadEnvelope.readPayloadLength(iOffset, tPayloadBuffer);

        //-This section is needed to make buffering distinct from PayloadCreation.
        // This allows the system to be tested distinctly from buffering.
        if (bCreateSeperateBuffers) {
            ByteBuffer tNewPayloadBuffer = null;
            if (mtBufferCache != null) {
                //-create the new ByteBuffer from the installed buffer cache
                // and copy the contents into the new ByteBuffer before creation
                tNewPayloadBuffer = mtBufferCache.acquireBuffer(pLen);
                int iOldLimit = tPayloadBuffer.limit();
                int iOldPos   = tPayloadBuffer.position();
                tPayloadBuffer.position(iOffset);
                tPayloadBuffer.limit(iOffset + pLen);
                //-setup position and limit for copy
                tNewPayloadBuffer.position(0);
                tNewPayloadBuffer.limit(pLen);
                tNewPayloadBuffer.put(tPayloadBuffer);
                //-reset the position and limit
                tNewPayloadBuffer.position(0);
                tPayloadBuffer.position(iOldPos);
                tPayloadBuffer.limit(iOldLimit);
            } else {
                //-allocate the seperate buffer
                byte[] tNewPayloadBufferBytes = new byte[pLen];
                //-copy the data from the old buffer to the njew one.
                int iOldPosition = tPayloadBuffer.position();
                tPayloadBuffer.position(iOffset);
                tPayloadBuffer.get(tNewPayloadBufferBytes, 0, pLen);
                tNewPayloadBuffer = ByteBuffer.wrap( tNewPayloadBufferBytes );
                tPayloadBuffer.position(iOldPosition);
            }
            //-create the payload from the factory.
            tPayload = tFactory.createPayload(0, tNewPayloadBuffer);
        } else {
            //-create the payload using the input ByteBuffer
            tPayload = tFactory.createPayload(iOffset, tPayloadBuffer);
        }
        return tPayload;
    }
    /**
     * This method must be implemented specific to the format of the
     * the input stream to determine when a complete data element is available.
     * @param iOffset The offset in the ByteBuffer from which to create the payload/spliceable
     * @param tBuffer ByteBuffer from which to detect a spliceable.
     *
     * @return the length of this spliceable
     *
     * @exception DataFormatException if there is an error in the format of the payload
     */
    public int readSpliceableLength(int iOffset, ByteBuffer tBuffer) throws DataFormatException {
        return PayloadEnvelope.readPayloadLength(iOffset, tBuffer);
    }
}