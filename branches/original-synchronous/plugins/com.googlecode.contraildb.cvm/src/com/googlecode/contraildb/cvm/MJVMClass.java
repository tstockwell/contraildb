/*
 * MJVM - Copyright (C) 2009  Igor Maznitsa www.igormaznitsa.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.googlecode.contraildb.cvm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The class allows to load and parse a Java class file from an InputStream and to execute its bytecode.
 * At present it doesn't support Float and Double data types, also it does not support anysynchronization
 * The class was developed to use Java class files as scripts on Java platforms which don't support ClassLoaders (as an example
 * The class has been written in Java 1.1 style to be used without problems in J2ME devices
 *
 * @author Igor Maznitsa
 * @version 1.03
 */
public final class MJVMClass
{

    /**
     * A constant pool UTF8 string item
     */
    public static final int CONSTANT_UTF8 = 1;
    /**
     * A constant pool UNICODE string item
     */
    public static final int CONSTANT_UNICODE = 2;
    /**
     * A constant pool INTEGER item
     */
    public static final int CONSTANT_INTEGER = 3;
    /**
     * A constant pool FLOAT item
     */
    public static final int CONSTANT_FLOAT = 4;
    /**
     * A constant pool LONG item
     */
    public static final int CONSTANT_LONG = 5;
    /**
     * A constant pool DOUBLE item
     */
    public static final int CONSTANT_DOUBLE = 6;
    /**
     * A constant pool class reference item
     */
    public static final int CONSTANT_CLASSREF = 7;
    /**
     * A constant pool string reference item
     */
    public static final int CONSTANT_STRING = 8;
    /**
     * A constant pool class field item
     */
    public static final int CONSTANT_FIELDREF = 9;
    /**
     * A constant pool class method item
     */
    public static final int CONSTANT_METHODREF = 10;
    /**
     * A constant pool interface method item
     */
    public static final int CONSTANT_INTERFACEMETHOD = 11;
    /**
     * A constant pool name-type item
     */
    public static final int CONSTANT_NAMETYPEREF = 12;
    //-----------------------------
    public static final int CLASS_FLAG_PUBLIC = 0x0001;
    public static final int CLASS_FLAG_FINAL = 0x0010;
    public static final int CLASS_FLAG_SUPER = 0x0020;
    public static final int CLASS_FLAG_INTERFACE = 0x0200;
    public static final int CLASS_FLAG_ABSTRACT = 0x0400;
    //-----------------------------
    // inside string attribute identifiers
    static final String ATTRIBUTE_EXCEPTIONS = "Exceptions";
    static final String ATTRIBUTE_CODE = "Code";
    static final String ATTRIBUTE_CONSTANTVALUE = "ConstantValue";
    //-----------------------------
    /**
     * The variable contains the packed version data of the compiler which has produced the class file
     */
    protected int i_Version;
    /**
     * The variable contains the class flags
     */
    protected int i_ClassFlags;
    /**
     * The variable contains the index of the class name in the SUN class name format
     */
    protected int i_ClassName;
    /**
     * The variable contains the index of the base class name in the SUN class name format
     */
    protected int i_BaseClassName;
    /**
     * The array contains the list of interfaces implemented by the class. Every interface as a String in the SUN class name format
     */
    protected String[] as_Interfaces;
    /**
     * The hashtable contains all fields of the class in the <Integer (UID),ClassField> format
     */
    protected Hashtable p_FieldsTable;
    /**
     * The hashtable contains all fields of the class in the <Integer (UID),ClassMethod> format
     */
    protected Hashtable p_MethodsTable;
    /**
     * The variable contains the link to the processor to process system calls in an execution time
     */
    protected MJVMProcessor p_Processor;
    /**
     * The array contains identifiers of constant pool items
     */
    protected byte[] ab_ConstantPoolTypes;
    /**
     * The array contains all objects of the class constant pool
     */
    protected Object[] ap_ConstantPoolData;
    /**
     * The variable contains simple class name
     */
    protected String s_SimpleClassName;

    /**
     * The constructor
     * @param _classInputStream an InputStream is being read for the class data, must not be null. The stream will not be closed automatically because you can have several classes in the same stream sequentially.
     * @param _processor a processor object which will be used for class operations
     * @throws java.io.IOException the exception will be thrown if there is any problem in a transport operation
     */
    public MJVMClass(InputStream _classInputStream, MJVMProcessor _processor) throws IOException
    {
        if (_processor == null)
        {
            throw new NullPointerException("The processor object must not be null");
        }
        if (_classInputStream == null)
        {
            throw new NullPointerException("The input stream must not be null");
        }

        p_Processor = _processor;

        DataInputStream p_datastream = new DataInputStream(_classInputStream);

        // check the header
        if (p_datastream.readInt() != 0xCAFEBABE)
        {
            throw new IOException("It's not a java class");
        }

        // read the version
        i_Version = p_datastream.readInt();

        // read the constant pool
        _readConstantPool(p_datastream);

        // class flags
        i_ClassFlags = p_datastream.readUnsignedShort();

        // class name
        i_ClassName = p_datastream.readUnsignedShort();

        // get the simple class name (it will be useful to find constructors)
        String s_name = getClassName();
        int i_lastCharPackage = s_name.lastIndexOf('/');

        if (i_lastCharPackage < 0)
        {
            // there is not package in the name
            s_SimpleClassName = s_name;
        }
        else
        {
            // extract pure name
            s_SimpleClassName = s_name.substring(i_lastCharPackage + 1);
        }

        // base class name
        i_BaseClassName = p_datastream.readUnsignedShort();

        // interfaces
        int i_number = p_datastream.readUnsignedShort();
        as_Interfaces = new String[i_number];
        for (int li = 0; li < i_number; li++)
        {
            as_Interfaces[li] = (String) ap_ConstantPoolData[p_datastream.readUnsignedShort()];
        }

        // fields
        _loadFields(p_datastream);

        // methods
        _loadMethods(p_datastream);

        // skip all attributes
        _skipAttributes(p_datastream);

        // init static variables
        ClassMethod p_clinitMethod = findMethodForName("<clinit>", "()V");
        if (p_clinitMethod != null)
        {
            // there are some static data to be initialized
            try
            {
                invoke(p_clinitMethod, null, null);
            }
            catch (Throwable _thr)
            {
                _thr.printStackTrace();
                throw new IOException("Can't initialize the class static data");
            }
        }
    }

    /**
     * The function returns the array contains all interfaces implemented by the class
     * @return a String array contains the list of interfaces implemented by the class, every interface name is in the standard SUN class name format
     */
    public String[] getInterfaces()
    {
        return as_Interfaces;
    }

    /**
     * To get the name of the class name
     * @return the class name as String in the SUN standart format for class names i.e. "com/package/test"
     */
    public String getClassName()
    {
        return (String) ap_ConstantPoolData[((Integer) ap_ConstantPoolData[i_ClassName]).intValue()];
    }

    /**
     * To get the simple class name (it means only a class name without a package name part)
     * @return the simple class name as String
     */
    public String getSimpleClassName()
    {
        return s_SimpleClassName;
    }

    /**
     * To get the name of the base class name which is the ancestor for the class
     * @return the base class name as String in the SUN standart format for class names i.e. "com/package/test"
     */
    public String getBaseClassName()
    {
        return (String) ap_ConstantPoolData[((Integer) ap_ConstantPoolData[i_BaseClassName]).intValue()];
    }

    /**
     * To get the class file information about the compiler which was being used to generate the class file
     * @return the version as an integer value in the format (minor_version << 16) | major_version
     */
    public final int getCompilerVersion()
    {
        return i_Version;
    }

    /**
     * Get a constant pool item for an index
     * @param _constantPoolIndex the index of an item
     * @return a constant pool item as Object, can be null
     */
    public final Object getConstantPoolItemForIndex(int _constantPoolIndex)
    {
        return ap_ConstantPoolData[_constantPoolIndex];
    }

    /**
     * Get a constant pool item type for an index
     * @param _constantPoolIndex the index of an item
     * @return the item type as integer
     */
    public final int getConstantPoolItemTypeForIndex(int _constantPoolIndex)
    {
        return ab_ConstantPoolTypes[_constantPoolIndex] & 0xFF;
    }

    /**
     * Get the size of the class constant pool
     * @return the constant pool size as integer
     */
    public final int getConstantPoolSize()
    {
        return ab_ConstantPoolTypes.length;
    }

    /**
     * This function loads methods of a class from a DataInputStream
     * @param _stream a DataInputStream contains class data, must not be null
     * @throws java.io.IOException the exception will be thrown if there is any problem at transport operations.
     */
    private final void _loadMethods(DataInputStream _stream) throws IOException
    {
        int i_number = _stream.readUnsignedShort();
        p_MethodsTable = new Hashtable(i_number);
        for (int li = 0; li < i_number; li++)
        {
            ClassMethod p_method = new ClassMethod(this, _stream);
            p_MethodsTable.put(new Integer(p_method.i_UID), p_method);
        }
    }

    /**
     * The function loads fields of a class from a DataInputStream
     * @param _stream a DataInputStream contains the class data, must not be null
     * @throws java.io.IOException the exception will be thrown if there is any exception in a transport operation
     */
    private void _loadFields(DataInputStream _stream) throws IOException
    {
        int i_number = _stream.readUnsignedShort();
        p_FieldsTable = new Hashtable(i_number);
        for (int li = 0; li < i_number; li++)
        {
            ClassField p_field = new ClassField(li, this, _stream);
            p_FieldsTable.put(new Integer(p_field.i_UID), p_field);
        }
    }

    /**
     * To skip all attribute data in the incomming class
     * @param _stream a DataInputStream contains the class data, must not be null
     * @throws java.io.IOException the exception will be thrown if there is any exception in a transport operation
     */
    static void _skipAttributes(DataInputStream _stream) throws IOException
    {
        int i_number = _stream.readUnsignedShort();
        while (i_number > 0)
        {
            // skip name
            _stream.skip(2);
            //skip data
            int i_length = _stream.readInt();
            _stream.skip(i_length);
            i_number--;
        }
    }

    /**
     * Read the constant pool of a class from a DataInputStream
     * @param _instream a DataInputStream contains the class data, must not be null
     * @throws java.io.IOException
     */
    private final void _readConstantPool(DataInputStream _instream) throws IOException
    {
        // read the item number from the stream
        int i_poolitemnumber = _instream.readUnsignedShort();

        // create arrays to place incomming parsed data
        final byte[] ab_cptypes = new byte[i_poolitemnumber];
        final Object[] ap_cpdata = new Object[i_poolitemnumber];

        // the zero indexed item
        ab_cptypes[0] = 0;
        ap_cpdata[0] = null;

        // read items
        for (int li = 1; li < i_poolitemnumber; li++)
        {
            // read the item type
            int i_datatype = _instream.readUnsignedByte();
            ab_cptypes[li] = (byte) i_datatype;

            // read the item data
            switch (i_datatype)
            {
                case CONSTANT_UTF8:
                    {
                        ap_cpdata[li] = _instream.readUTF();
                    }
                    break;
                case CONSTANT_UNICODE:
                    {
                        int i_chars = _instream.readUnsignedShort();
                        StringBuffer p_buffer = new StringBuffer(i_chars);
                        for (int lc = 0; lc < i_chars; lc++)
                        {
                            char ch_char = (char) _instream.readUnsignedShort();
                            p_buffer.append(ch_char);
                        }
                        ap_cpdata[li] = p_buffer.toString();
                    }
                    break;
                case CONSTANT_INTEGER:
                    ap_cpdata[li] = new Integer(_instream.readInt());
                    break;
                case CONSTANT_FLOAT:
                    throw new IllegalArgumentException("Float"); // currently is not supported
                case CONSTANT_LONG:
                    ap_cpdata[li] = new Long(_instream.readLong());
                    break;
                case CONSTANT_DOUBLE:
                    throw new IllegalArgumentException("Double"); // currently is not supported
                case CONSTANT_CLASSREF:
                    ap_cpdata[li] = new Integer(_instream.readUnsignedShort());
                    break;
                case CONSTANT_STRING:
                    ap_cpdata[li] = new Integer(_instream.readUnsignedShort());
                    break;

                case CONSTANT_FIELDREF:
                case CONSTANT_METHODREF:
                case CONSTANT_INTERFACEMETHOD:
                    {
                        int i_classref = _instream.readUnsignedShort();
                        int i_typenameref = _instream.readUnsignedShort();
                        ap_cpdata[li] = new Integer((i_classref << 16) | i_typenameref);
                    }
                    break;

                case CONSTANT_NAMETYPEREF:
                    {
                        int i_nameref = _instream.readUnsignedShort();
                        int i_descrref = _instream.readUnsignedShort();
                        ap_cpdata[li] = new Integer((i_nameref << 16) | i_descrref);
                    }
                    break;

                default:
                {
                    throw new IOException("Unsupported constant pool item [" + i_datatype + ']');
                }
            }
        }

        ap_ConstantPoolData = ap_cpdata;
        ab_ConstantPoolTypes = ab_cptypes;
    }

    /**
     * This function allows to find an information about a class field
     * @param _name the name of a class field (must not be null)
     * @return a ClassField object contains information about the field or null if the field is not found
     */
    public final ClassField findFieldForName(String _name)
    {
        Enumeration p_fields = p_FieldsTable.elements();
        while (p_fields.hasMoreElements())
        {
            ClassField p_field = (ClassField) p_fields.nextElement();
            if (_name.equals(p_field.s_Name))
            {
                return p_field;
            }
        }
        return null;
    }

    /**
     * This function allows to find a method of the class for its name and the prototype
     * @param _name the method name (must not be null) as String, as an example "test"
     * @param _prototype the method prototype (signature), as an example "()V" for the method "void test()"
     * @return a ClassMethod object contains data of the method of null if the method is not found in the class
     */
    public final ClassMethod findMethodForName(String _name, String _prototype)
    {
        Enumeration p_methods = p_MethodsTable.elements();
        while (p_methods.hasMoreElements())
        {
            ClassMethod p_method = (ClassMethod) p_methods.nextElement();
            if (_name.equals(p_method.s_Name))
            {
                _prototype.equals(p_method.s_Prototype);
                return p_method;
            }
        }
        return null;
    }

    /**
     * Invoke a method of the class
     * @param _method a ClassMethod object to be invoked, must not be null and must be a ClassMethod of the class
     * @param _object a MJVMObject which contains object data for the class, it can be null if the method uses only static data
     * @param _arguments arguments for the method, it can be null if the method doesn't need arguments
     * @return an object or null as a result of the method work
     * @throws java.lang.Throwable the exception will be thrown if there is any unprocessed exception during the bytecode processing
     */
    public Object invoke(ClassMethod _method, MJVMObject _object, Object[] _arguments) throws Throwable
    {
        // implementation of synchronization mechanism
        final int i_flags = _method.i_MethodFlags;
        if ((i_flags & ClassMethod.METHOD_SYNCHRONIZED) != 0)
        {
            // it's a synchronized method
            Object p_synchroObject = null;

            if ((i_flags & ClassMethod.METHOD_STATIC) != 0)
            {
                // it's a static method
                // we need to use class as the synchro object
                p_synchroObject = this;
            }
            else
            {
                // it's a nonstatic method
                // we need to use the instance as the synchro object
                p_synchroObject = _object;
            }

            synchronized (p_synchroObject)
            {
                return _invoke(_method, _object, _arguments);
            }
        }
        else
        {
            // it's not a synchronized method and we just call inside invoke function
            return _invoke(_method, _object, _arguments);
        }
    }

    /**
     * Internal invoke a method of the class
     * @param _method a ClassMethod object to be invoked, must not be null and must be a ClassMethod of the class
     * @param _object a MJVMObject which contains object data for the class, it can be null if the method uses only static data
     * @param _arguments arguments for the method, it can be null if the method doesn't need arguments
     * @return an object or null as a result of the method work
     * @throws java.lang.Throwable the exception will be thrown if there is any unprocessed exception during the bytecode processing
     */
    private Object _invoke(ClassMethod _method, MJVMObject _object, Object[] _arguments) throws Throwable
    {
        // init registers
        int i_SPreg = 0;
        int i_PCreg = 0;

        // create the method stack and local vaiable tables
        Object[] ap_localStack = new Object[_method.i_MaxStackDepth];
        Object[] ap_localVariables = new Object[_method.i_LocalVariableNumber];

        // the variable contains the first local variable index contains the first method argument
        int i_firstArgumentOffset = 0;

        final int i_methodFlags = _method.i_MethodFlags;

        // check the method flags
        if ((i_methodFlags & (ClassMethod.METHOD_ABSTRACT | ClassMethod.METHOD_SYNCHRONIZED | ClassMethod.METHOD_STRICT)) != 0)
        {
            // decoding
            if ((i_methodFlags & ClassMethod.METHOD_ABSTRACT) != 0)
            {
                throw new IllegalArgumentException("It's an abstract method");
            }
            if ((i_methodFlags & ClassMethod.METHOD_STRICT) != 0)
            {
                throw new IllegalArgumentException("I don't support strict methods yet");
            }
        }

        // if the method is not static, we will need to place "this" in the zero-indexed local variable
        if ((i_methodFlags & ClassMethod.METHOD_STATIC) == 0)
        {
            // place "this"
            ap_localVariables[0] = _object;
            // the first argument will be at the index 1
            i_firstArgumentOffset = 1;
        }

        // fill the method stack with arguments
        if (_arguments != null)
        {
            for (int li = 0; li < _arguments.length; li++)
            {
                ap_localVariables[i_firstArgumentOffset] = _arguments[li];
                i_firstArgumentOffset++;
            }
        }

        // the string below to increase the speed
        final byte[] ab_bytecodes = _method.ab_Bytecode;

        // the flag will be used by the WIDE command
        boolean lg_wideFlag = false;

        while (true)
        {
            // remember the value of the PC register
            final int i_prevPC = i_PCreg;
            try
            {
                // read a bytecode
                final int i_bytecode = ab_bytecodes[i_PCreg++] & 0xFF;

                switch (i_bytecode)
                {
                    // NOP
                    case 0:
                        {
                        }
                        break;
                    // ACONST_NULL
                    case 1:
                        {
                            ap_localStack[i_SPreg++] = null;
                        }
                        break;
                    // ICONST_M1
                    case 2:
                        {
                            ap_localStack[i_SPreg++] = new Integer(-1);
                        }
                        break;
                    // ICONST_0,1,2,3,4,5
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                        {
                            ap_localStack[i_SPreg++] = new Integer(i_bytecode - 3);
                        }
                        break;
                    // LCONST_0, LCONST_1
                    case 9:
                    case 10:
                        {
                            ap_localStack[i_SPreg++] = new Long(i_bytecode - 9);
                        }
                        break;
                    // FCONST_0,FCONST_1,FCONST_2
                    case 11:
                    case 12:
                    case 13:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DCONST_0, DCONST_1
                    case 14:
                    case 15:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // BIPUSH
                    case 16:
                        {
                            ap_localStack[i_SPreg++] = new Integer(ab_bytecodes[i_PCreg++]);
                        }
                        break;
                    // SIPUSH
                    case 17:
                        {
                            int i_value = _readShortFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 2;
                            ap_localStack[i_SPreg++] = new Integer(i_value);
                        }
                        break;
                    // LDC1,LDC2
                    case 18:
                    case 19:
                        {
                            int i_index = ab_bytecodes[i_PCreg++] & 0xFF;
                            if (i_bytecode == 19)
                            {
                                i_index = (i_index << 8) | (ab_bytecodes[i_PCreg++] & 0xFF);
                            }

                            switch (ab_ConstantPoolTypes[i_index] & 0xFF)
                            {
                                case CONSTANT_INTEGER:
                                case CONSTANT_UTF8:
                                case CONSTANT_UNICODE:
                                case CONSTANT_LONG:
                                    ap_localStack[i_SPreg++] = ap_ConstantPoolData[i_index];
                                    break;
                                case CONSTANT_STRING:
                                    ap_localStack[i_SPreg++] = ap_ConstantPoolData[((Integer) ap_ConstantPoolData[i_index]).intValue()];
                                    break;
                                default:
                                    throw new Error("Unsupported constant type at LDC");
                            }
                        }
                        break;
                    // LDC2W
                    case 20:
                        {
                            int i_value = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;
                            ap_localStack[i_SPreg++] = ap_ConstantPoolData[i_value];
                        }
                        break;
                    // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        {
                            int i_index = ab_bytecodes[i_PCreg++] & 0xFF;

                            if (lg_wideFlag)
                            {
                                i_index = (i_index << 8) | (ab_bytecodes[i_PCreg++] & 0xFF);
                                lg_wideFlag = false;
                            }

                            ap_localStack[i_SPreg++] = ap_localVariables[i_index];
                        }
                        break;
                    // ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                        {
                            ap_localStack[i_SPreg++] = ap_localVariables[i_bytecode - 26];
                        }
                        break;
                    // LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                        {
                            ap_localStack[i_SPreg++] = ap_localVariables[i_bytecode - 30];
                        }
                        break;
                    // FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                        {
                            ap_localStack[i_SPreg++] = ap_localVariables[i_bytecode - 34];
                        }
                        break;
                    // DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                        {
                            ap_localStack[i_SPreg++] = ap_localVariables[i_bytecode - 38];
                        }
                        break;
                    // ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                        {
                            ap_localStack[i_SPreg++] = ap_localVariables[i_bytecode - 42];
                        }
                        break;
                    // IALOAD
                    case 46:
                        {
                            int i_index = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int[] ai_array = (int[]) ap_localStack[--i_SPreg];
                            ap_localStack[i_SPreg++] = new Integer(ai_array[i_index]);
                        }
                        break;
                    // LALOAD
                    case 47:
                        {
                            int i_index = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            long[] al_array = (long[]) ap_localStack[--i_SPreg];
                            ap_localStack[i_SPreg++] = new Long(al_array[i_index]);
                        }
                        break;
                    // FALOAD
                    case 48:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DALOAD
                    case 49:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // AALOAD
                    case 50:
                        {
                            int i_index = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            Object[] ap_array = (Object[]) ap_localStack[--i_SPreg];
                            ap_localStack[i_SPreg++] = ap_array[i_index];
                        }
                        break;
                    // BALOAD
                    case 51:
                        {
                            int i_index = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            Object p_arr = ap_localStack[--i_SPreg];
                            if (p_arr instanceof boolean[])
                            {
                                boolean[] alg_array = (boolean[]) ap_localStack[--i_SPreg];
                                ap_localStack[i_SPreg++] = new Integer(alg_array[i_index] ? 1 : 0);
                            }
                            else
                            {
                                // byte
                                byte[] ab_array = (byte[]) ap_localStack[--i_SPreg];
                                ap_localStack[i_SPreg++] = new Integer(ab_array[i_index]);
                            }
                        }
                        break;
                    // CALOAD
                    case 52:
                        {
                            int i_index = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            char[] ach_array = (char[]) ap_localStack[--i_SPreg];
                            ap_localStack[i_SPreg++] = new Integer(ach_array[i_index]);
                        }
                        break;
                    // SALOAD
                    case 53:
                        {
                            int i_index = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            short[] ash_array = (short[]) ap_localStack[--i_SPreg];
                            ap_localStack[i_SPreg++] = new Integer(ash_array[i_index]);
                        }
                        break;
                    // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                        {
                            int i_index = ab_bytecodes[i_PCreg++] & 0xFF;

                            if (lg_wideFlag)
                            {
                                i_index = (i_index << 8) | (ab_bytecodes[i_PCreg++] & 0xFF);
                                lg_wideFlag = false;
                            }

                            ap_localVariables[i_index] = ap_localStack[--i_SPreg];
                        }
                        break;
                    // ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                        {
                            Integer p_integer = (Integer) ap_localStack[--i_SPreg];
                            ap_localVariables[i_bytecode - 59] = p_integer;
                        }
                        break;
                    // LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3
                    case 63:
                    case 64:
                    case 65:
                    case 66:
                        {
                            Long p_value = (Long) ap_localStack[--i_SPreg];
                            ap_localVariables[i_bytecode - 63] = p_value;
                        }
                        break;
                    // FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                        {
                            Object p_value = ap_localStack[--i_SPreg];
                            ap_localVariables[i_bytecode - 75] = p_value;
                        }
                        break;
                    // IASTORE
                    case 79:
                        {
                            Integer p_value = (Integer) ap_localStack[--i_SPreg];
                            Integer p_index = (Integer) ap_localStack[--i_SPreg];
                            int[] ap_array = (int[]) ap_localStack[--i_SPreg];
                            ap_array[p_index.intValue()] = p_value.intValue();
                        }
                        break;
                    // LASTORE
                    case 80:
                        {
                            Long p_value = (Long) ap_localStack[--i_SPreg];
                            Integer p_index = (Integer) ap_localStack[--i_SPreg];
                            long[] ap_array = (long[]) ap_localStack[--i_SPreg];
                            ap_array[p_index.intValue()] = p_value.longValue();
                        }
                        break;
                    // FASTORE
                    case 81:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DASTORE
                    case 82:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // AASTORE
                    case 83:
                        {
                            Object p_value = ap_localStack[--i_SPreg];
                            Integer p_index = (Integer) ap_localStack[--i_SPreg];
                            Object[] ap_array = (Object[]) ap_localStack[--i_SPreg];
                            ap_array[p_index.intValue()] = p_value;
                        }
                        break;
                    // BASTORE
                    case 84:
                        {
                            Integer p_value = (Integer) ap_localStack[--i_SPreg];
                            Integer p_index = (Integer) ap_localStack[--i_SPreg];

                            Object p_arr = ap_localStack[--i_PCreg];
                            if (p_arr instanceof boolean[])
                            {
                                boolean[] alg_arr = (boolean[]) p_arr;
                                alg_arr[p_index.intValue()] = p_value.intValue() != 0;
                            }
                            else
                            {
                                byte[] ab_arr = (byte[]) p_arr;
                                ab_arr[p_index.intValue()] = (byte) p_value.intValue();
                            }
                        }
                        break;
                    // CASTORE, SASTORE
                    case 85:
                    case 86:
                        {
                            Integer p_value = (Integer) ap_localStack[--i_SPreg];
                            Integer p_index = (Integer) ap_localStack[--i_SPreg];

                            if (i_bytecode == 85)
                            {
                                char[] ap_array = (char[]) ap_localStack[--i_SPreg];
                                ap_array[p_index.intValue()] = (char) p_value.intValue();
                            }
                            else
                            {
                                short[] ap_array = (short[]) ap_localStack[--i_SPreg];
                                ap_array[p_index.intValue()] = (short) p_value.intValue();
                            }
                        }
                        break;

                    // POP
                    case 87:
                        {
                            ap_localStack[--i_SPreg] = null;
                        }
                        break;

                    // POP2
                    case 88:
                        {
                            Object p_obj = ap_localStack[--i_SPreg];
                            ap_localStack[i_SPreg] = null;
                            if (!(p_obj instanceof Long))
                            {
                                ap_localStack[--i_SPreg] = null;
                            }
                        }
                        break;

                    // DUP
                    case 89:
                        {
                            Object p_obj = ap_localStack[i_SPreg - 1];
                            ap_localStack[i_SPreg++] = p_obj;
                        }
                        break;

                    // DUP_X1 (X Y -> Y X Y)
                    case 90:
                        {
                            Object p_top = ap_localStack[i_SPreg - 1];
                            Object p_sec = ap_localStack[i_SPreg - 2];
                            ap_localStack[i_SPreg++] = p_top;
                            ap_localStack[i_SPreg - 3] = p_top;
                            ap_localStack[i_SPreg - 2] = p_sec;
                        }
                        break;

                    // DUP_X2 (A1 B1 C1 -> C1 A1 B1 C1 | A2 B1 -> B1 A2 B1)
                    case 91:
                        {
                            Object p_top = ap_localStack[i_SPreg - 1];
                            Object p_sec = ap_localStack[i_SPreg - 2];

                            if (p_sec instanceof Long)
                            {
                                // form 2
                                ap_localStack[i_SPreg++] = p_top;
                                ap_localStack[i_SPreg - 3] = p_top;
                                ap_localStack[i_SPreg - 2] = p_sec;
                            }
                            else
                            {
                                // form 1
                                Object p_third = ap_localStack[i_SPreg - 3];
                                ap_localStack[i_SPreg++] = p_top; // a b c -> a b c c
                                ap_localStack[i_SPreg - 2] = p_sec; // a b c c -> a b b c
                                ap_localStack[i_SPreg - 3] = p_third; // a b b c -> a a b c
                                ap_localStack[i_SPreg - 4] = p_top; // a a b c -> c a b c
                            }
                        }
                        break;
                    // DUP2 (A1 B1 -> A1 B1 A1 B1 | A2 -> A2 A2)
                    case 92:
                        {
                            Object p_top = ap_localStack[i_SPreg - 1];

                            if (p_top instanceof Long)
                            {
                                // form 2
                                ap_localStack[i_SPreg++] = p_top;
                            }
                            else
                            {
                                // form 1
                                Object p_sec = ap_localStack[i_SPreg - 2];
                                ap_localStack[i_SPreg++] = p_sec;
                                ap_localStack[i_SPreg++] = p_top;
                            }
                        }
                        break;
                    // DUP2_x1 (A1 B1 C1 -> B1 C1 A1 B1 C1 | A2 B2 -> B2 A2 B2)
                    case 93:
                        {
                            Object p_top = ap_localStack[i_SPreg - 1];

                            if (p_top instanceof Long)
                            {
                                // form 2
                                ap_localStack[i_SPreg++] = p_top; // a b -> a b b
                                ap_localStack[i_SPreg - 2] = ap_localStack[i_SPreg - 3]; // a b b -> a a b
                                ap_localStack[i_SPreg - 3] = p_top; // a a b -> b a b
                            }
                            else
                            {
                                // form 1
                                Object p_sec = ap_localStack[i_SPreg - 2];
                                i_SPreg += 2;
                                int i_newindex = i_SPreg - 1;
                                int i_oldindex = i_newindex - 2;
                                for (int li = 0; li < 3; li++)
                                {
                                    ap_localStack[i_newindex] = ap_localStack[i_oldindex];
                                    i_newindex--;
                                    i_oldindex--;
                                }
                                ap_localStack[i_oldindex] = p_top;
                                ap_localStack[--i_oldindex] = p_sec;
                            }
                        }
                        break;
                    // DUP2_X2 ( A1 B1 C1 D1 -> C1 D1 A1 B1 C1 D1 | A1 B1 C2 -> C2 A1 B1 C2 | A2 B1 C1 -> B1 C1 A2 B1 C1 | A2 B2 -> B2 A2 B2
                    case 94:
                        {
                            Object p_top = ap_localStack[i_SPreg - 1];
                            Object p_second = ap_localStack[i_SPreg - 2];
                            if (p_top instanceof Long)
                            {
                                if (p_second instanceof Long)
                                {
                                    // form 4 (a b -> b a b)
                                    ap_localStack[i_SPreg++] = p_top;
                                    ap_localStack[i_SPreg - 3] = p_top;
                                    ap_localStack[i_SPreg - 2] = p_second;
                                }
                                else
                                {
                                    // form 2
                                    ap_localStack[i_SPreg++] = p_top; // a b c -> a b c c
                                    ap_localStack[i_SPreg - 2] = ap_localStack[i_SPreg - 3]; // a b c c -> a b b c
                                    ap_localStack[i_SPreg - 3] = ap_localStack[i_SPreg - 4]; // a b b c -> a a b c
                                    ap_localStack[i_SPreg - 4] = p_top; // a a b c -> c a b c
                                }
                            }
                            else
                            {
                                Object p_third = ap_localStack[i_SPreg - 3];
                                if (p_third instanceof Long)
                                {
                                    // form 3
                                    i_SPreg += 2;
                                    int i_newindex = i_SPreg - 1;
                                    int i_oldindex = i_newindex - 2;
                                    for (int li = 0; li < 3; li++)
                                    {
                                        ap_localStack[i_newindex] = ap_localStack[i_oldindex];
                                        i_newindex--;
                                        i_oldindex--;
                                    }
                                    ap_localStack[i_oldindex] = p_top;
                                    ap_localStack[--i_oldindex] = p_second;
                                }
                                else
                                {
                                    //form 1
                                    i_SPreg += 2;
                                    int i_newindex = i_SPreg - 1;
                                    int i_oldindex = i_newindex - 2;
                                    for (int li = 0; li < 4; li++)
                                    {
                                        ap_localStack[i_newindex] = ap_localStack[i_oldindex];
                                        i_newindex--;
                                        i_oldindex--;
                                    }
                                    ap_localStack[i_oldindex] = p_top;
                                    ap_localStack[--i_oldindex] = p_second;
                                }
                            }
                        }
                        break;
                    // SWAP
                    case 95:
                        {
                            Object p_top = ap_localStack[i_SPreg - 1];
                            ap_localStack[i_SPreg - 1] = ap_localStack[i_SPreg - 2];
                            ap_localStack[i_SPreg - 2] = p_top;
                        }
                        break;
                    // IADD
                    case 96:
                        {
                            int i_first = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_second = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_first + i_second);
                        }
                        break;
                    // LADD
                    case 97:
                        {
                            long l_first = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_second = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_first + l_second);
                        }
                        break;
                    // FADD
                    case 98:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DADD
                    case 99:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // ISUB (a b -> c) c = a - b
                    case 100:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a - i_b);
                        }
                        break;
                    // LSUB (a b -> c) c = a - b
                    case 101:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a - l_b);
                        }
                        break;
                    // FSUB
                    case 102:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DSUB
                    case 103:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // IMUL (a b -> c) c = a * b
                    case 104:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a * i_b);
                        }
                        break;
                    // LMUL (a b -> c) c = a * b
                    case 105:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a * l_b);
                        }
                        break;
                    // FNUL
                    case 106:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DMUL
                    case 107:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // IDIV (a b -> c) c = a / b
                    case 108:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a / i_b);
                        }
                        break;
                    // LDIV (a b -> c) c = a / b
                    case 109:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a / l_b);
                        }
                        break;
                    // FDIV
                    case 110:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DDIV
                    case 111:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // IREM (a b -> c) c = a % b
                    case 112:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a % i_b);
                        }
                        break;
                    // LREM (a b -> c) c = a % b
                    case 113:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a % l_b);
                        }
                        break;
                    // FREM
                    case 114:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DREM
                    case 115:
                    {
                        throw new IllegalArgumentException("Double");
                    }

                    // INEG (a -> b) b = -a
                    case 116:
                        {
                            int i_val = 0 - ((Integer) ap_localStack[i_SPreg - 1]).intValue();
                            ap_localStack[i_SPreg - 1] = new Integer(i_val);
                        }
                        break;
                    // LNEG (a -> b) b = -a
                    case 117:
                        {
                            long l_val = 0 - ((Long) ap_localStack[i_SPreg - 1]).longValue();
                            ap_localStack[i_SPreg - 1] = new Long(l_val);
                        }
                        break;
                    // FNEG
                    case 118:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DNEG
                    case 119:
                    {
                        throw new IllegalArgumentException("Double");
                    }

                    // ISHL (a b -> c) c = a << b
                    case 120:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a << i_b);
                        }
                        break;
                    // LSHL (a b -> c) c = a<<b
                    case 121:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a << i_b);
                        }
                        break;

                    // ISHR (a b -> c) c = a >> b
                    case 122:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a >> i_b);
                        }
                        break;
                    // LSHR (a b -> c) c = a>>b
                    case 123:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a >> i_b);
                        }
                        break;

                    // IUSHR (a b -> c) c = a >>> b
                    case 124:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a >>> i_b);
                        }
                        break;
                    // LUSHR (a b -> c) c = a>>>b
                    case 125:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a >>> i_b);
                        }
                        break;

                    // IAND (a b -> c) c = a & b
                    case 126:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a & i_b);
                        }
                        break;
                    // LAND (a b -> c) c = a & b
                    case 127:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a & l_b);
                        }
                        break;

                    // IOR (a b -> c) c = a | b
                    case 128:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a | i_b);
                        }
                        break;
                    // LOR (a b -> c) c = a | b
                    case 129:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a | l_b);
                        }
                        break;


                    // IXOR (a b -> c) c = a ^ b
                    case 130:
                        {
                            int i_b = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_a = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            ap_localStack[i_SPreg++] = new Integer(i_a ^ i_b);
                        }
                        break;
                    // LXOR (a b -> c) c = a ^ b
                    case 131:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();
                            ap_localStack[i_SPreg++] = new Long(l_a ^ l_b);
                        }
                        break;

                    // IINC
                    case 132:
                        {
                            int i_index = ab_bytecodes[i_PCreg++] & 0xFF;

                            if (lg_wideFlag)
                            {
                                i_index = (i_index << 8) | (ab_bytecodes[i_PCreg++] & 0xFF);
                            }


                            int i_const = ab_bytecodes[i_PCreg++];

                            if (lg_wideFlag)
                            {
                                i_const = (i_const << 8) | (ab_bytecodes[i_PCreg++] & 0xFF);
                                lg_wideFlag = false;
                            }

                            int i_val = ((Integer) ap_localVariables[i_index]).intValue() + i_const;
                            ap_localVariables[i_index] = new Integer(i_val);
                        }
                        break;

                    // I2L
                    case 133:
                        {
                            ap_localStack[i_SPreg - 1] = new Long(((Integer) ap_localStack[i_SPreg - 1]).intValue());
                        }
                        break;
                    // I2F
                    case 134:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // I2D
                    case 135:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // L2I
                    case 136:
                        {
                            ap_localStack[i_SPreg - 1] = new Integer((int) ((Long) ap_localStack[i_SPreg - 1]).longValue());
                        }
                        break;
                    // L2F
                    case 137:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // L2D
                    case 138:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // F2I
                    case 139:
                    // F2L
                    case 140:
                    // F2D
                    case 141:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // D2I
                    case 142:
                    // D2L
                    case 143:
                    // D2F
                    case 144:
                    {
                        throw new IllegalArgumentException("Double");
                    }
                    // I2B
                    case 145:
                        {
                            ap_localStack[i_SPreg - 1] = new Integer(((Integer) ap_localStack[i_SPreg - 1]).byteValue());
                        }
                        break;
                    // I2Ñ
                    case 146:
                        {
                            ap_localStack[i_SPreg - 1] = new Integer(((Integer) ap_localStack[i_SPreg - 1]).intValue() & 0xFFFF);
                        }
                        break;
                    // I2S
                    case 147:
                        {
                            ap_localStack[i_SPreg - 1] = new Integer(((Integer) ap_localStack[i_SPreg - 1]).shortValue());
                        }
                        break;
                    // LCMP (a b -> c)  a > b ? c = 1 , a = b ? c = 0, a < b? c = -1;
                    case 148:
                        {
                            long l_b = ((Long) ap_localStack[--i_SPreg]).longValue();
                            long l_a = ((Long) ap_localStack[--i_SPreg]).longValue();

                            int i_val = l_a > l_b ? 1 : l_a == l_b ? 0 : -1;
                            ap_localStack[i_SPreg++] = new Integer(i_val);
                        }
                        break;
                    // FCMPL, FCMPG
                    case 149:
                    case 150:
                    {
                        throw new IllegalArgumentException("Float");
                    }
                    // DCMPL, DCMPG
                    case 151:
                    case 152:
                    {
                        throw new IllegalArgumentException("Double");
                    }

                    // IFEQ,IFNE,IFLT,IFGE,IFLE
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                        {
                            int i_branchbyte1 = ab_bytecodes[i_PCreg++] & 0xFF;
                            int i_branchbyte2 = ab_bytecodes[i_PCreg++] & 0xFF;

                            int i_jumpoffset = (short) ((i_branchbyte1 << 8) | i_branchbyte2);

                            int i_val = ((Integer) ap_localStack[--i_SPreg]).intValue();

                            switch (i_bytecode)
                            {
                                // IFEQ
                                case 153:
                                    if (i_val == 0)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                                // IFNE
                                case 154:
                                    if (i_val != 0)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                                // IFLT
                                case 155:
                                    if (i_val < 0)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                                // IFGE
                                case 156:
                                    if (i_val >= 0)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                                // IFGT
                                case 157:
                                    if (i_val > 0)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                                // IFLE
                                case 158:
                                    if (i_val <= 0)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                            }
                        }
                        break;
                    // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                        {
                            int i_branchOffset = _readShortFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 2;

                            int i_val2 = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_val1 = ((Integer) ap_localStack[--i_SPreg]).intValue();

                            switch (i_bytecode)
                            {
                                // IF_ICMPEQ
                                case 159:
                                    if (i_val1 == i_val2)
                                    {
                                        i_PCreg = i_prevPC + i_branchOffset;
                                    }
                                    break;
                                // IF_CMPNE
                                case 160:
                                    if (i_val1 != i_val2)
                                    {
                                        i_PCreg = i_prevPC + i_branchOffset;
                                    }
                                    break;
                                // IF_ICMPLT
                                case 161:
                                    if (i_val1 < i_val2)
                                    {
                                        i_PCreg = i_prevPC + i_branchOffset;
                                    }
                                    break;
                                // IF_ICMPGE
                                case 162:
                                    if (i_val1 >= i_val2)
                                    {
                                        i_PCreg = i_prevPC + i_branchOffset;
                                    }
                                    break;
                                // IF_ICMPGT
                                case 163:
                                    if (i_val1 > i_val2)
                                    {
                                        i_PCreg = i_prevPC + i_branchOffset;
                                    }
                                    break;
                                // IF_ICMPLE
                                case 164:
                                    if (i_val1 <= i_val2)
                                    {
                                        i_PCreg = i_prevPC + i_branchOffset;
                                    }
                                    break;
                            }
                        }
                        break;
                    // IF_ACMPEQ, IF_ACMPNE
                    case 165:
                    case 166:
                        {
                            int i_branchbyte1 = ab_bytecodes[i_PCreg++] & 0xFF;
                            int i_branchbyte2 = ab_bytecodes[i_PCreg++] & 0xFF;

                            int i_jumpoffset = (short) ((i_branchbyte1 << 8) | i_branchbyte2);

                            Object p_val2 = ap_localStack[--i_SPreg];
                            Object p_val1 = ap_localStack[--i_SPreg];

                            switch (i_bytecode)
                            {
                                // IF_ACMPEQ
                                case 165:
                                    if (p_val1 == p_val2)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                                // IF_ACMPNE
                                case 160:
                                    if (p_val1 != p_val2)
                                    {
                                        i_PCreg = i_prevPC + i_jumpoffset;
                                    }
                                    break;
                            }
                        }
                        break;
                    // GOTO
                    case 167:
                        {
                            int i_branchbyte1 = ab_bytecodes[i_PCreg++] & 0xFF;
                            int i_branchbyte2 = ab_bytecodes[i_PCreg++] & 0xFF;

                            int i_jumpoffset = (short) ((i_branchbyte1 << 8) | i_branchbyte2);

                            i_PCreg = i_prevPC + i_jumpoffset;
                        }
                        break;

                    // JSR
                    case 168:
                        {
                            int i_branchbyte1 = ab_bytecodes[i_PCreg++] & 0xFF;
                            int i_branchbyte2 = ab_bytecodes[i_PCreg++] & 0xFF;

                            ap_localStack[i_SPreg++] = new Integer(i_PCreg);

                            int i_jumpoffset = (short) ((i_branchbyte1 << 8) | i_branchbyte2);

                            i_PCreg = i_prevPC + i_jumpoffset;
                        }
                        break;
                    // RET
                    case 169:
                        {
                            int i_index = ab_bytecodes[i_PCreg++] & 0xFF;

                            if (lg_wideFlag)
                            {
                                i_index = (i_index << 8) | (ab_bytecodes[i_PCreg++] & 0xFF);
                                lg_wideFlag = false;
                            }

                            i_PCreg = ((Integer) ap_localVariables[i_index]).intValue();
                        }
                        break;
                    // TABLESWITCH
                    case 170:
                        {
                            // pad
                            i_PCreg += (i_PCreg % 4);

                            int i_default = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 4;
                            int i_low = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 4;
                            int i_high = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 4;

                            int i_value = ((Integer) ap_localStack[--i_SPreg]).intValue();

                            int i_offset = i_default;

                            if (i_value >= i_low && i_value <= i_high)
                            {
                                i_offset = _readIntegerFromBytecode(ab_bytecodes, i_PCreg + ((i_value - i_low) << 2));
                            }

                            i_PCreg = i_prevPC + i_offset;
                        }
                        break;
                    // LOOKUPSWITCH
                    case 171:
                        {
                            // pad
                            i_PCreg += (i_PCreg % 4);

                            int i_default = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 4;
                            int i_pairsnumber = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 4;

                            int i_value = ((Integer) ap_localStack[--i_SPreg]).intValue();

                            int i_offset = i_default;

                            for (int li = 0; li < i_pairsnumber; li++)
                            {
                                int i_index = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);

                                if (i_value == i_index)
                                {
                                    i_PCreg += 4;
                                    i_offset = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                                    break;
                                }
                                else
                                {
                                    i_PCreg += 8;
                                }
                            }

                            i_PCreg = i_prevPC + i_offset;
                        }
                        break;
                    // IRETURN,LRETURN,FRETURN,DRETURN,ARETURN
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    {
                        return ap_localStack[--i_SPreg];
                    }
                    // RETURN
                    case 177:
                    {
                        return null;
                    }

                    // GETSTATIC, PUTSTATIC
                    case 178:
                    case 179:
                        {
                            int i_poolindex = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;
                            int i_data = ((Integer) ap_ConstantPoolData[i_poolindex]).intValue();

                            int i_classref = i_data >>> 16;
                            Integer p_nametype = (Integer) ap_ConstantPoolData[i_data & 0xFFFF];

                            Object p_value = null;

                            if (i_ClassName == i_classref)
                            {
                                // the same class
                                ClassField p_classField = (ClassField) p_FieldsTable.get(p_nametype);

                                if (i_bytecode == 178)
                                {
                                    // GET
                                    p_value = p_classField.getStaticValue();
                                    ap_localStack[i_SPreg++] = p_value;
                                }
                                else
                                {
                                    // PUT
                                    p_value = ap_localStack[--i_SPreg];
                                    p_classField.setStaticValue(p_value);
                                }
                            }
                            else
                            {
                                String s_identifier = _makeStringRepresentationForField(i_classref, p_nametype);

                                if (i_bytecode == 178)
                                {
                                    // GET
                                    p_value = p_Processor.getStatic(this, s_identifier);
                                    ap_localStack[i_SPreg++] = p_value;
                                }
                                else
                                {
                                    // PUT
                                    p_value = ap_localStack[--i_SPreg];
                                    p_Processor.setStatic(this, s_identifier, p_value);
                                }
                            }
                        }
                        break;

                    // GETFIELD, PUTFIELD
                    case 180:
                    case 181:
                        {
                            int i_poolindex = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;

                            int i_data = ((Integer) ap_ConstantPoolData[i_poolindex]).intValue();

                            int i_classref = i_data >>> 16;
                            Integer p_nametype = (Integer) ap_ConstantPoolData[i_data & 0xFFFF];

                            if (i_bytecode == 180)
                            {
                                // GET
                                Object p_object = ap_localStack[--i_SPreg];

                                if (p_object instanceof MJVMObject)
                                {
                                    MJVMObject p_mjvmobj = (MJVMObject) p_object;
                                    ap_localStack[i_SPreg++] = p_mjvmobj.getFieldForUID(p_nametype);
                                }
                                else
                                {
                                    String s_identifier = _makeStringRepresentationForField(i_classref, p_nametype);
                                    ap_localStack[i_SPreg++] = p_Processor.getField(this, p_object, s_identifier);
                                }
                            }
                            else
                            {
                                // PUT
                                Object p_value = ap_localStack[--i_SPreg];
                                Object p_object = ap_localStack[--i_SPreg];

                                if (p_object instanceof MJVMObject)
                                {
                                    MJVMObject p_mjvmobj = (MJVMObject) p_object;
                                    p_mjvmobj.setFieldForUID(p_nametype, p_value);
                                }
                                else
                                {
                                    String s_identifier = _makeStringRepresentationForField(i_classref, p_nametype);
                                    p_Processor.setField(this, p_object, s_identifier, p_value);
                                }
                            }

                        }
                        break;
                    //INVOKEVIRTUAL, INVOKENONVIRTUAL, INVOKESTATIC, INVOKEINTERFACE
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                        {
                            int i_poolindex = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;

                            int i_data = ((Integer) ap_ConstantPoolData[i_poolindex]).intValue();

                            int i_classref = i_data >>> 16;
                            Integer p_nametype = (Integer) ap_ConstantPoolData[i_data & 0xFFFF];
                            String s_type = (String) ap_ConstantPoolData[p_nametype.intValue() & 0xFFFF];

                            int i_args = _getArgsNumber(s_type);

                            Object p_result = null;

                            Object[] ap_args = new Object[i_args];
                            while (i_args > 0)
                            {
                                i_args--;
                                ap_args[i_args] = ap_localStack[--i_SPreg];
                            }

                            Object p_object = null;

                            switch (i_bytecode)
                            {
                                case 182:  // INVOKEVIRTUAL
                                case 183:  // INVOKENONVIRTUAL
                                case 185:  // INVOKEINTERFACE
                                    {
                                        p_object = ap_localStack[--i_SPreg];

                                        if (i_bytecode == 185)
                                        {
                                            i_PCreg += 2;
                                        }
                                    }
                                    break;
                                case 184: // INVOKESTATIC
                                    {
                                    }
                                    break;
                            }

                            String s_id = _makeStringRepresentationForField(i_classref, p_nametype);

                            if (p_object != null)
                            {
                                if (i_classref == i_ClassName && p_object instanceof MJVMObject)
                                {
                                    MJVMObject p_mjvmobj = (MJVMObject) p_object;

                                    int i_nametype = p_nametype.intValue();
                                    MJVMClass p_class = p_mjvmobj.p_Class;
                                    ClassMethod p_method = p_class.findMethodForName((String) ap_ConstantPoolData[i_nametype >>> 16], (String) ap_ConstantPoolData[i_nametype & 0xFFFF]);

                                    if (p_method == null)
                                    {
                                        throw new IllegalArgumentException("Unknown method");
                                    }

                                    p_result = p_mjvmobj.p_Class.invoke(p_method, p_mjvmobj, ap_args);
                                }
                                else
                                {
                                    p_result = p_Processor.invoke(this, p_object, s_id, ap_args);
                                }
                            }
                            else
                            {
                                if (i_ClassName == i_classref)
                                {
                                    int i_nametype = p_nametype.intValue();
                                    String s_method = (String) ap_ConstantPoolData[i_nametype >>> 16];
                                    String s_signature = (String) ap_ConstantPoolData[i_nametype & 0xFFFF];

                                    ClassMethod p_method = findMethodForName(s_method, s_signature);
                                    if (p_method == null)
                                    {
                                        throw new Error("Unsupported method [" + s_method + ' ' + s_signature + ']');
                                    }

                                    p_result = invoke(p_method, null, ap_args);
                                }
                                else
                                {
                                    p_result = p_Processor.invoke(this, null, s_id, ap_args);
                                }
                            }


                            if (s_type.charAt(s_type.length() - 1) != ClassMethod.TYPE_VOID)
                            {
                                ap_localStack[i_SPreg++] = p_result;
                            }
                        }
                        break;
                    // NEW
                    case 187:
                        {
                            int i_classref = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;

                            String s_className = (String) ap_ConstantPoolData[((Integer) ap_ConstantPoolData[i_classref]).intValue()];

                            Object p_newInstance = null;

                            try
                            {
                                // trying to make a class object
                                Class p_class = Class.forName(s_className.replace('/', '.'));
                                p_newInstance = p_class.newInstance();
                            }
                            catch (Throwable _thr)
                            {
                                // ask a processor for the new instance
                                p_newInstance = p_Processor.newInstance(this, s_className);
                            }

                            ap_localStack[i_SPreg++] = p_newInstance;
                        }
                        break;
                    // NEWARRAY
                    case 188:
                        {
                            int i_count = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            int i_atype = ab_bytecodes[i_PCreg++] & 0xFF;

                            Object p_result = null;

                            switch (i_atype)
                            {
                                case 4: // boolean
                                    {
                                        p_result = new boolean[i_count];
                                    }
                                    break;
                                case 5: // char
                                    {
                                        p_result = new char[i_count];
                                    }
                                    break;
                                case 8: // byte
                                    {
                                        p_result = new byte[i_count];
                                    }
                                    break;
                                case 9: // short
                                    {
                                        p_result = new short[i_count];
                                    }
                                    break;
                                case 10: // int
                                    {
                                        p_result = new int[i_count];
                                    }
                                    break;
                                case 11: // long
                                    {
                                        p_result = new long[i_count];
                                    }
                                    break;
                                case 6: // float
                                case 7: // double
                                default:
                                {
                                    throw new IllegalArgumentException("Unsupported array type");
                                }
                            }

                            ap_localStack[i_SPreg++] = p_result;
                        }
                        break;
                    // ANEWARAY
                    case 189:
                        {
                            int i_index = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;

                            int i_count = ((Integer) ap_localStack[--i_SPreg]).intValue();

                            String s_className = (String) ap_ConstantPoolData[((Integer) ap_ConstantPoolData[i_index]).intValue()];

                            Object[] ap_array = p_Processor.newObjectArray(this, s_className, i_count);

                            ap_localStack[i_SPreg++] = ap_array;
                        }
                        break;
                    // ARRAYLENGTH
                    case 190:
                        {
                            Object p_array = ap_localStack[--i_SPreg];
                            int i_length = ((Object[]) p_array).length;
                            ap_localStack[i_SPreg++] = new Integer(i_length);
                        }
                        break;
                    // ATWHROW
                    case 191:
                    {
                        Throwable p_throwable = (Throwable) ap_localStack[--i_SPreg];
                        if (p_throwable == null)
                        {
                            p_throwable = new NullPointerException();
                        }
                        throw p_throwable;
                    }
                    // CHECKCAST, INSTANCEOF
                    case 192:
                    case 193:
                        {
                            int i_index = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;

                            String s_classReference = (String) ap_ConstantPoolData[((Integer) ap_ConstantPoolData[i_index]).intValue()];
                            s_classReference = s_classReference.replace('/', '.');

                            Class p_class = Class.forName(s_classReference);

                            Object p_object = ap_localStack[i_SPreg - 1];

                            if (!p_class.isInstance(p_object))
                            {
                                if (i_bytecode == 192)
                                {
                                    throw new ClassCastException("Class cast exception " + p_object);
                                }
                                else
                                {
                                    // instanceof
                                    ap_localStack[i_SPreg - 1] = new Integer(0);
                                }
                            }
                            else
                            {
                                if (i_bytecode == 192)
                                {
                                    ap_localStack[i_SPreg - 1] = new Integer(1);
                                }
                            }
                        }
                        break;
                    // MONITORENTER
                    case 194:
                        {
                            //TODO in future
                            Object p_synchObject = ap_localStack[--i_SPreg];
                            if (p_synchObject == null)
                            {
                                throw new NullPointerException("Monitor is null");
                            }
                            if (!(p_synchObject instanceof MJVMObject))
                            {
                                throw new IllegalArgumentException("You must use only a MJVMObject as a monitor");
                            }

                            MJVMObject p_mjvmobject = (MJVMObject) p_synchObject;
                            p_mjvmobject.lock(Thread.currentThread());
                        }
                        break;
                    // MONITOREXIT
                    case 195:
                        {
                            // TODO in future
                            Object p_synchObject = ap_localStack[--i_SPreg];
                            if (p_synchObject == null)
                            {
                                throw new NullPointerException("Monitor is null");
                            }
                            if (!(p_synchObject instanceof MJVMObject))
                            {
                                throw new IllegalArgumentException("You must use only a MJVMObject as a monitor");
                            }

                            MJVMObject p_mjvmobject = (MJVMObject) p_synchObject;
                            p_mjvmobject.unlock(Thread.currentThread());
                        }
                        break;
                    // WIDE
                    case 196:
                        {
                            lg_wideFlag = true;
                        }
                        break;
                    // MULTIANEWARRAY
                    case 197:
                        {
                            int i_index = _readShortFromBytecode(ab_bytecodes, i_PCreg) & 0xFFFF;
                            i_PCreg += 2;
                            int i_dimensions = ab_bytecodes[i_PCreg++] & 0xFF;

                            int[] ai_dimensions = new int[i_dimensions];

                            while (i_dimensions > 0)
                            {
                                i_dimensions--;

                                ai_dimensions[i_dimensions] = ((Integer) ap_localStack[--i_SPreg]).intValue();
                            }

                            String s_class = (String) ap_ConstantPoolData[(((Integer) ap_ConstantPoolData[i_index]).intValue())];

                            Object p_array = p_Processor.newMultidimensionObjectArray(this, s_class, ai_dimensions);

                            ap_localStack[i_SPreg++] = p_array;
                        }
                        break;
                    // IFNULL, IFNONNULL
                    case 198:
                    case 199:
                        {
                            Object p_obj = ap_localStack[--i_SPreg];

                            boolean lg_result = i_bytecode == 198 ? p_obj == null : p_obj != null;

                            if (lg_result)
                            {
                                int i_offset = _readShortFromBytecode(ab_bytecodes, i_PCreg);
                                i_PCreg = i_prevPC + i_offset;
                            }
                            else
                            {
                                i_PCreg += 2;
                            }
                        }
                        break;
                    // GOTO_2
                    case 200:
                        {
                            int i_offset = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg = i_prevPC + i_offset;
                        }
                        break;
                    // JSR_W
                    case 201:
                        {
                            int i_offset = _readIntegerFromBytecode(ab_bytecodes, i_PCreg);
                            i_PCreg += 4;

                            ap_localStack[i_SPreg++] = new Integer(i_PCreg);

                            i_PCreg = i_prevPC + i_offset;
                        }
                        break;
                    // UNKNOWN
                    default:
                    {
                        throw new Error("Unsupported byte code [" + i_bytecode + ']');
                    }
                }
            }
            catch (Throwable _thr)
            {
                // processing of try..catch blocks
                MethodExceptionRecord[] ap_exceptionRecords = _method.ap_MethodExceptionRecords;

                MethodExceptionRecord p_record = null;

                for (int li = 0; li < ap_exceptionRecords.length; li++)
                {
                    MethodExceptionRecord p_exceptionCatch = ap_exceptionRecords[li];
                    if (p_exceptionCatch.checkAddress(i_prevPC))
                    {
                        final String s_normalizedExceptionClassName = p_exceptionCatch.s_NormalizedExceptionClassName;
                        if (s_normalizedExceptionClassName == null)
                        {
                            // it process any exception, may be it is finally
                            p_record = p_exceptionCatch;
                            break;
                        }

                        // check for the exception class
                        Class p_class = Class.forName(s_normalizedExceptionClassName);
                        if (p_class.isInstance(_thr))
                        {
                            p_record = p_exceptionCatch;
                            break;
                        }
                    }
                }

                if (p_record != null)
                {
                    // set the exception processor address to the PC
                    i_PCreg = p_record.getExceptionProcessorAddress();
                    // place the exception object on the stack
                    ap_localStack[i_SPreg++] = _thr;
                }
                else
                {
                    // we have not found a try..catch for the exception so we throw it 
                    throw _thr;
                }
            }
        }
    }

    /**
     * Parse a method signature to get the arguments number for the method
     * @param _type a method signature, must not be null, as an example "(II[[L)V"
     * @return the argument number as integer
     */
    private final int _getArgsNumber(String _signature)
    {
        int i_len = _signature.length();

        boolean lg_object = false;

        int i_args = 0;

        for (int li = 0; li < i_len; li++)
        {
            char ch_char = _signature.charAt(li);
            switch (ch_char)
            {
                case '(':
                    continue;
                case ')':
                    return i_args;
                case '[':
                    continue;
                case 'L':
                    i_args++;
                    lg_object = true;
                    break;
                case ';':
                    lg_object = false;
                    break;
                default:
                {
                    if (!lg_object)
                    {
                        i_args++;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Wrong format of a type");
    }

    /**
     * Generate a class-name-type identifier from such item in a constant pool
     * @param _classref the class reference index in the constant pool
     * @param _nametype an Integer object contains the name-type item index in the constant pool
     * @return an unique generated representation as String, as an example "com/igormaznitsa/class.testField.I"
     */
    private final String _makeStringRepresentationForField(int _classref, Integer _nametype)
    {
        int i_int = _nametype.intValue();
        return ((String) ap_ConstantPoolData[((Integer) ap_ConstantPoolData[_classref]).intValue()]) + '.' + ((String) ap_ConstantPoolData[i_int >>> 16]) + '.' + ((String) ap_ConstantPoolData[i_int & 0xFFFF]);
    }

    /**
     * Read an integer from a byte array in an address
     * @param _bytecode a byte array, must not be null
     * @param _address an address where the integer value begins
     * @return the integer as an int value
     */
    private static final int _readIntegerFromBytecode(byte[] _bytecode, int _address)
    {
        int i_b0 = _bytecode[_address++] & 0xFF;
        int i_b1 = _bytecode[_address++] & 0xFF;
        int i_b2 = _bytecode[_address++] & 0xFF;
        int i_b3 = _bytecode[_address] & 0xFF;

        return (i_b0 << 24) | (i_b1 << 16) | (i_b2 << 8) | i_b3;
    }

    /**
     * Read a short from a byte array in an address
     * @param _bytecode a byte array, must not be null
     * @param _address an address where the short value begins
     * @return the short value as an integer
     */
    private static final int _readShortFromBytecode(byte[] _bytecode, int _address)
    {
        int i_b0 = _bytecode[_address++] & 0xFF;
        int i_b1 = _bytecode[_address] & 0xFF;

        return (short) ((i_b0 << 8) | i_b1);
    }

    /**
     * Create "new instance" of the loaded class with the default constructor
     * @return the new instance as a MJVMObject
     * @exception Throwable it will be thrown if the object can not be initialized
     */
    public MJVMObject newInstance() throws Throwable
    {
        ClassMethod p_constructor = findMethodForName("<init>", "()V");

        if (p_constructor == null)
        {
            throw new IllegalAccessException("Can't find the default constructor");
        }

        // create new MJVMObject
        MJVMObject p_result = new MJVMObject(this);
        _initFieldsByDefaultValues(p_result);

        // invoke the default constructor
        invoke(p_constructor, p_result, null);

        return p_result;
    }

    /**
     * Create "new instance" of the loaded class with a constructor
     * @param _signature thesignature of an invoked constructor
     * @param _arguments an array contains arguments (they must be the same ones as in the signature)
     * @return the new instance as a MJVMObject
     * @exception Throwable it will be thrown if the object can not be initialized
     */
    public MJVMObject newInstance(String _signature, Object[] _arguments) throws Throwable
    {
        ClassMethod p_constructor = findMethodForName("<init>", _signature);

        if (p_constructor == null)
        {
            throw new IllegalAccessException("Can't find the constructor [" + getSimpleClassName() + ' ' + _signature + ']');
        }

        // create new MJVMObject
        MJVMObject p_result = new MJVMObject(this);
        _initFieldsByDefaultValues(p_result);

        // invoke the default constructor
        invoke(p_constructor, p_result, _arguments);

        return p_result;
    }

    /**
     * Init all fields by their default values, the default value for every field depends on its type
     * @param _object a MJVMObject to be inited, must not be null
     */
    private final void _initFieldsByDefaultValues(MJVMObject _object)
    {
        Enumeration p_enum = p_FieldsTable.elements();
        while (p_enum.hasMoreElements())
        {
            ClassField p_field = (ClassField) p_enum.nextElement();

            String s_type = p_field.s_Type;
            Object p_value = null;

            if (s_type.length() > 1)
            {
                // it is an object type
                // it has been inited by null already
            }
            else
            {
                // it is a primitive type
                switch (s_type.charAt(0))
                {
                    case ClassMethod.TYPE_INT:
                        p_value = new Integer(0);
                        break;
                    case ClassMethod.TYPE_LONG:
                        p_value = new Long(0);
                        break;
                    case ClassMethod.TYPE_SHORT:
                        p_value = new Integer(0);
                        break;
                    case ClassMethod.TYPE_CHAR:
                        p_value = new Integer(0);
                        break;
                    case ClassMethod.TYPE_DOUBLE:
                        throw new IllegalArgumentException("Double");
                    case ClassMethod.TYPE_FLOAT:
                        throw new IllegalArgumentException("Float");
                    case ClassMethod.TYPE_BOOLEAN:
                        p_value = new Boolean(false);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported field type");
                }
            }

            if ((p_field.i_Flags & ClassField.FIELD_FLAG_STATIC) == 0)
            {
                _object.setFieldForIndex(p_field.i_FieldIndex, p_value);
            }
        }
    }
}
