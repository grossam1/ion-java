// Copyright (c) 2008-2009 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion;

import java.util.Iterator;

/**
 * FIXME this replicates {@link LoadBinaryBytesSystemProcessingTest}
 * Except for how we get the bytes.
 */
public class DatagramBytesSystemProcessingTest
    extends IteratorSystemProcessingTest
{
    private byte[] myBytes;

    @Override
    protected void prepare(String text)
        throws Exception
    {
        IonLoader loader = loader();
        IonDatagram datagram = loader.load(text);
        myBytes = datagram.getBytes();
    }

    @Override
    protected Iterator<IonValue> iterate()
    {
        IonLoader loader = loader();
        IonDatagram datagram = loader.load(myBytes);
        return datagram.iterator();
    }

    @Override
    protected Iterator<IonValue> systemIterate()
    {
        IonLoader loader = loader();
        IonDatagram datagram = loader.load(myBytes);
        return datagram.systemIterator();
    }

    @Override
    protected boolean checkMissingSymbol(String expected,
                                         int expectedSymbolTableSid,
                                         int expectedLocalSid)
        throws Exception
    {
        checkMissingSymbol(expectedSymbolTableSid);

        // when missing from a shared table the symbol
        // will not have been added to the local symbols
        return false;
    }
}
