package com.googlecode.contraildb.cvm.wrapper;

import com.contraildb.cvm.utils.Utils;

public class AboutDialog extends javax.swing.JDialog
{

    /** Creates new form AboutDialog */
    public AboutDialog(java.awt.Frame parent)
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

        p_Button_Close = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("About");
        setResizable(false);

        p_Button_Close.setText("Close");
        p_Button_Close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                p_Button_CloseActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("MJVM Stub Generator utility\nVersion: 1.00\n--------------------------------------------------------------\nAuthor: Igor A. Maznitsa\n\nThe utility is absolute free and can be used without any resstrictions.\nNew version of the utility can be found on http://www.igormaznitsa.com");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(p_Button_Close, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(p_Button_Close))
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