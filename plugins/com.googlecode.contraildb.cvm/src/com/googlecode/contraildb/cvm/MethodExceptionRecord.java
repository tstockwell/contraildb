package com.googlecode.contraildb.cvm;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The class describes a exception try..catch block record for a method bytecode
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 * @version 1.00
 */
public class MethodExceptionRecord
{

    /**
     * The start addres of the bytecode area for the exception processor
     */
    protected int i_StartPCValue;
    /**
     * The end addres of the bytecode area for the exception processor
     */
    protected int i_EndPCValue;
    /**
     * The start address of a bytecode which will be called if the exception is thrown
     */
    protected int i_ExceprionProcessorAddress;
    /**
     * The standard SUN class name representation of the exception, i.e. "java/lang/Exception"
     */
    protected String s_ExceptionClass;
    /**
     * The normalized class name representation of the exception, i.e. "java.lang.Excption"
     */
    protected String s_NormalizedExceptionClassName;

    /**
     * Get the start address of the bytecode area for the try..catch
     * @return the address as integer
     */
    public int getStartPC()
    {
        return i_StartPCValue;
    }

    /**
     * Get the end address of the bytecode area for the try..catch
     * @return the address as integer
     */
    public int getEndPC()
    {
        return i_EndPCValue;
    }

    /**
     * Get the start address of the bytecode area contains the code processing the exception
     * @return the address as integer
     */
    public int getExceptionProcessorAddress()
    {
        return i_ExceprionProcessorAddress;
    }

    /**
     * Get the normalized class name of the exception
     * @return the normalized name as String
     */
    public String getNormalizedExceptionClassName()
    {
        return s_NormalizedExceptionClassName;
    }

    /**
     * Get the exception class name
     * @return the exception class name as String
     */
    public String getExceptionClassName()
    {
        return s_ExceptionClass;
    }

    /**
     * Check an address to be placed in the area for the try..catch block
     * @param _address an address as integer
     * @return true if the address in the try..catch else false
     */
    public final boolean checkAddress(int _address)
    {
        return _address >= i_StartPCValue && _address <= i_EndPCValue;
    }

    /**
     * The constructor
     * @param _constantPool the constant pool of the class, must not be null
     * @param _stream the DataInputStream contains the data of the excepion
     * @throws java.io.IOException the exception will be thrown if there is any error in a transport operation
     */
    public MethodExceptionRecord(Object[] _constantPool, DataInputStream _stream) throws IOException
    {
        i_StartPCValue = _stream.readUnsignedShort();
        i_EndPCValue = _stream.readUnsignedShort();
        i_ExceprionProcessorAddress = _stream.readUnsignedShort();
        int i_index = _stream.readUnsignedShort();
        if (i_index == 0)
        {
            s_ExceptionClass = null;
            s_NormalizedExceptionClassName = null;
        }
        else
        {
            String s_exceptionClass = ((String) _constantPool[((Integer) _constantPool[i_index]).intValue()]);
            s_ExceptionClass = s_exceptionClass.replace('/', '.');
            s_NormalizedExceptionClassName = s_ExceptionClass.replace('/', '.');
        }
    }
}