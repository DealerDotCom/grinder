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

/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @version $Revision$
 */
class EvolGraph extends JComponent{

    EvolGraph(){
        _data = new float[_x];
        setPreferredSize(new Dimension(_x, _y));
    }

    EvolGraph(int x, int y, float min, float max){
        _x = x;
        _y = y;
        _min = min;
        _max = max;
        _data = new float[_x];
        setPreferredSize(new Dimension(_x, _y));        
    }
        
    EvolGraph(float min, float max){
        _min = min;
        _max = max;
        _data = new float[_x];
        setPreferredSize(new Dimension(_x, _y));        
    }

    public void add(float f){       
        for (int i=0; i<(_x-1); i++)
            _data[i] = _data[i+1];
        _data[(_x-1)] = f;
        if (f>_max)
            _max = f;
        repaint();
    }
    
    public float getMax(){
        return _max;
    }

    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);
        g.setColor(Color.gray);
        for (int i=(_x-1); i>=0; i--){
            if (_data[i] > 0){
                g.drawLine(i,(_y-1),i,_y-(int)((_data[i]*_y)/_max));
            }
        }
    }
    
    protected int _x = 75;
    protected int _y = 60;
    protected float _data[] = null;
    protected float _min = 0.0f;
    protected float _max = 100.0f;

}
