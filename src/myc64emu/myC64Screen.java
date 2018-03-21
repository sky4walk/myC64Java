/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */
package myc64emu;

import java.applet.Applet;
import java.awt.Image;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
/**
 *
 * @author andre
 */
public class myC64Screen  {
    public class DoubleBufferWithBufferedImage extends Applet {
        private int gap = 3;
        private int mx, my;
        private int w, h;
        private Image buffer = null;

        public DoubleBufferWithBufferedImage() {            
            setSize(300, 300);
            Dimension d = getSize();
            w = d.width;
            h = d.height;
            buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            
        }
  
    public void paint(Graphics g) {
          Graphics screengc = null;

          screengc = g;

          g = buffer.getGraphics();

          g.setColor(Color.blue);
          g.fillRect(0, 0, w, h);

          g.setColor(Color.red);
          for (int i = 0; i < w; i += gap)
            g.drawLine(i, 0, w - i, h);
          for (int i = 0; i < h; i += gap)
            g.drawLine(0, i, w, h - i);
          screengc.drawImage(buffer, 0, 0, null);
        }
    }
    
    public myC64Screen() {
        JFrame f = new JFrame();
        f.setSize(300, 300);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(new DoubleBufferWithBufferedImage());
        f.setVisible(true);
        
    }
    
}
