// The Grinder
// Copyright (C) 2001 Paco Gomez
// Copyright (C) 2001 Philip Aston

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
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.Test;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;
import net.grinder.util.PropertiesHelper;

/**
 * This is the entry point of The Grinder Console.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class Console implements ActionListener
{       
    private GrinderProperties m_properties = null;
    private Map m_tests = new TreeMap();
    private GraphStatInfo m_gsi[] = null;
    private StatInfo m_si[] = null;
    //ms between console refreshes
    private int _interval = 500;     
                 
    public static void main(String args[]) throws Exception {
	System.err.println("net.grinder.console.Console is deprecated");
	System.err.println("Use net.grinder.Console instead.");

        Console c = new Console();
        c.run();
    }

    public Console() throws GrinderException
    {
	m_properties = GrinderProperties.getProperties();
	final PropertiesHelper propertiesHelper =
	    new PropertiesHelper(m_properties);

	final GrinderPlugin grinderPlugin =
	    propertiesHelper.getPlugin();

	// Shove the tests into a TreeMap so that they're ordered.
	final Iterator testSetIterator =
	    propertiesHelper.getTestSet().iterator();

	while (testSetIterator.hasNext())
	{
	    final Test test = (Test)testSetIterator.next();
	    final Integer testNumber = test.getTestNumber();
	    m_tests.put(test.getTestNumber(), test);
	}
    }
    
    public void run() throws GrinderException
    {
        String s = new java.util.Date().toString() + ": ";
        System.out.println(s + "Grinder Console started.");        
        createFrame();        
        MsgReader mr =
	    new MsgReader(m_si,
			  m_properties.getMandatoryProperty(
			      "grinder.console.multicastAddress"),
			  m_properties.getMandatoryInt(
			      "grinder.console.multicastPort"));
	
        while(true){
            try{
                Thread.sleep(_interval);
                
                for (int i=0; i<m_si.length; i++){
                    m_gsi[i].add(m_si[i]._art);
                    m_gsi[i].update(m_si[i]);
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
        
	int n = m_tests.size();
	    
        m_gsi = new GraphStatInfo[n];
        m_si = new StatInfo[n];

	final Iterator testIterator = m_tests.values().iterator();
	int i = 0;
	
	while (testIterator.hasNext()) {
	    final Test test = (Test)testIterator.next();

            m_gsi[i] = new GraphStatInfo(test.toString(), 0, 0);
            p.add(m_gsi[i]);
            m_si[i] = new StatInfo(0,0);
	    i++;
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
