package com.googlecode.contraildb.cvm.wrapper;

import com.contrail.cvm.wrapper.model.ClassItem;
import com.contrail.cvm.wrapper.model.PackageItem;
import com.contrail.cvm.wrapper.model.Storage;
import com.contraildb.cvm.utils.Utils;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;

public class TRender extends JLabel implements TreeCellRenderer
{
    protected ImageIcon p_Icon_Storage;
    protected ImageIcon p_Icon_PackageItem;
    protected ImageIcon p_Icon_ClassItem;

    protected Color p_SelBackground;
    protected Color p_SelForeground;
    protected Color p_Foreground;
    protected Color p_Background;
    
    public TRender()
    {
        super();
        p_Icon_Storage = new ImageIcon(Utils.loadImage("images/icon_storage.gif"));
        p_Icon_PackageItem = new ImageIcon(Utils.loadImage("images/icon_packageitem.gif"));
        p_Icon_ClassItem = new ImageIcon(Utils.loadImage("images/icon_classitem.gif"));
    
        p_SelForeground = UIManager.getColor("Tree.selectionForeground");
        p_SelBackground = UIManager.getColor("Tree.selectionBackground");
        p_Foreground = UIManager.getColor("Tree.textForeground");
        p_Background = UIManager.getColor("Tree.textBackground");
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree _tree, Object _value, boolean _selected, boolean _expanded, boolean _leaf, int _row, boolean _hasFocus)
    {
        String s_str = _value.toString()+"  ";
        
        if (_selected)
        {
            setOpaque(true);
            setForeground(p_SelForeground);
            setBackground(p_SelBackground);
        }
        else
        {
            setOpaque(false);
            setForeground(p_Foreground);
            setBackground(p_Background);
        }
        
        if (_value instanceof Storage)
        {
            setIcon(p_Icon_Storage);
            setText(s_str);
        }
        else
        if (_value instanceof PackageItem)
        {
            setIcon(p_Icon_PackageItem);
            setText(s_str);
        }
        else
        if (_value instanceof ClassItem)
        {
            setIcon(p_Icon_ClassItem);
            setText(s_str);
        }
        else
        {
            setIcon(null);
            setText(s_str);
        }
        return this;
    }
}
