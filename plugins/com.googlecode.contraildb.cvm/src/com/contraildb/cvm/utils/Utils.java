package com.contraildb.cvm.utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

public class Utils
{
    public static final byte [] loadResourceFromJAR(Class _class,String _resource) throws IOException
    {
        InputStream p_inStream = null;
        try
        {
            p_inStream = _class.getResourceAsStream(_resource);
            ByteArrayOutputStream p_outStream = new ByteArrayOutputStream(16384);
            
            final int BUFFERSIZE = 1024;
            byte [] ab_buffer = new byte[BUFFERSIZE];

            while(true)
            {
                int i_read = p_inStream.read(ab_buffer);
                if (i_read<0) break;
                p_outStream.write(ab_buffer,0,i_read);
            }
            
            return p_outStream.toByteArray();
        }
        finally
        {
            if (p_inStream!=null)
            {
                try
                {
                    p_inStream.close();
                }
                catch(Throwable _thr)
                {}
            }
        }
    }
    
    public static byte [] loadFromURL(URL _url) throws IOException
    {
        HttpURLConnection p_connection = (HttpURLConnection) _url.openConnection();
        p_connection.setRequestMethod("GET");
        p_connection.setDoOutput(false);
        p_connection.setDoInput(true);

        p_connection.connect();

        if (p_connection.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            InputStream p_inStream = null;
            ByteArrayOutputStream p_fos = null;
            try
            {
                p_inStream = p_connection.getInputStream();
                p_fos = new ByteArrayOutputStream(16384);

                byte[] ab_buffer = new byte[16384];
                while (true)
                {
                    int i_read = p_inStream.read(ab_buffer, 0, ab_buffer.length);
                    if (i_read <= 0)
                    {
                        break;
                    }
                    p_fos.write(ab_buffer, 0, i_read);
                }
                p_fos.flush();

                return p_fos.toByteArray();
            }
            catch (Throwable _thr)
            {
                throw (IOException) (new IOException(_thr.getClass().getCanonicalName()).initCause(_thr));
            }
            finally
            {
                try
                {
                    p_connection.disconnect();
                }
                catch (Throwable _thr)
                {
                }

                if (p_fos != null)
                {
                    try
                    {
                        p_fos.close();
                    }
                    catch (Throwable _thr)
                    {
                    }
                }
            }
        }
        else
            return null;
    }
 
    
    public static final void saveFile(File _file,byte [] _data) throws IOException
    {
        FileOutputStream p_out = null;
        try
        {
            p_out = new FileOutputStream(_file);
            p_out.write(_data);
            p_out.flush();
        }
        finally
        {
            if (p_out != null)
            {
                try
                {
                    p_out.close();
                }
                catch(Throwable _thr){}
            }
        }
    }
    
    public static final byte [] loadFile(File _file) throws IOException
    {
        FileInputStream p_inStream = null;
        try
        {
            p_inStream = new FileInputStream(_file);
            int i_length = (int)_file.length();
            byte [] ab_result = new byte[i_length];
            int i_pos = 0;
            while(i_length>0)
            {
                int i_read = p_inStream.read(ab_result, i_pos, i_length);
                if (i_read<0) break;
                i_pos += i_read;
                i_length -= i_read;
            }
            if (i_length>0) throw new IOException("Can't read full file");
            return ab_result;
        }
        finally
        {
            if (p_inStream!=null)
            {
                try
                {
                    p_inStream.close();
                }
                catch(Throwable _thr)
                {}
            }
        }
    }
    
    public static final void changeMenuItemsState(JMenu _menu, boolean _enable)
    {
        int i_itemsNumber = _menu.getItemCount();
        for(int li=0;li<i_itemsNumber;li++)
        {
            JMenuItem p_item = _menu.getItem(li);
            p_item.setEnabled(_enable);
        }
    }

    public static final void toScreenCenter(Window _component)
    {
        Dimension p_dim = Toolkit.getDefaultToolkit().getScreenSize();
        _component.setLocation((p_dim.width-_component.getWidth())/2, (p_dim.height-_component.getHeight())/2);
    }
    
    public static final Image loadImage(String _path)
    {
        try
        {
            return ImageIO.read(Utils.class.getClassLoader().getResourceAsStream(_path));
        }
        catch(Throwable _thr)
        {
            _thr.printStackTrace();
            return null;
        }
    }
    
    public static File selectFileForOpen(Component _parent, FileFilter [] _fileFilters, String _title, FileFilter [] _selectedFilter, File _initFile)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDragEnabled(false);
        chooser.setControlButtonsAreShown(true);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        for(int li=0;li<_fileFilters.length;li++) chooser.addChoosableFileFilter(_fileFilters[li]);
        
        chooser.setDialogTitle(_title);
        chooser.setAcceptAllFileFilterUsed(false);

        if (_initFile != null)
        {
            chooser.setCurrentDirectory(_initFile);
            chooser.setName(_initFile.getName());
        }

        int returnVal = chooser.showDialog(_parent, "Open");
        _selectedFilter[0]=chooser.getFileFilter();
        
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File p_file = chooser.getSelectedFile();
            return p_file;
        }
        else
        {
            return null;
        }


    }

    public static File selectFileForOpen(Component _parent, FileFilter _fileFilter, String _title, File _initFile)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDragEnabled(false);
        chooser.setControlButtonsAreShown(true);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(_fileFilter);
        chooser.setDialogTitle(_title);
        chooser.setAcceptAllFileFilterUsed(false);

        if (_initFile != null)
        {
            chooser.setCurrentDirectory(_initFile);
            chooser.setName(_initFile.getName());
        }

        int returnVal = chooser.showDialog(_parent, "Open");

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File p_file = chooser.getSelectedFile();
            return p_file;
        }
        else
        {
            return null;
        }
    }

    public static File selectFileForSave(Component _parent, FileFilter _fileFilter, String _title, File _initFile)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDragEnabled(false);
        chooser.setControlButtonsAreShown(true);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(_fileFilter);
        chooser.setDialogTitle(_title);
        chooser.setAcceptAllFileFilterUsed(false);

        if (_initFile != null)
        {
            chooser.setSelectedFile(_initFile);
        }

        int returnVal = chooser.showDialog(_parent, "Save");

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File p_file = chooser.getSelectedFile();
            return p_file;
        }
        else
        {
            return null;
        }
    }

    public static File selectDirectoryForSave(Component _parent, String _title, File _initFile)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDragEnabled(false);
        chooser.setControlButtonsAreShown(true);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(_title);
        chooser.setAcceptAllFileFilterUsed(false);

        if (_initFile != null)
        {
            chooser.setCurrentDirectory(_initFile);
            chooser.setName(_initFile.getName());
        }

        int returnVal = chooser.showDialog(_parent, "Save");

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File p_file = chooser.getSelectedFile();
            return p_file;
        }
        else
        {
            return null;
        }
    }

    public static final byte[] Str2UTF8(String _in)
    {
        if (_in == null) return null;
        ByteArrayOutputStream p_buffer = new ByteArrayOutputStream(_in.length());
        
        for(int li=0;li<_in.length();li++)
        {
            int i_char = _in.charAt(li);
            
            if (i_char<0x7F)
            {
                p_buffer.write(i_char);
            }
            else
            if (i_char<0x7FF)
            {
                p_buffer.write(0xC0 | (i_char >>> 6));
                p_buffer.write(0x80 | (i_char & 0x3F));
            }
            else
            if (i_char<0x10000)
            {
                p_buffer.write(0xE0 | (i_char >>> 12));
                p_buffer.write(0x80 | ((i_char >>> 6) & 0x3F));
                p_buffer.write(0x80 | (i_char & 0x3F));
            }
            else
            if (i_char<0x200000)
            {
                p_buffer.write(0xC0 | (i_char >>> 18));
                p_buffer.write(0x80 | ((i_char >>> 12) & 0x3F));
                p_buffer.write(0x80 | ((i_char >>> 6) & 0x3F));
                p_buffer.write(0x80 | (i_char & 0x3F));
            }
            else
                throw new Error ("Unsupported symbol");
        }
        
        return p_buffer.toByteArray();
    }
    
    public static BufferedImage loadImageFromStream(InputStream _inStream) throws IOException
    {
        int i_flag = _inStream.read();
        if (i_flag>0)
        {
            int i_len = _inStream.read();
            i_len |= (_inStream.read()<<8);
            i_len |= (_inStream.read()<<16);
            i_len |= (_inStream.read()<<24);
            
            if (i_len < 0 ) throw new IOException("Wrong image length");
            
            byte [] ab_bytes = new byte[i_len];
            int i_pos = 0;
            while(i_len>0)
            {
                int i_read = _inStream.read(ab_bytes,i_pos,i_len);
                if (i_read<0) throw new IOException("Can't read full image array");
                i_pos += i_read;
                i_len -= i_read;
            }
            
            return ImageIO.read(new ByteArrayInputStream(ab_bytes));
        }
        else
        if (i_flag<0) throw new IOException("End of stream");
            
        return null;
    }
    
    public static void saveImageIntoStream(BufferedImage _image,OutputStream _out) throws IOException
    {
        if (_image == null)
        {
            _out.write(0);
        }
        else
        {
            _out.write(1);
            ByteArrayOutputStream p_baos = new ByteArrayOutputStream(6000);
            ImageIO.write(_image, "png", p_baos);
            byte [] ab_array = p_baos.toByteArray();
            p_baos = null;
            int i_len = ab_array.length;
            
            _out.write(i_len);
            i_len>>>=8;
            _out.write(i_len);
            i_len>>>=8;
            _out.write(i_len);
            i_len>>>=8;
            _out.write(i_len);

            _out.write(ab_array);
            
            _out.flush();
        }
    }
}
