package com.googlecode.contraildb.cvm.wrapper;

import com.contraildb.cvm.utils.JCCSplash;

import javax.swing.SwingUtilities;

public class main
{
    public static final String APPLICATION = "MJVM Stub Generator";
    public static final String VERSION = "1.00";
    public static final String AUTHOR = "Igor A. Maznitsa";

    public static final void main(String ... _args)
    {
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run()
            {
                new JCCSplash("images/splash.jpg", 3000);
                new MainForm();
            }
        });
    }
}
