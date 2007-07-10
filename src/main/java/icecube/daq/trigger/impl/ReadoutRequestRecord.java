package icecube.daq.trigger.impl;

import java.nio.ByteOrder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.Vector;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.IWriteablePayloadRecord;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.payload.PayloadDestination;
import icecube.util.Poolable;

/**
 * This object contains the data representing a single
 * readout request generated by an Trigger Event,
 * and the logic of how to read and write it to/from a ByteBuffer.
 *
 * @author dwharton
 */
public class ReadoutRequestRecord extends Poolable implements IWriteablePayloadRecord, IReadoutRequest {
    public static final int DEFAULT_REC_TYPE = 1;
    //
    //-Static's defining location and size of data within a ByteBuffer
    //
    public static final int SIZE_REQUEST_TYPE       = 2;    //-request type including endian detection
    public static final int SIZE_TRIGGER_UID        = 4;    //-int the uid for this trigger
    public static final int SIZE_SOURCEID           = SourceID4B.SIZE;    //-int
    public static final int SIZE_NUMBER_ELEMENTS    = 4;    //-int number of contained request elements

    //-This is the size of the header before the beginning of the variable length portion.
    public static final int SIZE_HEADER = SIZE_REQUEST_TYPE + SIZE_TRIGGER_UID + SIZE_SOURCEID + SIZE_NUMBER_ELEMENTS;

    public static final int OFFSET_REQUEST_TYPE     = 0;
    public static final int OFSET_TRIGGER_UID       = OFFSET_REQUEST_TYPE + SIZE_REQUEST_TYPE; //2;
    public static final int OFFSET_SOURCEID         = OFSET_TRIGGER_UID   + SIZE_TRIGGER_UID;  //6;
    public static final int OFFSET_NUM_ELEMENTS     = OFFSET_SOURCEID     + SIZE_SOURCEID;
    public static final int OFFSET_START_ELEMENTS   = OFFSET_NUM_ELEMENTS + SIZE_NUMBER_ELEMENTS;

    public static final String REQUEST_TYPE = "REQUEST_TYPE";
    public static final String TRIGGER_UID   = "TRIGGER_UID";
    public static final String SOURCEID     = "SOURCEID";
    public static final String NUM_ELEMENTS = "NUM_ELEMENTS";

    //
    //-Container Data
    //
    //-NOTE: The specific hard instances of these classes will be generated when read from
    //       a ByteBuffer, but when writing to a destination, it will extract only that which
    //       is necessary for initialization. (ie IDOMID as a long...).
    //       (This may have to be changed in the future, but for now we assume that there is
    //       everything in the interface to be able to write the objecte uniquely for these low-level
    //       objects without resorting to versioning, etc)
    public boolean   mb_IsLoaded;

    public short     msi_RequestType         = DEFAULT_REC_TYPE; //this is used for endian detection too
    public int       mi_TriggerUID           = -1;
    public ISourceID mt_SourceID;
    public int       mi_numRequestElements;
    public Vector    mt_RequestElementVector;


    /**
     * Standard constructor, empty so as to be 'poolable'
     */
    public ReadoutRequestRecord() {
    }


    /**
     * computes and returns the total size of this
     * record as it would be written as bytes.
     * @return the size in bytes of the record(including header + elements)
     */
    public int getTotalRecordSize() {
        int iSize = SIZE_HEADER + (mt_RequestElementVector.size() * ReadoutRequestElementRecord.SIZE_READOUT_REQUEST_ELEMENT_RECORD);
        return iSize;
    }

    /**
     * method to initialize a ReadoutRequestRecord.
     * @param i_TriggerUID the UID of this trigger.
     * @param t_SourceID the ISourceID makeing this request.
     * @param t_RequestElementVector Vector of IReadoutRequestElement's
     */
    public void initialize(
            int       i_TriggerUID,
            ISourceID t_SourceID,
            Vector    t_RequestElementVector
            ) {
        mb_IsLoaded = true;
        msi_RequestType           = 0x00FF; //this is used for endian detection only!
        mi_TriggerUID             = i_TriggerUID;
        mt_SourceID               = t_SourceID;
        mi_numRequestElements     = t_RequestElementVector.size();
        mt_RequestElementVector   = t_RequestElementVector;
    }

    /**
     * method to initialize a ReadoutRequestRecord.
     * @param IReadoutRequest the request which contains the information with which to init the record.
     */
    public void initialize(
            IReadoutRequest tRequest
            ) {
        mb_IsLoaded = true;
        msi_RequestType           = 0x00FF; //this is used for endian detection only!
        mi_TriggerUID             = tRequest.getUID();
        mt_SourceID               = tRequest.getSourceID();
        mt_RequestElementVector   = tRequest.getReadoutRequestElements();
        mi_numRequestElements     = mt_RequestElementVector.size();
    }

    //--[IWriteablePayloadRecord]---

    /**
     * Method to write this record to the payload destination.
     * @param tDestination PayloadDestination to which to write this record.
     * @return the number of bytes written.
     */
    public int writeData(PayloadDestination tDestination) throws IOException {
        int iBytesWritten = SIZE_HEADER;
        if (tDestination.doLabel()) tDestination.label("[ReadoutRequestRecord]=>").indent();
        //-write request-type (including endianness)
        // OFFSET_REQUEST_TYPE
        //-write triggeruid
        // OFSET_TRIGGER_UID
        //-write sourcid
        // OFFSET_SOURCEID
        //-write number of request elements
        // OFFSET_NUM_ELEMENTS
        tDestination.writeShort(REQUEST_TYPE  ,msi_RequestType);
        tDestination.writeInt(  TRIGGER_UID   ,mi_TriggerUID);
        tDestination.writeInt(  SOURCEID      ,mt_SourceID.getSourceID());
        tDestination.writeInt(  NUM_ELEMENTS  ,mi_numRequestElements);

        //-write individual request elements
        for ( int ii=0; ii < mi_numRequestElements; ii++ ) {
            ReadoutRequestElementRecord tRequestElement = (ReadoutRequestElementRecord) mt_RequestElementVector.get(ii);
            if (tDestination.doLabel()) tDestination.label("[ReadoutRequestElementRecord("+(ii+1)+" of "+mi_numRequestElements+")]=>").indent();
            iBytesWritten += tRequestElement.writeData(tDestination);
            if (tDestination.doLabel()) tDestination.undent().label("<=[ReadoutRequestElementRecord("+(ii+1)+" of "+mi_numRequestElements+")]");
        }
        if (tDestination.doLabel()) tDestination.undent().label("<=[ReadoutRequestRecord]");
        return iBytesWritten;
    }
    /**
     * Method to write this record to the payload destination.
     * @param iOffset the offset at which to start writing the object.
     * @param tBuffer the ByteBuffer into which to write this payload-record.
     * @return the number of bytes written.
     */
    public int writeData(int iRecordOffset, ByteBuffer tBuffer) throws IOException {
        int iBytesWritten = SIZE_HEADER;
        ByteOrder tSaveOrder = tBuffer.order();
        tBuffer.order(ByteOrder.BIG_ENDIAN);
        //-write request-type (including endianness)
        // OFFSET_REQUEST_TYPE
        tBuffer.putShort(iRecordOffset + OFFSET_REQUEST_TYPE, msi_RequestType);

        //-write triggeruid
        // OFSET_TRIGGER_UID
        tBuffer.putInt(iRecordOffset + OFSET_TRIGGER_UID, mi_TriggerUID);

        //-write sourcid
        // OFFSET_SOURCEID
        tBuffer.putInt(iRecordOffset + OFFSET_SOURCEID, mt_SourceID.getSourceID());

        //-write number of request elements
        // OFFSET_NUM_ELEMENTS
        tBuffer.putInt(iRecordOffset + OFFSET_NUM_ELEMENTS, mi_numRequestElements);

        //-write individual request elements
        int iCurrOffset = iRecordOffset + OFFSET_START_ELEMENTS;
        for ( int ii=0; ii < mi_numRequestElements; ii++, iCurrOffset += ReadoutRequestElementRecord.SIZE_READOUT_REQUEST_ELEMENT_RECORD ) {
            ReadoutRequestElementRecord tRequestElement = (ReadoutRequestElementRecord) mt_RequestElementVector.get(ii);
            iBytesWritten += tRequestElement.writeData(iCurrOffset, tBuffer);
        }
        tBuffer.order(tSaveOrder);
        return iBytesWritten;
    }

    //--[IPayloadRecord]---
    /**
     * Determines if this record is loaded with valid data.
     * @return true if data is loaded, false otherwise.
     */
    public boolean isDataLoaded() {
        return mb_IsLoaded;
    }

    /**
     * Loads the data from the buffer into the container record.
     * @param iRecordOffset the offset from which to start loading the data fro the engin.
     * @param tBuffer ByteBuffer from which to construct the record.
     *
     * @exception IOException if errors are detected reading the record
     * @exception DataFormatException if the record is not of the correct format.
     */
    public void loadData(int iRecordOffset, ByteBuffer tBuffer) throws IOException, DataFormatException {
        mb_IsLoaded = false;
        ByteOrder tSaveOrder = tBuffer.order();
        tBuffer.order(ByteOrder.BIG_ENDIAN);
        //-read request-type
        // OFFSET_REQUEST_TYPE
        msi_RequestType = tBuffer.getShort(iRecordOffset + OFFSET_REQUEST_TYPE);

        //-read triggeruid
        // OFSET_TRIGGER_UID
        mi_TriggerUID = tBuffer.getInt(iRecordOffset + OFSET_TRIGGER_UID);

        //-read sourcid
        // OFFSET_SOURCEID
        mt_SourceID = (ISourceID) new SourceID4B(tBuffer.getInt(iRecordOffset + OFFSET_SOURCEID));

        //-read number of request elements
        // OFFSET_NUM_ELEMENTS
        mi_numRequestElements = tBuffer.getInt(iRecordOffset + OFFSET_NUM_ELEMENTS);

        //-read individual request elements
        mt_RequestElementVector = new Vector();
        int iCurrOffset = iRecordOffset + OFFSET_START_ELEMENTS;
        for ( int ii=0; ii < mi_numRequestElements; ii++, iCurrOffset += ReadoutRequestElementRecord.SIZE_READOUT_REQUEST_ELEMENT_RECORD ) {
            ReadoutRequestElementRecord tRequestElement = (ReadoutRequestElementRecord) ReadoutRequestElementRecord.getFromPool();
            tRequestElement.loadData(iCurrOffset, tBuffer);
            mt_RequestElementVector.add(tRequestElement);
        }

        //-restore order
        tBuffer.order(tSaveOrder);
        mb_IsLoaded = true;
    }

    /**
     * Method to reset this object for reuse by a pool.
     * This is called once this Object has been used and is no longer valid.
     */
    public void dispose() {
        if ( mb_IsLoaded ) {
            mb_IsLoaded = false;
            //-recycle the loaded request elements
            for ( int ii=0; ii < mt_RequestElementVector.size(); ii++ ) {
                ((IWriteablePayloadRecord) mt_RequestElementVector.get(ii)).dispose();
            }
            mt_RequestElementVector = null;
            mi_TriggerUID = -1;
            mt_SourceID = null;
        }
    }

    //--[IReadoutRequest]---
    /**
     * getReadoutSPRequestElements()
     * returns a Vector of IReadoutRequestElement's describing the
     * readout request for a single ISourceID (ie String)
     * @return Vector Vector of IReadoutRequestElement
     */
    public Vector getReadoutRequestElements() {
        return mt_RequestElementVector;
    }

    /**
     * getUID()
     * returns the unique Trigger ID
     * by using this UID and the Stringnumber the Eventbuilder can
     * reassemble the events
     * @return int unique Trigger ID given by GlobalTrigger
     */
    public int getUID() {
        return mi_TriggerUID;
    }

    /**
     * getSourceID()
     * returns the source to which this message is send.
     * Also necessary to reassemble events in the Eventbuilder
     * @return ISourceID of the source which is accessed
     *         for fulfilling this data request.
     *         NOTE: This indicates the StringProcessor which is queried.
     *               The ISourceID should be used by lookup by the EventBuilder
     *               to identify the communications channel to which to send this
     *               request. This may be a PayloadDestination for example.
     */
    public ISourceID getSourceID() {
        return mt_SourceID;
    }
    /**
     * Method to get a useable ReadoutRequestElementRecord from a pool.
     * @return the useable record, from a pool (TODO)
     */
    public static ReadoutRequestElementRecord getUseableReadoutRequestElementRecord() {
        //-TODO: implement pooling
        return new ReadoutRequestElementRecord();
    }

    /**
     * Allows this object to know how to pool itself.
     * @return  ReadoutRequestRecord from the pool
     * TODO: implement pooling!!!!!
     */
    public static Poolable getFromPool() {
        return(Poolable) new ReadoutRequestRecord();
    }

    /**
     * Get an object from the pool in a non-static context.
     * @return object of this type from the object pool.
     */
    public Poolable getPoolable() {
        return this.getFromPool();
    }
    /**
     * Returns an instance of this object so that it can be
     * recycled, ie returned to the pool.
     */
    public void recycle() {
        dispose();
    }

}
