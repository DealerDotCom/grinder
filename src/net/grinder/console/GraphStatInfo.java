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

/**
 * This class is used graphically show statistics.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
public class GraphStatInfo extends javax.swing.JPanel{
    
    public GraphStatInfo(String msg, float min, float max){
        _eg = new EvolGraph(_x, _y, min, max);
               
        _title = new JLabel(msg + " (ART ms)");
        _min = new JLabel("" + min);
        _max = new JLabel("" + max);
        _cur = new JLabel("" + min);
        _time = new JLabel("processing time: " + 0 + " s");
        _trans = new JLabel("transactions: " + 0);
        _art = new JLabel("ART: " + 0 + " s");
        
        Border blackline;
        blackline = BorderFactory.createLineBorder(Color.black);
        
        _eg.setBorder(blackline);        
        
        _title.setForeground(Color.black);
        _min.setForeground(Color.black);
        _max.setForeground(Color.black);
        _cur.setForeground(Color.black);
        _time.setForeground(Color.black);
        _trans.setForeground(Color.black);
        _art.setForeground(Color.black);
        
        Font f = new Font("helvetica", Font.PLAIN, 10);
        _title.setFont(f);
        _min.setFont(f);
        _max.setFont(f);
        _cur.setFont(f);
        _time.setFont(f);
        _trans.setFont(f);
        _art.setFont(f);
        _title.setVerticalAlignment(SwingConstants.TOP);
        _min.setVerticalAlignment(SwingConstants.TOP);
        _max.setVerticalAlignment(SwingConstants.BOTTOM);
        _cur.setVerticalAlignment(SwingConstants.TOP);
        _time.setVerticalAlignment(SwingConstants.TOP);
        _trans.setVerticalAlignment(SwingConstants.TOP);
        _art.setVerticalAlignment(SwingConstants.BOTTOM);
        
        
        setLayout(null);

        add(_title);
        add(_eg);
        add(_min);
        add(_cur);
        add(_max);
        add(_time);
        add(_trans);
        add(_art);
        
        //FontMetrics fm = max.getFontMetrics();
        //int as = fm.getAscent();
        Insets insets = getInsets();
        _title.setBounds(5 + insets.left, 5 + insets.top, 150, 20);
        _trans.setBounds(5 + insets.left, 25 + insets.top, 150, 20);
        _time.setBounds(5 + insets.left, 45 + insets.top, 150, 20);
        _art.setBounds(5 + insets.left, 55 + insets.top, 150, 20);
        
        _eg.setBounds(170 + insets.left, 20 + insets.top, _x, _y);
        _min.setBounds(247 + insets.left, 79 + insets.top, _x, 20);
        _max.setBounds(247 + insets.left, 14 + insets.top, _x, 10);        
        _cur.setBounds(247 + insets.left, (79+14)/2 + insets.top, _x, 10);        
        setPreferredSize(new Dimension(300, 125));        
    }
    
    public void add(float f){
        float fMax = _eg.getMax();
        if (f > fMax)
            _max.setText("" + f);
        _cur.setText("" + f);
        _eg.add(f);
    }   
    
    public void update(StatInfo si){
        _time.setText("processing time: " + ((float)si._time/1000.0f) + " s");
        _trans.setText("transactions: " + si._trans);
        _art.setText("ART: " + ((float)si._time/(float)si._trans)/1000.0f + " s");
    }
    
    protected EvolGraph _eg = null;
    protected JLabel _title, _min, _max, _cur;
    protected JLabel _time, _trans, _art;
    protected int _x = 75;
    protected int _y = 60;
    
}
