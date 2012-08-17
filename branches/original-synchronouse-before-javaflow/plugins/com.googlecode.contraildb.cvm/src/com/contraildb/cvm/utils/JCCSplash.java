package com.contraildb.cvm.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.*;

/**
 * This class shows a splash on the screen center 
 * @version 1.00
 */
public class JCCSplash extends JDialog implements Runnable
{
    /**
     * This variable contains delay in milleseconds to show the splash on the screen
     */
    protected volatile long l_Delay;
    
    /**
     * The constructor 
     * @param _imagePath the path to the image which will be used as the splash
     * @param _delay the delay to show the window in milliseconds (must be > 0)
     */
    public JCCSplash(String _imagePath,long _delay)
    {
        super();
        
        l_Delay = _delay;
        
        if (_delay<=0) throw new IllegalArgumentException("Delay is less than zero or equ");
        
        setModal(true);
        setUndecorated(true);
        setResizable(false);
        
        JPanel p_panel = new JPanel(new BorderLayout());
        
        URL p_imageURL = ClassLoader.getSystemResource(_imagePath);
        if (p_imageURL==null) throw new IllegalArgumentException("Can't load splash image "+_imagePath);
        ImageIcon p_BackgroundImage = new ImageIcon(p_imageURL);
        p_panel.add(new JLabel(p_BackgroundImage),BorderLayout.CENTER);
        setContentPane(p_panel);
        
        Dimension p_scrDim = Toolkit.getDefaultToolkit().getScreenSize();
        pack();

        int i_x = (p_scrDim.width-getWidth())/2;
        int i_y = (p_scrDim.height-getHeight())/2;
        
        setLocation(i_x,i_y);
        setAlwaysOnTop(true);
        
        new Thread(this).start();
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                setVisible(true);
            }
        }
       ).start();
    }
    
    @Override
    public void run()
    {
        while(!isVisible() && !Thread.currentThread().isInterrupted())
        {
            Thread.yield();
        }
        
        if (Thread.currentThread().isInterrupted()) 
        {
            dispose();
            return;
        }
        
        try
        {
            Thread.sleep(l_Delay);
        }
        catch(Throwable _thr)
        {}
        finally
        {
            this.dispose();
        }
    }
}
