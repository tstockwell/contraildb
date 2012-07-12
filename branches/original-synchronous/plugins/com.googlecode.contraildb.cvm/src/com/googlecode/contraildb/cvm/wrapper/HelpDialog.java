package com.googlecode.contraildb.cvm.wrapper;

import com.contraildb.cvm.utils.Utils;

public class HelpDialog extends javax.swing.JDialog
{

    /** Creates new form HelpDialog */
    public HelpDialog(java.awt.Frame parent)
    {
        super(parent, true);
        initComponents();

        Utils.toScreenCenter(this);

        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        p_Button_Close = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Shortly\n-----------------\n   This utility allows to generate a java class source that implements the MJVMProcessor interface. The generated class can be used to process calls from a MJVMClass object. The generated class is not fully completed but it really helps to process classes which have a lot of class members.\n\nHow it works?\n-----------------\n  It is very easy. You need to place all classes, which you want to use, at the tree panel. The tree contains all classes which will be used to generate the processor class source, of course only their non-private members will be used for the process (also I do not recommend to use the utility for inside anonymous classes, it is no so smart to dig very hard cases).\n  You can add either detached classes or full packages (JAR or ZIP) into the tree with the '+' button. The '-' button allows to remove selected items from the tree and the 'x' button allows to clear whole tree.\n  When you have completed your class list, you can generate the java source from it with the menu 'File->Generate stub'.");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextArea1);

        p_Button_Close.setText("Close");
        p_Button_Close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                p_Button_CloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                    .addComponent(p_Button_Close))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(p_Button_Close)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void p_Button_CloseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_p_Button_CloseActionPerformed
    {//GEN-HEADEREND:event_p_Button_CloseActionPerformed
        dispose();
    }//GEN-LAST:event_p_Button_CloseActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JButton p_Button_Close;
    // End of variables declaration//GEN-END:variables
}