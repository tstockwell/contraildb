package com.googlecode.contraildb.cvm;

import java.io.DataInputStream;
import java.io.IOException;


/**
 * This class describes an object contains information about a class method
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 * @version 1.00
 */
public class ClassMethod
{
    // flags of a method

    public static final int METHOD_PUBLIC = 0x0001;
    public static final int METHOD_PRIVATE = 0x0002;
    public static final int METHOD_PROTECTED = 0x0004;
    public static final int METHOD_STATIC = 0x0008;
    public static final int METHOD_FINAL = 0x0010;
    public static final int METHOD_SYNCHRONIZED = 0x0020;
    public static final int METHOD_NATIVE = 0x0100;
    public static final int METHOD_ABSTRACT = 0x0400;
    public static final int METHOD_STRICT = 0x0800;
    //-----------------------------------------
    // types of data
    public static final char TYPE_BYTE = 'B';
    public static final char TYPE_CHAR = 'C';
    public static final char TYPE_DOUBLE = 'D';
    public static final char TYPE_FLOAT = 'F';
    public static final char TYPE_INT = 'I';
    public static final char TYPE_LONG = 'L';
    public static final char TYPE_SHORT = 'S';
    public static final char TYPE_BOOLEAN = 'Z';
    public static final char TYPE_VOID = 'V';
    public static final char TYPE_CLASS = 'L';
    public static final char TYPE_ARRAY = '[';
    //-----------------------------------------
    /**
     * The variable contains method flags
     */
    protected int i_MethodFlags;
    /**
     * The variable contains the string representation of the method name
     */
    protected String s_Name;
    /**
     * The variable contains the method prototype (signature)
     */
    protected String s_Prototype;
    /**
     * The array contains the exception list for the method
     */
    protected String[] as_Exceptions;
    /**
     * The variable contains the link to the class owner of the method
     */
    protected MJVMClass p_Owner;
    /**
     * The variable contains the value of the maximum stack depth for the executing code of the method
     */
    protected int i_MaxStackDepth;
    /**
     * The variable contains the number of local variables
     */
    protected int i_LocalVariableNumber;
    /**
     * The array contains the bytecode of the method
     */
    protected byte[] ab_Bytecode;
    /**
     * The list contains exception records for the bytecode
     */
    protected MethodExceptionRecord[] ap_MethodExceptionRecords;
    /**
     * The UID of the method, the vaue is unique in the class
     */
    protected int i_UID;

    /**
     * The constructor
     * @param _owner the class which is the owner for the method, myst not be null
     * @param _instream a DataInputStream is being used to read the data of the method, must not be null
     * @throws java.io.IOException the exception will be thrown if there is any problem in a transport operation
     */
    public ClassMethod(MJVMClass _owner, DataInputStream _instream) throws IOException
    {
        // set the owner
        p_Owner = _owner;

        // read the method flags
        i_MethodFlags = _instream.readUnsignedShort();

        // read the name index
        int i_nameIndex = _instream.readUnsignedShort();
        // read the method signature index
        int i_prototypeIndex = _instream.readUnsignedShort();

        // convert indexes into String reresentation
        s_Name = (String) _owner.ap_ConstantPoolData[i_nameIndex];
        s_Prototype = (String) _owner.ap_ConstantPoolData[i_prototypeIndex];

        // make the UID for the method from its name and signature indexes
        i_UID = (i_nameIndex << 16) | i_prototypeIndex;

        // read the method attributes
        int i_attributesNumber = _instream.readUnsignedShort();
        while (i_attributesNumber > 0)
        {
            String s_name = (String) _owner.ap_ConstantPoolData[_instream.readUnsignedShort()];
            // read the size of the attribute data
            int i_size = _instream.readInt();
            if (MJVMClass.ATTRIBUTE_EXCEPTIONS.equals(s_name))
            {
                // read exceptions table for the method i.e. the tale contains exceptions which can be thrown by the method
                int i_number = _instream.readUnsignedShort();
                as_Exceptions = new String[i_number];
                for (int li = 0; li < i_number; li++)
                {
                    as_Exceptions[li] = (String) _owner.ap_ConstantPoolData[_instream.readUnsignedShort()];
                }
            }
            else
            {
                if (MJVMClass.ATTRIBUTE_CODE.equals(s_name))
                {
                    // read the method bytecode and its attributes
                    i_MaxStackDepth = _instream.readUnsignedShort();
                    i_LocalVariableNumber = _instream.readUnsignedShort();
                    int i_number = _instream.readInt();
                    ab_Bytecode = new byte[i_number];
                    _instream.readFully(ab_Bytecode);

                    // read the table of exception processors for the bytecode
                    i_number = _instream.readUnsignedShort();
                    ap_MethodExceptionRecords = new MethodExceptionRecord[i_number];
                    for (int li = 0; li < i_number; li++)
                    {
                        ap_MethodExceptionRecords[li] = new MethodExceptionRecord(_owner.ap_ConstantPoolData, _instream);
                    }

                    // skip all other attributes
                    MJVMClass._skipAttributes(_instream);
                }
                else
                {
                    // skip other attribute data
                    _instream.skip(i_size);
                }
            }

            i_attributesNumber--;
        }

        // it would be good for us to keep arrays as objects but null
        if (as_Exceptions == null)
        {
            as_Exceptions = new String[0];
        }
        if (ap_MethodExceptionRecords == null)
        {
            ap_MethodExceptionRecords = new MethodExceptionRecord[0];
        }
    }
}