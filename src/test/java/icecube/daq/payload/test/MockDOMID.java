package icecube.daq.payload.test;

import icecube.daq.payload.IDOMID;

public class MockDOMID
    implements IDOMID
{
    private long id;

    public MockDOMID(long id)
    {
        this.id = id;
    }

    public Object deepCopy()
    {
        throw new Error("Unimplemented");
    }

    public long getDomIDAsLong()
    {
        return id;
    }

    public String getDomIDAsString()
    {
        throw new Error("Unimplemented");
    }
}
