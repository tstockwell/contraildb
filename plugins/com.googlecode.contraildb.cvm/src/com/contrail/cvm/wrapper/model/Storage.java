package com.contrail.cvm.wrapper.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class Storage implements TreeModel
{

    protected String s_Name;
    protected ArrayList<TreeModelListener> p_TreeModelListeners;
    protected ArrayList<StorageItem> p_Items;
    public static final Comparator<StorageItem> ALPHA_COMPARATOR = new Comparator<StorageItem>()
    {

        @Override
        public int compare(StorageItem o1, StorageItem o2)
        {
            // packages on top
            if (o1 instanceof PackageItem && o2 instanceof ClassItem)
            {
                return -1;
            }
            if (o2 instanceof PackageItem && o1 instanceof ClassItem)
            {
                return 1;
            }

            return o1.getName().compareTo(o2.getName());
        }
    };

    public Storage(String _name)
    {
        s_Name = _name;
        p_TreeModelListeners = new ArrayList<TreeModelListener>();
        p_Items = new ArrayList<StorageItem>();
    }

    public void addArchive(File _file) throws IOException
    {
        FileInputStream p_inStream = null;

        try
        {
            p_inStream = new FileInputStream(_file);
            ZipInputStream p_zipInputStream = new ZipInputStream(p_inStream);

            final byte[] ab_bufferArray = new byte[1024];

            while (true)
            {
                ZipEntry p_entry = p_zipInputStream.getNextEntry();
                if (p_entry == null)
                {
                    break;
                }

                if (p_entry.isDirectory())
                {
                    continue;
                }

                if (!p_entry.getName().endsWith(".class"))
                {
                    continue;
                }

                ByteArrayOutputStream p_baosEntry = new ByteArrayOutputStream(16384);
                byte[] ab_classArray = null;
                if (p_entry.getSize() < 0)
                {
                    while (true)
                    {
                        int i_readLen = p_zipInputStream.read(ab_bufferArray, 0, ab_bufferArray.length);
                        if (i_readLen < 0)
                        {
                            break;
                        }
                        p_baosEntry.write(ab_bufferArray, 0, i_readLen);
                    }
                    p_zipInputStream.closeEntry();
                    p_baosEntry.close();
                    ab_classArray = p_baosEntry.toByteArray();
                    p_baosEntry = null;
                }
                else
                {
                    while (true)
                    {
                        int i_readLen = p_zipInputStream.read(ab_bufferArray, 0, ab_bufferArray.length);
                        if (i_readLen < 0)
                        {
                            break;
                        }
                        p_baosEntry.write(ab_bufferArray, 0, i_readLen);
                    }
                    p_zipInputStream.closeEntry();
                    p_baosEntry.close();
                    ab_classArray = p_baosEntry.toByteArray();
                    p_baosEntry = null;
                }

                addClass(new ByteArrayInputStream(ab_classArray), false);
            }
        }
        finally
        {
            if (p_inStream != null)
            {
                try
                {
                    p_inStream.close();
                }
                catch (Throwable _thr)
                {
                }
            }

            for (TreeModelListener p_listener : p_TreeModelListeners)
            {
                p_listener.treeStructureChanged(new TreeModelEvent(this, new TreePath(this)));
            }
        }
    }

    public ClassItem[] getAllClassItems()
    {
        Vector<ClassItem> p_result = new Vector<ClassItem>();

        for (StorageItem p_item : p_Items)
        {
            if (p_item instanceof PackageItem)
            {
                ClassItem[] ap_classes = ((PackageItem) p_item).getClassItems();
                for (int li = 0; li < ap_classes.length; li++)
                {
                    p_result.add(ap_classes[li]);
                }
            }
            else
            {
                if (p_item instanceof ClassItem)
                {
                    p_result.add((ClassItem) p_item);
                }
            }
        }

        return p_result.toArray(new ClassItem[p_result.size()]);
    }

    public PackageItem getPackageForName(String _name)
    {
        for (StorageItem p_item : p_Items)
        {
            if (p_item instanceof PackageItem)
            {
                if (p_item.getName().equals(_name))
                {
                    return (PackageItem) p_item;
                }
            }
        }
        return null;
    }

    public void removeAll()
    {
        p_Items.clear();
        for (TreeModelListener p_listener : p_TreeModelListeners)
        {
            p_listener.treeStructureChanged(new TreeModelEvent(this, new TreePath(this)));
        }
    }

    protected PackageItem addPackage(String _name)
    {
        PackageItem p_newPackage = new PackageItem(_name);
        p_Items.add(p_newPackage);
        return p_newPackage;
    }

    public void addClass(InputStream _classStream, boolean _notify) throws IOException
    {
        JavaClass p_class = new ClassParser(_classStream, null).parse();

        String s_package = p_class.getPackageName();
        if (s_package==null || s_package.length()==0) s_package = "<default>";


        PackageItem p_packageItem = getPackageForName(s_package);

        if (p_packageItem == null)
        {
            p_packageItem = addPackage(s_package);
        }

        ClassItem p_item = new ClassItem(p_packageItem, p_class);
        p_packageItem.addClass(p_item);

        if (_notify)
        {
            for (TreeModelListener p_listener : p_TreeModelListeners)
            {
                p_listener.treeStructureChanged(new TreeModelEvent(this, new TreePath(this)));
            }
        }
    }

    public void removeItem(StorageItem _item)
    {
        if (_item instanceof ClassItem)
        {
            PackageItem p_package = ((ClassItem) _item).getPackage();
            if (p_package != null)
            {
                p_package.removeClass((ClassItem) _item);
                if (p_package.getChildCount(p_package) == 0)
                {
                    p_Items.remove(p_package);
                }
            }
        }
        else
        {
            if (_item instanceof PackageItem)
            {
                p_Items.remove(_item);
            }
        }

        for (TreeModelListener p_listener : p_TreeModelListeners)
        {
            p_listener.treeStructureChanged(new TreeModelEvent(this, new TreePath(this)));
        }
    }

    public void addClass(File _class) throws IOException
    {
        FileInputStream p_fis = null;
        try
        {
            p_fis = new FileInputStream(_class);
            addClass(p_fis, true);
        }
        finally
        {
            if (p_fis != null)
            {
                try
                {
                    p_fis.close();
                }
                catch (Throwable _thr)
                {
                }
            }
        }
    }

    @Override
    public Object getRoot()
    {
        return this;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if (this == parent)
        {
            return p_Items.get(index);
        }
        else
        {
            if (parent instanceof PackageItem)
            {
                return ((PackageItem) parent).getChild(parent, index);
            }
            else
            {
                return null;
            }
        }
    }

    @Override
    public int getChildCount(Object parent)
    {
        if (this == parent)
        {
            return p_Items.size();
        }
        else
        {
            if (parent instanceof PackageItem)
            {
                return ((PackageItem) parent).getChildCount(parent);
            }
            else
            {
                return 0;
            }
        }
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
        if (parent == this)
        {
            return p_Items.indexOf(child);
        }
        else
        {
            if (parent instanceof PackageItem)
            {
                return ((PackageItem) parent).getIndexOfChild(parent, child);
            }
            else
            {
                return -1;
            }
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
        if (!p_TreeModelListeners.contains(l))
        {
            p_TreeModelListeners.add(l);
        }
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        p_TreeModelListeners.remove(l);
    }

    @Override
    public String toString()
    {
        return s_Name;
    }
}
