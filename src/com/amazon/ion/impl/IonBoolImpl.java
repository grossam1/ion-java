/*
 * Copyright (c) 2007 Amazon.com, Inc.  All rights reserved.
 */

package com.amazon.ion.impl;

import java.io.IOException;

import com.amazon.ion.IonBool;
import com.amazon.ion.IonException;
import com.amazon.ion.NullValueException;
import com.amazon.ion.ValueVisitor;


/**
 * Implements the Ion <code>bool</code> type.
 */
public final class IonBoolImpl
    extends IonValueImpl
    implements IonBool
{
    static final int _bool_typeDesc = 
               IonConstants.makeTypeDescriptorByte(
                     IonConstants.tidBoolean
                    ,IonConstants.lnIsNullAtom
               );

    private Boolean _bool_value;

    /**
     * Constructs a null bool value.
     */
    public IonBoolImpl()
    {
        super(_bool_typeDesc);
    }
    public IonBoolImpl(int typeDesc)
    {
        super( typeDesc );
        assert pos_getType() == IonConstants.tidBoolean;
    }

    public boolean booleanValue()
        throws NullValueException
    {
        makeReady();
        if (_bool_value == null) throw new NullValueException();
        return _bool_value;
    }

    public void setValue(boolean b)
    {
        setValue(Boolean.valueOf(b));
    }

    public void setValue(Boolean b)
    {
        _bool_value = b;
        _hasNativeValue = true;
        setDirty();
    }
    
    @Override
    protected int getNativeValueLength()
    {
        return 0;
    }

    @Override
    protected int computeLowNibble(int valuelen)
    {
        assert _hasNativeValue == true;
        
        int ln = 0;
        if (_bool_value == null) {
            ln = IonConstants.lnIsNullAtom;
        }
        else if (_bool_value.equals(true)) {
            ln = IonConstants.lnBooleanTrue;
        }
        else {
            ln = IonConstants.lnBooleanFalse;
        }
        return ln;
    }


    @Override
    public synchronized boolean isNullValue()
    {
        if (!_hasNativeValue) return super.isNullValue();
        return (_bool_value == null);
    }

    @Override
    protected void doMaterializeValue(IonBinary.Reader reader)
    {
        assert this._isPositionLoaded == true && this._buffer != null;
        
        // a native value trumps a buffered value
        if (_hasNativeValue) return;
        
        // the reader will have been positioned for us
        assert reader.position() == this.pos_getOffsetAtValueTD();

        // decode the low nibble to get the boolean value
        int ln = this.pos_getLowNibble();
        switch (ln) {
        case IonConstants.lnIsNullAtom:
            _bool_value = null;
            break;
        case IonConstants.lnBooleanFalse:
            _bool_value = Boolean.FALSE;
            break;
        case IonConstants.lnBooleanTrue:
            _bool_value = Boolean.TRUE;
            break;
        default:
            throw new IonException("malformed binary boolean value");
        }
        
        _hasNativeValue = true;
    }

    
    @Override
    protected void doWriteNakedValue(IonBinary.Writer writer, int valueLen) throws IOException
    {
        throw new IonException("call not needed!");
    }
    

    public void accept(ValueVisitor visitor)
        throws Exception
    {
        makeReady();
        visitor.visit(this);
    }
}