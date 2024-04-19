package edu.emory.cellbio.svg;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Parameter dialog box for SVG embed and crop images tool
 * @author Benjamin Nanes
 */
public class OutputParamDialog extends JDialog implements ActionListener {
     
     // -- Fields --
     
     private JLabel imgFileLabel;
     private JComboBox imgFileMode;
     private JLabel compQualLabel;
     private JSpinner compQual;
     private JLabel resampLabel;
     private JLabel resampLabel2;
     private JCheckBox resampCheck;
     private JSpinner resampLevel;
     private JSpinner resampLevel2;
     private JButton ok;
     private JButton cancel;
     
     private boolean wasOKd = false;
     
     // -- Constructors --
     
     public OutputParamDialog() {
          setup();
     }
     
     // -- ActionListener implementation --

     @Override
     public synchronized void actionPerformed(ActionEvent e) {
          if(e.getSource() == imgFileMode) {
               compQual.setEnabled(imgFileMode.getSelectedIndex() == 1);
               compQualLabel.setEnabled(imgFileMode.getSelectedIndex() == 1);
          }
          if(e.getSource() == ok) {
               System.err.println("ok");
               wasOKd = true;
               setVisible(false);
               notifyAll();
          }
          if(e.getSource() == cancel) {
               System.err.println("cancel");
               setVisible(false);
               notifyAll();
          }
     }
     
     // -- Methods --
     
     /** Show the dialog and wait for a response before returning */
     public synchronized void showAndWait() {
          setVisible(true);
          while(isVisible()) {
               try{ wait(); }
               catch(InterruptedException e){ }
          }
     }
     
     /** Returns {@code true} if the OK button was pressed */
     public boolean wasOKd() {
          return wasOKd;
     }
     
     /** Get the selected image file type */
     public String getImgFileMode() {
          switch(imgFileMode.getSelectedIndex()) {
               case 0:
                    return "png";
               case 1:
                    return "jpeg";
          }
          throw new IllegalArgumentException("Invalid image type selecetd");
     }
     
     /** Get the selected image compression quality parameter */
     public float getCompressionQuality() {
          return ((Double)compQual.getModel().getValue()).floatValue();
     }
     
     /** Get the selected resampling option */
     public boolean getDoResample() {
         return resampCheck.getModel().isSelected();
     }
     
     /** Get the selected resampling level */
     public float getTargetRes() {
         return ((Double)resampLevel.getModel().getValue()).floatValue();
     }
     public float getMaxRes() {
         return ((Double)resampLevel2.getModel().getValue()).floatValue();
     }
     
     // -- Helper methods --
     
     private void setup() {
          setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
          addWindowListener(new WindowAdapter() {
               public void windowClosing(WindowEvent we) {
                    synchronized(we.getWindow()) {
                         setVisible(false);
                         we.getWindow().notifyAll();
                    }
               }
          });
          setLocationRelativeTo(null);
          
          setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
          imgFileLabel = new JLabel("Image encoding type:");
          imgFileMode = new JComboBox(new String[] {"PNG", "JPEG"});
          imgFileMode.setAlignmentX(Component.LEFT_ALIGNMENT);
          imgFileMode.addActionListener(this);
          compQualLabel = new JLabel("Compression level\n(high, better quality; low, smaller file):");
          compQualLabel.setEnabled(imgFileMode.getSelectedIndex() == 1);
          compQual = new JSpinner(new SpinnerNumberModel(0.8f, 0.0f, 1.0f, 0.1f));
          compQual.setAlignmentX(Component.LEFT_ALIGNMENT);
          compQual.setEnabled(imgFileMode.getSelectedIndex() == 1);
          resampLabel = new JLabel("Downsample to (px/mm):");
          resampLabel2 = new JLabel("For images greater than (px/mm):");
          resampCheck = new JCheckBox("Do resampling?");
          resampLevel = new JSpinner(new SpinnerNumberModel(11.811f, 0.0f, 1000.0f, 0.1f));
          resampLevel.setAlignmentX(Component.LEFT_ALIGNMENT);
          resampLevel2 = new JSpinner(new SpinnerNumberModel(15.748f, 0.0f, 1000.0f, 0.1f));
          resampLevel2.setAlignmentX(Component.LEFT_ALIGNMENT);
          JPanel buttons = new JPanel();
          buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
          buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
          ok = new JButton("OK");
          ok.addActionListener(this);
          ok.setDefaultCapable(true);
          cancel = new JButton("Cancel");
          cancel.addActionListener(this);
          
          buttons.add(Box.createHorizontalStrut(10));
          buttons.add(ok);
          buttons.add(Box.createHorizontalStrut(10));
          buttons.add(cancel);
          
          add(Box.createVerticalStrut(5));
          add(imgFileLabel);
          add(imgFileMode);
          add(Box.createVerticalStrut(10));
          add(compQualLabel);
          add(compQual);
          add(Box.createVerticalStrut(10));
          add(resampCheck);
          add(resampLabel);
          add(resampLevel);
          add(resampLabel2);
          add(resampLevel2);
          add(Box.createVerticalStrut(10));
          add(buttons);
          add(Box.createVerticalStrut(5));
          pack();
     }

}
