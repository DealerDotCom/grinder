// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.swingui;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * TestImage.java
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ImageTest{
    
    public static void main(String[] args)
    {
	new ImageTest();
    }

    public ImageTest()
    {
	final JFrame frame = new JFrame("Test images");
	final Container topLevelPane = frame.getContentPane();
	topLevelPane.setLayout(new GridLayout());

	final String images[] = {
	    "grinder.gif",
	    "start.gif",
	    "stop.gif",
	    "start-processes.gif",
	    "stop-processes.gif",
	    "summary.gif",
	    "save.gif",
	    "pause.gif",
	    "reset.gif",
	};

	for (int i=0; i<images.length; i++) {
	    final ImageIcon logoIcon = getImageIcon(images[i]);

	    if (logoIcon != null) {
		final JLabel logo = new JLabel(logoIcon);
		topLevelPane.add(logo);
	    }
	}

	frame.addWindowListener(new WindowCloseAdapter());
	frame.pack();
	frame.show();
    }

    private URL getResource(String name, boolean warnIfMissing)
    {
	final URL url = this.getClass().getResource("resources/" + name);

	if (warnIfMissing && url == null) {
	    System.err.println("Warning - could not load resource " + name);
	}

	return url;
    }
    
    private ImageIcon getImageIcon(String resourceName)
    {
	final URL resource = getResource(resourceName, false);

	return resource != null ? new ImageIcon(resource) : null;
    }

    private static final class WindowCloseAdapter extends WindowAdapter
    {
	public void windowClosing(WindowEvent e)
	{
	    System.exit(0);
	}
    }
} // ImageTest
