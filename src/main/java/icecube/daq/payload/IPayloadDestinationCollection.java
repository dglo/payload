/*
 * interface: IPayloadDestinationCollection
 *
 * Version $Id: IPayloadDestinationCollection.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: October 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.payload;

import icecube.daq.payload.splicer.Payload;

import java.util.Collection;
import java.io.IOException;

/**
 * This interface defines a collection of PayloadDestinations.
 *
 * @version $Id: IPayloadDestinationCollection.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public interface IPayloadDestinationCollection
{

    /**
     * Add a PayloadDestination associated with a SourceId to the collection.
     * @param sourceId     SourceId of this destination
     * @param destination  PayloadDestination
     */
    void addPayloadDestination(ISourceID sourceId, IPayloadDestination destination);

    /**
     * Get the PayloadDestination associated with a SourceId.
     * @param sourceId SourceId of the destination
     * @return a PayloadDestination, or null if one is not associated with the SourceId
     */
    IPayloadDestination getPayloadDestination(ISourceID sourceId);

    /**
     * Get all PayloadDestinations.
     * @return a Collection of PayloadDestinations
     */
    Collection getAllPayloadDestinations();

    /**
     * Get all PayloadDestination SourceIds.
     * @return a Collection of SourceIds
     */
    Collection getAllSourceIDs();

    /**
     * Write a Payload to a destination specified by SourceId.
     * @param sourceId SourceId of destination
     * @param payload  Payload to write
     * @return number of bytes written
     * @throws IOException if there is a write error from the underlying PayloadDestination
     */
    int writePayload(ISourceID sourceId, IWriteablePayload payload) throws IOException;

    /**
     * Write a Payload to all destinations.
     * @param payload Payload to write
     * @return total number of bytes written
     * @throws IOException if there is a write error from the underlying PayloadDestination
     */
    int writePayload(IWriteablePayload payload) throws IOException;

    /**
     * Close the PayloadDestination for this SourceId.
     * @param sourceId SourceId of destination to close
     * @throws IOException if there is a close error from the underlying PayloadDestination
     */
    void closePayloadDestination(ISourceID sourceId) throws IOException;

    /**
     * Close all PayloadDestinations.
     * @throws IOException if there is a close error from the underlying PayloadDestination
     */
    void closeAllPayloadDestinations() throws IOException;

    /**
     * Register a Controller object for this collection.
     * @param controller object that will control this collection
     */
    void registerController(IPayloadDestinationCollectionController controller);

    /**
     * Stop all PayloadDestinations.
     * @throws IOException
     */
    void stopAllPayloadDestinations() throws IOException;

    /**
     * Stop a PayloadDestination that maps this sourceID.
     * @param sourceId SourceId of destination to stop
     * @throws IOException
     */
    void stopPayloadDestination(ISourceID sourceId) throws IOException;

}
