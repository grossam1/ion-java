/*
 * Copyright (c) 2007 Amazon.com, Inc.  All rights reserved.
 */

package com.amazon.ion.impl;

import com.amazon.ion.ContainedValueException;
import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonException;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.LocalSymbolTable;
import com.amazon.ion.NullValueException;
import com.amazon.ion.ValueVisitor;
import com.amazon.ion.impl.IonBinary.BufferManager;
import com.amazon.ion.system.StandardIonSystem;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public final class IonDatagramImpl
    extends IonValueImpl.list
    implements IonDatagram
{
    static private final int DATAGRAM_TYPEDESC  =
        IonConstants.makeTypeDescriptorByte(
            IonConstants.tidSexp
           ,IonConstants.lnIsDatagram
        );

    private final static String[] EMPTY_STRING_ARRAY = new String[0];


    /**
     * The system that created this datagram.
     */
    private IonSystem             _system;

    /**
     * Used while constructing, then set to null.
     */
    private SystemReader _rawStream;

    /**
     * Superset of {@link #_contents}; contains only user values.
     */
    private ArrayList<IonValue> _userContents;


    private static BufferManager make_empty_buffer()
    {
        BufferManager buffer = new BufferManager();
        try {
            buffer.writer(0).writeFixedIntValue(0xffffffff, 4);
            buffer.writer().writeFixedIntValue(IonConstants.MAGIC_TOKEN, 4);
        }
        catch (IOException e) {
             throw new IonException(e);
        }
        return buffer;
    }


    //=========================================================================

    public IonDatagramImpl(StandardIonSystem system) {
        this(system, make_empty_buffer());
    }


    public IonDatagramImpl(StandardIonSystem system, Reader ionText)
    {
        this(system, system.newLocalSymbolTable(), ionText);
    }


    public IonDatagramImpl(StandardIonSystem system,
                        LocalSymbolTable initialSymbolTable,
                        Reader ionText)
    {
        this(new SystemReader(system,
                              system.getCatalog(),
                              initialSymbolTable, ionText));

        // If the synthetic local table has anything in it, inject it.
        if (initialSymbolTable.size() != 0 || initialSymbolTable.hasImports())
        {
            IonStruct ionRep = initialSymbolTable.getIonRepresentation();
            assert ionRep.getSymbolTable() != null;
            assert _contents.size() != 0;

            _contents.add(0, ionRep);
            ((IonValueImpl)ionRep)._container = this;

            setDirty();
        }
    }


    /**
     *
     * @param buffer is filled with Ion data.
     */
    public IonDatagramImpl(StandardIonSystem system, BufferManager buffer)
    {
        this(new SystemReader(system, buffer));
    }



    /**
     * workhorse constructor this does the actual work
     */
    public IonDatagramImpl(SystemReader rawStream)
    {
        super(DATAGRAM_TYPEDESC, true);

        _userContents = new ArrayList<IonValue>();

        // This is only used during construction.
        _rawStream = rawStream;

        _system = rawStream.getSystem();
        _buffer = _rawStream.getBuffer();

        // Force reading of the first element, so we get the buffer bootstrapped
        // including the header.
        _rawStream.hasNext();

        // fake up the pos values as this is an odd case
        // Note that size is incorrect; we fix it below.
        pos_initDatagram(DATAGRAM_TYPEDESC, _buffer.buffer().size());


        // Force materialization and update of the buffer, since we're
        // injecting the initial symbol table.
        try {
            materialize();
            // This ends up in doMaterializeValue below.
        }
        catch (IOException e) {
            throw new IonException(e);
        }
        finally {
            rawStream.close();
            _rawStream = null;
        }

        // TODO this touches privates (testBinaryDataWithNegInt)
        _next_start = _buffer.buffer().size();
    }

    //=========================================================================

    // FIXME need to make add more solid, maintain symbol tables etc.

    public void add(IonValue element)
        throws ContainedValueException, NullPointerException
    {
        // FIXME here we assume that we're inserting a user value!

        LocalSymbolTable symtab;
        int userSize = _userContents.size();
        if (userSize == 0)
        {
            symtab = _system.newLocalSymbolTable();
            IonStruct ionRep = symtab.getIonRepresentation();
            _contents.add(0, ionRep);
            ((IonValueImpl)ionRep)._container = this;
        }
        else
        {
            symtab = _userContents.get(userSize - 1).getSymbolTable();
            assert symtab != null;
        }


        super.add(element);  // Clears symtab pointer
        assert element.getSymbolTable() == null;

        ((IonValueImpl)element).setSymbolTable(symtab);
        _userContents.add(element);
    }

    public void add(int index, IonValue element)
        throws ContainedValueException, NullPointerException
    {
        throw new UnsupportedOperationException();
    }

    public IonValue get(int index)
        throws NullValueException, IndexOutOfBoundsException
    {
        return _userContents.get(index);
    }

    public IonValue systemGet(int index)
        throws IndexOutOfBoundsException
    {
        return super.get(index);
    }

    public Iterator<IonValue> iterator() throws NullValueException
    {
        // TODO implement remove on UserDatagram.iterator()
        // Tricky bit is properly removing from both user and system contents.
        return new IonContainerIterator(_userContents.iterator(), false);
    }

    public Iterator<IonValue> systemIterator()
    {
        // Disable remove... what if a system value is removed?
        // What if a system value is removed?
        return new IonContainerIterator(_contents.iterator(), false);
    }

    public void clear()
    {
        // TODO implement datagram clear
        throw new UnsupportedOperationException();

    }

    public boolean isEmpty() throws NullValueException
    {
        return _userContents.isEmpty();
    }


    public boolean remove(IonValue element) throws NullValueException
    {
        // TODO may leave dead symbol tables (and/or symbols) in the datagram
        for (Iterator i = _userContents.iterator(); i.hasNext();)
        {
            IonValue child = (IonValue) i.next();
            if (child == element) // Yes, instance identity.
            {
                i.remove();
                super.remove(element);
                return true;
            }
        }
        return false;
    }

    public int size() throws NullValueException
    {
        return _userContents.size();
    }

    public int systemSize()
    {
        return super.size();
    }


    public void addTypeAnnotation(String annotation)
    {
        String message = "Datagrams do not have annotations";
        throw new UnsupportedOperationException(message);
    }


    public LocalSymbolTable getSymbolTable()
    {
        return null;
    }


    public void makeNull()
    {
        throw new UnsupportedOperationException("Cannot make a null datagram");
    }


    public void accept(ValueVisitor visitor)
        throws Exception
    {
        visitor.visit(this);
    }


    public void clearTypeAnnotations()
    {
        // No annotations, nothing to do.
    }


//    public IonContainer getContainer()
//    {
//        return null;
//    }


    /**
     * @return null
     */
    public String getFieldName()
    {
        return null;
    }


    /**
     * @return an empty array.
     */
    public String[] getTypeAnnotationStrings()
    {
        return EMPTY_STRING_ARRAY;
    }


    /**
     * @return an empty array.
     */
    public String[] getTypeAnnotations()
    {
        return EMPTY_STRING_ARRAY;
    }


    /**
     * @return false
     */
    public boolean hasTypeAnnotation(String annotation)
    {
        return false;
    }


    /**
     * @return false
     */
    public boolean isNullValue()
    {
        return false;
    }


    /**
     * Does nothing since datagrams have no annotations.
     */
    public void removeTypeAnnotation(String annotation)
    {
        // Nothing to do, no annotations here.
    }


    //=========================================================================
    // IonValueImpl overrides

    /**
     * This keeps the datagram from blowing up during updates.
     */
    @Override
    public int getTypeDescriptorAndLengthOverhead(int valuelen)
    {
        return 0;
    }

    /**
     * Overrides IonValueImpl.container copy to add code to
     * read the field name symbol ids (fieldSid)
     * @throws IOException
     */
    @Override
    protected void doMaterializeValue(IonBinary.Reader reader) throws IOException
    {
        while (_rawStream.hasNext())
        {
            IonValueImpl child = (IonValueImpl) _rawStream.next();
            assert child.getSymbolTable() != null;

            _contents.add(child);
            child._container = this;

            if (! _rawStream.currentIsHidden())
            {
                _userContents.add(child);
            }

            if (child.isDirty()) setDirty();
        }
    }


    @Override
    public void updateSymbolTable(LocalSymbolTable symtab) {
        if (this._contents != null) {
            for (IonValue v : this._contents) {
                ((IonValueImpl)v).updateSymbolTable(symtab);
            }
        }
    }

    private int updateBuffer() throws IOException
    {
        int oldSize = _buffer.buffer().size();

        if (this.isDirty()) {
            for (IonValue child : _userContents) {
                LocalSymbolTable symtab = child.getSymbolTable();
                assert symtab != null;
                ((IonValueImpl) child).updateSymbolTable(symtab);
            }
        }


        updateBuffer2(_buffer.writer(8), 8, 0);

        if (systemSize() == 0) {
            // Nothing should've been written.
            assert _buffer.writer().position() == 8;
        }
        else {
            IonValueImpl lastChild = (IonValueImpl) systemGet(systemSize() - 1);
            assert _buffer.writer().position() ==
                   lastChild.pos_getOffsetofNextValue();
        }
        _buffer.writer().truncate();

        return _buffer.buffer().size() - oldSize;
    }

    /**
     * Preconditions: isDirty().  Children's tokens not updated nor shifted.
     * @throws IOException
     */
    @Override
    protected int writeValue(IonBinary.Writer writer,
                             int cumulativePositionDelta) throws IOException
    {
        assert _hasNativeValue;

        cumulativePositionDelta = doWriteContainerContents(writer, cumulativePositionDelta);

        return cumulativePositionDelta;
    }


    /**
     *
     * @return the entire binary content of this datagram.
     * @throws IonException if there's an error encoding the data.
     */
    public byte[] toBytes() throws IonException
    {
        try
        {
            _buffer.reader().sync();   // FIXME is this correct?

            updateBuffer();

            int pos = _buffer.writer().position();
            _buffer.writer(0).writeFixedIntValue(this.pos_getOffsetofNextValue(), 4);
            _buffer.writer().setPosition(pos);

            int len = _buffer.buffer().size();
            byte[] bytes = new byte[len];
            IonBinary.Reader reader = _buffer.reader();
            reader.sync();
            reader.setPosition(0);
            reader.read(bytes, 0, len);

            return bytes;
        }
        catch (IOException e)
        {
            throw new IonException(e);
        }
    }


    public BufferManager getBuffer() {
        return this._buffer;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(getClass().getName());
        buf.append(' ');
        buf.append(_contents.toString());
        buf.append(']');
        return buf.toString();
    }
}