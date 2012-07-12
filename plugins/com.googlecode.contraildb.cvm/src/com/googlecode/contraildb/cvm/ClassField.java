package com.googlecode.contraildb.cvm;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The class describes a class field
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 * @version 1.00
 */
public class ClassField
{
    // flags of thr field

    public static final int FIELD_FLAG_PUBLIC = 0x0001;
    public static final int FIELD_FLAG_PRIVATE = 0x0002;
    public static final int FIELD_FLAG_PROTECTED = 0x0004;
    public static final int FIELD_FLAG_STATIC = 0x0008;
    public static final int FIELD_FLAG_FINAL = 0x0010;
    public static final int FIELD_FLAG_VOLATILE = 0x0040;
    public static final int FIELD_FLAG_TRANSIENT = 0x0080;

    //---------------------------
    /**
     * The variable contains flags of the field
     */
    protected int i_Flags;
    /**
     * The variable contains the field name
     */
    protected String s_Name;
    /**
     * The variable contains the field type (signature)
     */
    protected String s_Type;
    /**
     * The value contains the record index in a class object which contains the field value
     */
    protected int i_FieldIndex; // we use the value to identify the field in an object record structure
    /**
     * The variable contains the static value if the field is a static one
     */
    protected Object p_StaticValue;
    /**
     * The variable contains the constant value for the field
     */
    protected int i_ConstantValue;
    /**
     * The variable contains the link to the owner class
     */
    protected MJVMClass p_OwnerClass;
    /**
     * The variable contains the UID for the field in the class
     */
    protected int i_UID;

    /**
     * Set the static value for the field
     * @param _object the new value
     */
    public void setStaticValue(Object _object)
    {
        p_StaticValue = _object;
    }

    /**
     * Get the static value for the field
     * @return the static value
     */
    public Object getStaticValue()
    {
        return p_StaticValue;
    }

    /**
     * The constructor
     * @param _index the index of the field which will be used to identify it in an class object
     * @param _owner the class owns the field, must not be null
     * @param _instream a DataInputStream contains the field data, must not be null
     * @throws java.io.IOException the exception will be thrown if there is any exception in a transport operation
     */
    public ClassField(int _index, MJVMClass _owner, DataInputStream _instream) throws IOException
    {
        p_OwnerClass = _owner;
        i_ConstantValue = -1;
        p_StaticValue = null;
        i_FieldIndex = _index;

        // flags
        i_Flags = _instream.readUnsignedShort();

        // name
        int i_nameIndex = _instream.readUnsignedShort();
        s_Name = (String) _owner.ap_ConstantPoolData[i_nameIndex];

        // type
        int i_typeIndex = _instream.readUnsignedShort();
        s_Type = (String) _owner.ap_ConstantPoolData[i_typeIndex];

        i_UID = (i_nameIndex << 16) | i_typeIndex;

        // attributes
        int i_number = _instream.readUnsignedShort();
        while (i_number > 0)
        {
            String s_name = (String) _owner.ap_ConstantPoolData[_instream.readUnsignedShort()];
            if (MJVMClass.ATTRIBUTE_CONSTANTVALUE.equals(s_name))
            {
                // read
                int i_size = _instream.readInt();
                if (i_size != 2)
                {
                    throw new IOException("Wrong size of a constant value attribute");
                }
                i_ConstantValue = _instream.readUnsignedShort();
            }
            else
            {
                // ignore
                _instream.skip(((long) _instream.readInt()) & 0xFFFFFFFF);
            }
            i_number--;
        }
    }

    /**
     * Get the UID (I repeat that the UID is unique only in the class)
     * @return the UID as integer
     */
    public int getUID()
    {
        return i_UID;
    }

    /**
     * Get the field index in class object records
     * @return the field index as integer
     */
    public int getObjectFieldIndex()
    {
        return i_FieldIndex;
    }

    /**
     * Get the field value from a class object
     * @param _object the object contains a record for the field, it can be null if the field is a static one
     * @return the value of the field as an Object, can be null
     */
    public Object getValue(MJVMObject _object)
    {
        if ((i_Flags & FIELD_FLAG_STATIC) == 0)
        {
            return _object.getFieldForIndex(i_FieldIndex);
        }
        else
        {
            return p_StaticValue;
        }
    }

    /**
     * Set the field value in a class object
     * @param _object a object contains the field record, it can be null if the field is a static one
     * @param _value an Object which will be set to the field record, can be null
     */
    public void setValue(MJVMObject _object, Object _value)
    {
        if ((i_Flags & FIELD_FLAG_STATIC) == 0)
        {
            _object.setFieldForIndex(i_FieldIndex, _value);
        }
        else
        {
            p_StaticValue = _value;
        }
    }

    /**
     * Get the constant value for the field
     * @return the constant value for the field, can be null
     */
    public Object getConstantValue()
    {
        if (i_ConstantValue < 0)
        {
            return null;
        }
        return p_OwnerClass.ap_ConstantPoolData[i_ConstantValue];
    }

    /**
     * Get the field flags
     * @return the field flags as integer
     */
    public int getFlags()
    {
        return i_Flags;
    }
}