package com.inet.testapplication;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Test Program
 */
public class TestLauncher {

    /**
     * Startpunkt vom Installer
     * 
     * @param args ignored
     * @throws Exception if any error occur on connection the server
     */
    public static void main( String[] args ) throws Exception {
        try {
            // Set OS L&F if some error message will be displayed 
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch( Throwable e ) {
            // ignore
        }

        final JFrame frame = new JFrame( "Test Application" );
        frame.setUndecorated( true );
        frame.setAlwaysOnTop( true );
        frame.setVisible( true );
        frame.setLocationRelativeTo( null );
        JOptionPane.showMessageDialog( frame, "The Application seems to work, right?", "Test Application", JOptionPane.OK_OPTION );
        frame.dispose();
        System.exit(0);
    }
}
