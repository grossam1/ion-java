// Copyright (c) 2011 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion;

import static com.amazon.ion.TestUtils.ensureBinary;
import static com.amazon.ion.TestUtils.ensureText;

import com.amazon.ion.impl.IonImplUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * Abstracts the various ways that {@link IonReader}s can be created, so test
 * cases can cover all the APIs.
 */
public enum ReaderMaker
{
    /**
     * Invokes {@link IonSystem#newReader(String)}.
     */
    FROM_STRING(Feature.TEXT)
    {
        @Override
        public IonReader newReader(IonSystem system, String ionText)
        {
            return system.newReader(ionText);
        }
    },


    /**
     * Invokes {@link IonSystem#newReader(byte[])} with Ion binary.
     */
    FROM_BYTES_BINARY(Feature.BINARY)
    {
        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            ionData = ensureBinary(system, ionData);
            return system.newReader(ionData);
        }
    },


    /**
     * Invokes {@link IonSystem#newReader(byte[])} with Ion text.
     */
    FROM_BYTES_TEXT(Feature.TEXT)
    {
        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            ionData = ensureText(system, ionData);
            return system.newReader(ionData);
        }
    },


    /**
     * Invokes {@link IonSystem#newReader(byte[],int,int)} with Ion binary.
     */
    FROM_BYTES_OFFSET_BINARY(Feature.BINARY)
    {
        @Override
        public int getOffset() { return 37; }

        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            ionData = ensureBinary(system, ionData);
            byte[] padded = new byte[ionData.length + 70];
            System.arraycopy(ionData, 0, padded, 37, ionData.length);
            return system.newReader(padded, 37, ionData.length);
        }
    },


    /**
     * Invokes {@link IonSystem#newReader(byte[],int,int)} with Ion text.
     */
    FROM_BYTES_OFFSET_TEXT(Feature.TEXT)
    {
        @Override
        public int getOffset() { return 37; }

        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            ionData = ensureText(system, ionData);
            byte[] padded = new byte[ionData.length + 70];
            System.arraycopy(ionData, 0, padded, 37, ionData.length);
            return system.newReader(padded, 37, ionData.length);
        }
    },


    /**
     * Invokes {@link IonSystem#newReader(InputStream)} with Ion binary.
     */
    FROM_INPUT_STREAM_BINARY(Feature.BINARY, Feature.STREAM)
    {
        @Override
        public IonReader newReader(IonSystem system, byte[] ionData,
                                   InputStreamWrapper wrapper)
            throws IOException
        {
            ionData = ensureBinary(system, ionData);
            InputStream in = new ByteArrayInputStream(ionData);
            InputStream wrapped = wrapper.wrap(in);
            return system.newReader(wrapped);
        }

        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            ionData = ensureBinary(system, ionData);
            InputStream in = new ByteArrayInputStream(ionData);
            return system.newReader(in);
        }
    },


    /**
     * Invokes {@link IonSystem#newReader(InputStream)} with Ion text.
     */
    FROM_INPUT_STREAM_TEXT(Feature.TEXT, Feature.STREAM)
    {
        @Override
        public IonReader newReader(IonSystem system, byte[] ionData,
                                   InputStreamWrapper wrapper)
            throws IOException
        {
            ionData = ensureText(system, ionData);
            InputStream in = new ByteArrayInputStream(ionData);
            InputStream wrapped = wrapper.wrap(in);
            return system.newReader(wrapped);
        }

        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            ionData = ensureText(system, ionData);
            InputStream in = new ByteArrayInputStream(ionData);
            return system.newReader(in);
        }
    },


    FROM_DOM(Feature.DOM)
    {
        @Override
        public IonReader newReader(IonSystem system, String ionText)
        {
            IonDatagram dg = system.getLoader().load(ionText);
            return system.newReader(dg);
        }

        @Override
        public IonReader newReader(IonSystem system, byte[] ionData)
        {
            IonDatagram dg = system.getLoader().load(ionData);
            return system.newReader(dg);
        }
    };


    //========================================================================

    public enum Feature { TEXT, BINARY, DOM, STREAM }

    private final EnumSet<Feature> myFeatures;


    private ReaderMaker(Feature feature1, Feature... features)
    {
        myFeatures = EnumSet.of(feature1, features);
    }


    public boolean sourceIsText()
    {
        return myFeatures.contains(Feature.TEXT);
    }

    public boolean sourceIsBinary()
    {
        return myFeatures.contains(Feature.BINARY);
    }


    public int getOffset()
    {
        return 0;
    }


    public IonReader newReader(IonSystem system, String ionText)
    {
        byte[] utf8 = IonImplUtils.utf8(ionText);
        return newReader(system, utf8);
    }


    public IonReader newReader(IonSystem system, byte[] ionData)
    {
        IonDatagram dg = system.getLoader().load(ionData);
        String ionText = dg.toString();
        return newReader(system, ionText);
    }


    public IonReader newReader(IonSystem system, byte[] ionData,
                               InputStreamWrapper wrapper)
        throws IOException
    {
        return newReader(system, ionData);
    }


    public IonReader newReader(IonSystem system, InputStream ionData)
        throws IOException
    {
        byte[] bytes = IonImplUtils.loadStreamBytes(ionData);
        return newReader(system, bytes);
    }


    public static ReaderMaker[] valuesExcluding(ReaderMaker... exclusions)
    {
        ReaderMaker[] all = values();
        ArrayList<ReaderMaker> retained =
            new ArrayList<ReaderMaker>(Arrays.asList(all));
        retained.removeAll(Arrays.asList(exclusions));
        return retained.toArray(new ReaderMaker[retained.size()]);
    }

    public static ReaderMaker[] valuesWith(Feature feature)
    {
        ReaderMaker[] all = values();
        ArrayList<ReaderMaker> retained = new ArrayList<ReaderMaker>();
        for (ReaderMaker maker : all)
        {
            if (maker.myFeatures.contains(feature))
            {
                retained.add(maker);
            }
        }
        return retained.toArray(new ReaderMaker[retained.size()]);
    }

    public static ReaderMaker[] valuesWithout(Feature feature)
    {
        ReaderMaker[] all = values();
        ArrayList<ReaderMaker> retained = new ArrayList<ReaderMaker>();
        for (ReaderMaker maker : all)
        {
            if (! maker.myFeatures.contains(feature))
            {
                retained.add(maker);
            }
        }
        return retained.toArray(new ReaderMaker[retained.size()]);
    }
}
