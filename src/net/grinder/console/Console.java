// The Grinder
// Copyright (C) 2000  Paco Gomez

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.console;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.net.*;
import java.io.*;
import net.grinder.engine.*;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;

/**
 * This is the entry point of The Grinder Console.
 * 
 * @author Paco Gomez
 * @version $Revision$
 */
public class Console implements ActionListener
{       
    private GrinderProperties m_properties = null;
    private GraphStatInfo _gsi[] = null;
    private StatInfo _si[] = null;
    //ms between console refreshes
    private int _interval = 500;     
                 
    public static void main(String args[]) throws Exception {
	System.err.println("net.grinder.console.Console is deprecated");
	System.err.println("Use net.grinder.Console instead.");

        Console c = new Console();
        c.run();
    }
    
    public void run() throws GrinderException {
	m_properties = GrinderProperties.getProperties();

        String s = new java.util.Date().toString() + ": ";
        System.out.println(s + "Grinder Console started.");        
        createFrame();        
        MsgReader mr =
	    new MsgReader(_si,
			  m_properties.getMandatoryProperty(
			      "grinder.console.multicastAddress"),
			  m_properties.getMandatoryInt(
			      "grinder.console.multicastPort"));
	
        while(true){
            try{
                Thread.sleep(_interval);
                
                for (int i=0; i<_si.length; i++){
                    _gsi[i].add(_si[i]._art);
                    _gsi[i].update(_si[i]);
                }
            }
            catch(Exception e){
                System.err.println(e);
            }           
        }
    }
    
    protected void createFrame(){
        JFrame frame = new JFrame("Grinder Console");

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
       
        JPanel p = new JPanel();
        JButton b = new JButton("StartGrinder");
        b.addActionListener(this);
        
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(b);
        
	    StringTokenizer st;
	    
	    st = new StringTokenizer(m_properties.getProperty("grinder.plugin.methods"), "\t\n\r,");
	    int n = st.countTokens();
	    
	    //be aware of the index!!!
        _gsi = new GraphStatInfo[n];
        _si = new StatInfo[n];
        for (int i=0; i<n; i++){
            _gsi[i] = new GraphStatInfo((String)st.nextElement(), 0, 0);
            p.add(_gsi[i]);
            _si[i] = new StatInfo(0,0);
        }
        
        JScrollPane sp = new JScrollPane(p);
        frame.getContentPane().add(sp, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);        
                
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("StartGrinder")) {
            sendStartPacket();
        }
    }
    
    public void sendStartPacket(){
        
        System.out.println("Starting Grinder...");
        byte[] outbuf = new byte[128];
        try{
            DatagramSocket socket = new DatagramSocket();
            InetAddress groupAddr = InetAddress.getByName(
                m_properties.getProperty("grinder.multicastAddress"));
            DatagramPacket packet = new DatagramPacket(outbuf, outbuf.length, groupAddr, 
                m_properties.getMandatoryInt("grinder.multicastPort"));

            socket.send(packet);
            System.out.println("Grinder started at " + new Date());
        }
        catch(SocketException e){
            e.printStackTrace(System.err);
        }
        catch(IOException e){
            e.printStackTrace(System.err);
        }
        catch(Exception e){
            e.printStackTrace(System.err);
        }
        
    }
}
