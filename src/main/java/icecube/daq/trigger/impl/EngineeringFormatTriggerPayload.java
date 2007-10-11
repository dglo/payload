package icecube.daq.trigger.impl;

import java.util.zip.DataFormatException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import icecube.daq.payload.PayloadDestination;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.impl.DomHitEngineeringFormatPayload;
import icecube.daq.payload.impl.DomHitEngineeringFormatPayloadFactory;
import icecube.daq.payload.impl.PayloadEnvelope;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.impl.UTCTime8B;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.trigger.AbstractTriggerPayload;
import icecube.util.Poolable;

/**
 * This class is an implementation of ITriggerPayload for an EngineeringFormat
 * hit record.
 *
 * NOTE: The contained 'subpayload' should always be readjusted after initialization
 *       when being loaded if it is created from the parent ByteBuffer. If the 'subpayload'
 *       has been loaded externally (ie there is no backing for the parent payload) then
 *       this is not necessary.
 *
 *
 * @author dwharton
 */
public class EngineeringFormatTriggerPayload  extends AbstractTriggerPayload {

    public static final int SIZE_TRIGGER_CONFIG_ID = 4;
    public static final int SIZE_SOURCE_ID         = 4;
    public static final int SIZE_TRIGGER_HDR       = SIZE_TRIGGER_CONFIG_ID + SIZE_SOURCE_ID;

    public static final int OFFSET_TRIGGER_CONFIG_ID = OFFSET_PAYLOAD_DATA;
    public static final int OFFSET_SOURCE_ID         = OFFSET_TRIGGER_CONFIG_ID + SIZE_TRIGGER_CONFIG_ID;

    public static final int OFFSET_ENGFORM_PAYLOAD  = OFFSET_SOURCE_ID + SIZE_SOURCE_ID;

    public static final String TRIGGER_TYPE      = "TRIGGER_TYPE";
    public static final String TRIGGER_CONFIG_ID = "TRIGGER_CONFIG_ID";
    public static final String SOURCE_ID         = "SOURCE_ID";

    protected int mi_TriggerConfigID = -1;
    protected ISourceID mt_sourceId;

    /**
     * This Hard Coded Payload FActory is necessary based on the type of
     * data that is contained in the sub-payload.
     */
    protected static DomHitEngineeringFormatPayloadFactory mt_EngFormtPayloadFact = new DomHitEngineeringFormatPayloadFactory();

    /**
     * Note: This is not a standard Payload with a payload envelope! This
     *       payload is generated by DomHUB or TestDAQ. Therefor it is
     *       a special case.
     */
    protected DomHitEngineeringFormatPayload mt_EngFormatPayload;

    protected boolean mb_IsEngPayloadLoaded;
    protected boolean mb_IsTriggerPayloadLoaded;

    /**
     * Standard Constructor, enabling pooling
     */
    public EngineeringFormatTriggerPayload() {
        //-NOTE: This may not work 2 levels down...
        super.mipayloadtype = PayloadRegistry.PAYLOAD_ID_ENGFORMAT_TRIGGER;
        super.mipayloadinterfacetype = PayloadInterfaceRegistry.I_TRIGGER_PAYLOAD;
        mttime = (IUTCTime) UTCTime8B.getFromPool();
        ((UTCTime8B) mttime).initialize(-1L);
    }

    /**
     * `returns ID of trigger
     */
    public int getTriggerConfigID() {
        return mi_TriggerConfigID;
    }

    /**
     * returns ID of process that is responsible for this payload
     * This is undefined at this point.
     */
    public ISourceID getSourceID() {
        return mt_sourceId;
    }


    /**
     * Get an object from the pool
     * @return object of this type from the object pool.
     */
    public static Poolable getFromPool() {
        return (Poolable) new EngineeringFormatTriggerPayload();
    }

    /**
     * Get an object from the pool in a non-static context.
     * @return object of this type from the object pool.
     */
    public Poolable getPoolable() {
        Payload tPayload = (Payload) getFromPool();
        tPayload.mtParentPayloadFactory = mtParentPayloadFactory;
        return (Poolable) tPayload;
    }

    /**
     * Returns an instance of this object so that it can be
     * recycled, ie returned to the pool.
     */
    public void recycle() {
        if (mt_EngFormatPayload != null) {
            mt_EngFormatPayload.recycle();
            //-since this is a Payload, it must be nulled out after recycle() because
            // it is automatically disposed by the base class.
            mt_EngFormatPayload = null;
        }
        if (mt_sourceId != null) {
            ((SourceID4B)mt_sourceId).recycle();
            mt_sourceId = null;
        }
        //-call this LAST!!
        super.recycle();
    }

    /**
     * Initialize the hit information from a test-daq payload.
     */
    public void initialize(ISourceID tSourceID, int iTriggerType, int iTriggerConfigID, DomHitEngineeringFormatPayload tPayload) throws IOException, DataFormatException {
        mt_sourceId = (ISourceID) SourceID4B.getFromPool();
        ((SourceID4B)mt_sourceId).initialize(tSourceID.getSourceID());
        mi_TriggerConfigID = iTriggerConfigID;
        mt_PayloadEnvelope = (PayloadEnvelope) PayloadEnvelope.getFromPool();
        mt_EngFormatPayload = tPayload;
        UTCTime8B tTime = (UTCTime8B) UTCTime8B.getFromPool();
        tTime.initialize( mt_EngFormatPayload.getPayloadTimeUTC().getUTCTimeAsLong() );
        mttime = (IUTCTime) tTime;
        milength = PayloadEnvelope.SIZE_ENVELOPE + SIZE_TRIGGER_HDR+ mt_EngFormatPayload.getPayloadLength();
        //-Fill in the envelope data
        mt_PayloadEnvelope.initialize(
            this.getPayloadType(),
            milength,
            mttime.getUTCTimeAsLong()
            );
        //-when initialized in this way the payload has been created.
        super.mbPayloadCreated = true;
    }

    /**
     * This method writes this payload to the destination ByteBuffer
     * at the specified offset and returns the length of bytes written to the destination.
     * @param bWriteLoaded true to write loaded data (even if bytebuffer backing exists)
     *                                     false to write data normally (depending on backing)
     * @param iDestOffset the offset into the destination ByteBuffer at which to start writting the payload
     * @param tDestBuffer the destination ByteBuffer to write the payload to.
     *
     * @return the length in bytes which was written to the ByteBuffer.
     *
     * @throws IOException if an error occurs during the process
     */
    public int writePayload(boolean bWriteLoaded, int iDestOffset, ByteBuffer tDestBuffer) throws IOException {
        int iBytesWritten = 0;
        //-Check to make sure if this is a payload that has been loaded with backing
        if (super.mtbuffer != null && !bWriteLoaded) {
            iBytesWritten =  super.writePayload(bWriteLoaded, iDestOffset, tDestBuffer);
        } else {
            if (super.mtbuffer != null) {
                try {
                    loadPayload();
                } catch ( DataFormatException tException) {
                    throw new IOException("DataFormatException Caught during load");
                }
            }
            //-create the new payload from both the envelope and the hit payload
            //-Write out the PayloadEnvelope
            // NOTE: the initialize method has already filled in the appropriate lengths
            //       and the time (utc) has already been initialized.
            mt_PayloadEnvelope.writeData(iDestOffset, tDestBuffer);
            tDestBuffer.putInt(   iDestOffset + OFFSET_TRIGGER_CONFIG_ID , mi_TriggerConfigID        );
            tDestBuffer.putInt(   iDestOffset + OFFSET_SOURCE_ID         , mt_sourceId.getSourceID() );
            //-Write out the 'subpayload'
            ((Payload) mt_EngFormatPayload).writePayload(
                        bWriteLoaded,
                        (iDestOffset + OFFSET_ENGFORM_PAYLOAD), tDestBuffer);
            iBytesWritten = mt_PayloadEnvelope.miPayloadLen;
        }
        return iBytesWritten;
    }

    /**
     * This method writes this payload to the PayloadDestination.
     *
     * @param bWriteLoaded true to write loaded data (even if bytebuffer backing exists)
     *                                     false to write data normally (depending on backing)
     * @param tDestination PayloadDestination to which to write the payload
     * @return the length in bytes which was written to the destination.
     *
     * @throws IOException if an error occurs during the process
     */
    public int writePayload(boolean bWriteLoaded, PayloadDestination tDestination) throws IOException {
        if (tDestination.doLabel()) tDestination.label("[EngineeringFormatTriggerPayload]=>").indent();
        int iBytesWritten = 0;
        //-Check to make sure if this is a payload that has been loaded with backing
        if (super.mtbuffer != null && !bWriteLoaded) {
            iBytesWritten =  super.writePayload(bWriteLoaded, tDestination);
        } else {
            if (super.mtbuffer != null) {
                try {
                    loadPayload();
                } catch ( DataFormatException tException) {
                    tException.printStackTrace();
                    throw new IOException("DataFormatException Caught during load");
                }
            }
            //-create the new payload from both the envelope and the hit payload
            //-Write out the PayloadEnvelope
            // NOTE: the initialize method has already filled in the appropriate lengths
            //       and the time (utc) has already been initialized.
            mt_PayloadEnvelope.writeData(tDestination);
            //-Write out Trigger Specific Data, TriggerType, TriggerConfigID, ISourceID
            tDestination.writeInt(   TRIGGER_CONFIG_ID ,mi_TriggerConfigID        );
            tDestination.writeInt(   SOURCE_ID         ,mt_sourceId.getSourceID() );
            //-Write out the 'subpayload'
            //if (tDestination.doLabel()) tDestination.label("[EngineeringFormatTriggerPayload.DomHitEngineeringFormatPayload]=>").indent();
            ((Payload) mt_EngFormatPayload).writePayload(bWriteLoaded, tDestination);
            //if (tDestination.doLabel()) tDestination.undent().label("<=[EngineeringFormatTriggerPayload.DomHitEngineeringFormatPayload]");
            iBytesWritten = mt_PayloadEnvelope.miPayloadLen;
        }
        if (tDestination.doLabel()) tDestination.undent().label("<=[EngineeringFormatTriggerPayload]");
        return iBytesWritten;
    }


    /**
     * Loads the DomHitEngineeringFormatPayload if not already loaded
     */
    protected void loadEngPayload() throws IOException, DataFormatException {
        if (!mb_IsEngPayloadLoaded) {
            if (super.mtbuffer != null) {
                mt_EngFormatPayload = (DomHitEngineeringFormatPayload) mt_EngFormtPayloadFact.createPayload(mioffset + OFFSET_ENGFORM_PAYLOAD, mtbuffer);
                mt_EngFormatPayload.loadPayload();
            }
        }
    }

    /**
     * Loads the TriggerData if not already loaded
     */
    protected void loadTriggerPayload() throws IOException, DataFormatException {
        if (!mb_IsTriggerPayloadLoaded) {
            if (super.mtbuffer != null) {
                //-extract the order, so can switch to BIG_ENDIAN for reading the payload
                ByteOrder tSaveOrder = mtbuffer.order();
                if (tSaveOrder != ByteOrder.BIG_ENDIAN) {
                    mtbuffer.order(ByteOrder.BIG_ENDIAN);
                }
                mi_TriggerConfigID = mtbuffer.getInt( mioffset + OFFSET_TRIGGER_CONFIG_ID );
                mt_sourceId = (ISourceID) SourceID4B.getFromPool();
                ((SourceID4B) mt_sourceId).initialize( mtbuffer.getInt(  mioffset + OFFSET_SOURCE_ID) );
                //-restore order
                if (tSaveOrder != ByteOrder.BIG_ENDIAN) {
                    mtbuffer.order(tSaveOrder);
                }
                mb_IsTriggerPayloadLoaded = true;
            }
        }
    }

    /**
     * Initializes Payload from backing so it can be used as an IPayload.
     */
    public void loadPayload() throws IOException, DataFormatException {
        loadEnvelope();
        loadTriggerPayload();
        loadEngPayload();
    }

    /**
     * shift offset of object inside buffer (called by PayloadFactory)
     * NOTE: This is overriden from Payload to accomodate the subpayload
     */
    public void shiftOffset(int shift) {
        super.shiftOffset(shift);
        if (mb_IsEngPayloadLoaded) mt_EngFormatPayload.shiftOffset(shift);
    }


    /**
     * returns type of trigger based on the trigger mode in the underlying hit
     */
    public int getTriggerType() {
        int iTriggerType = -1;
        if (mb_IsEngPayloadLoaded || mt_EngFormatPayload == null) {
            try {
                //-Load the engineering payload so it can be accessed
                loadEngPayload();
            } catch ( IOException tIOException) {
                //-TODO log the error here
                System.out.println("Class("+this.getClass().getName()+"):EngineeringFormatTriggerPayload.getTriggerType() IOException="+tIOException);
                tIOException.printStackTrace();
            } catch ( DataFormatException tDataFormatException) {
                //-TODO log the error here
                System.out.println("Class("+this.getClass().getName()+"):EngineeringFormatTriggerPayload.getTriggerType() DataFormatException="+tDataFormatException);
                tDataFormatException.printStackTrace();
            }

        }
        if (mt_EngFormatPayload != null) {
            iTriggerType =  mt_EngFormatPayload.getTriggerMode();
        }
        return iTriggerType;
    }

    /**
     * dispose of this payload
     */
    public void dispose() {
        //-note on Payload's: If the recycle() method has been called
        // then these references should be null by the time dispose()
        // is caused.
        if (mt_EngFormatPayload != null) {
            mt_EngFormatPayload.dispose();
            mt_EngFormatPayload = null;
        }
        if (mt_sourceId != null) {
            ((SourceID4B)mt_sourceId).dispose();
            mt_sourceId = null;
        }
        mb_IsEngPayloadLoaded = false;
        mb_IsTriggerPayloadLoaded = false;
        //-envelope is handled by AbstractTriggerPayload
        //-This must be called LAST!!
        super.dispose();
    }

}
