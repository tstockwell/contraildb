package com.contrail.cvm.wrapper.model;

import org.apache.bcel.classfile.JavaClass;

public class ClassItem implements StorageItem
{
    protected boolean lg_Enabled;
    protected String s_Name;

    protected JavaClass p_Class;
    protected String s_InsideClassID;

    protected PackageItem p_Owner;

    public ClassItem(PackageItem _package,JavaClass _class)
    {
        s_Name = _class.getClassName();
        p_Class = _class;
        p_Owner = _package;

        int i_index = s_Name.lastIndexOf('.');
        if (i_index>=0)
        {
            s_Name = s_Name.substring(i_index+1);
        }

        lg_Enabled = true;

        s_InsideClassID = _class.getClassName();
    }

    public PackageItem getPackage()
    {
        return p_Owner;
    }

    public JavaClass getJavaClass()
    {
        return p_Class;
    }

    @Override
    public boolean isEnabled()
    {
        return lg_Enabled;
    }

    @Override
    public void setEnabled(boolean _flag)
    {
        lg_Enabled = _flag;
    }

    @Override
    public boolean equals(Object _obj)
    {
        if (_obj == null || !(_obj instanceof ClassItem) ) return false;
        return s_InsideClassID.equals(((ClassItem)_obj).s_InsideClassID);
    }

    @Override
    public int hashCode()
    {
        return s_InsideClassID.hashCode();
    }

    @Override
    public String toString()
    {
        return s_Name;
    }

    @Override
    public String getName()
    {
        return s_Name;
    }
}
