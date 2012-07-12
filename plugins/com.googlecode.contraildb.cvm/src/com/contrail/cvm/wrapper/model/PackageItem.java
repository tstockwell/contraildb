package com.contrail.cvm.wrapper.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class PackageItem implements StorageItem, TreeModel
{
    protected String s_Name;
    protected ArrayList<StorageItem> p_StorageItems;
    protected boolean lg_Enabled;

    public PackageItem(String _name)
    {
        s_Name = _name;
        lg_Enabled = true;
        p_StorageItems = new ArrayList<StorageItem>();
    }

    @Override
    public Object getRoot()
    {
        return this;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if (parent == this)
        {
            return p_StorageItems.get(index);
        }
        else
        {
            return null;
        }
    }

    @Override
    public int getChildCount(Object parent)
    {
        if (this == parent)
        {
            return p_StorageItems.size();
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node)
    {
        return node instanceof ClassItem;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        if (this == parent)
        {
            return p_StorageItems.indexOf(child);
        }
        else
            return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
       
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {

    }

    @Override
    public String toString()
    {
        return s_Name;
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
    public String getName()
    {
        return s_Name;
    }

    public ClassItem[] getClassItems()
    {
        Vector <ClassItem> p_result = new Vector<ClassItem>();
        for(StorageItem p_item : p_StorageItems)
        {
            if (p_item instanceof ClassItem) p_result.add((ClassItem)p_item);
        }
        return p_result.toArray(new ClassItem[p_result.size()]);
    }

    protected void addClass(ClassItem _item) throws IOException
    {
        if (p_StorageItems.contains(_item)) throw new IOException("Class already presented ["+_item.getJavaClass().getClassName()+']');
        p_StorageItems.add(_item);
        Collections.sort(p_StorageItems,Storage.ALPHA_COMPARATOR);
    }

    protected void removeClass(ClassItem classItem)
    {
        p_StorageItems.remove(classItem);
    }
    
}
