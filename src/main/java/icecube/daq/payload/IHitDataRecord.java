package icecube.daq.payload;


/**
 *  This object is meant to a thin wrapper around the actual
 *  record data associated with a HIT. It identifies the record
 *  type, and from that the object itself can be interpretted.
 *
 *  Strictly speaking, objects which implement this interface are self
 *  identifying containers for variable format data records.
 *
 *  NOTE: 12-11-04 This is meant to provide a placehold for better
 *        insulation of the data format.  At this time the only
 *        type of record that will be returned by this interface
 *        is a DomHitEngineeringFormatRecord - dwharton.
 *
 * @author dwharton
 */
public interface IHitDataRecord
{
    /**
     * Returns the type code used for the interpretation of this record so
     * that the object which is returned can be formatted/interpreted correctly.
     * @return the type of record, as identified by the RecordRegistry
     */
    int getRecordType();

    /**
     * Returns the particular version of this record type.
     * @return the version of this record type.
     */
    int getVersion();

    /**
     * Returns the record itself, generically as an object.
     *
     * @return the record which contains the Hit data, which is interpretted
     *         by the above id's
     */
    Object getRecord();
}
