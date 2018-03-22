/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */
package myc64emu;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
/**
 *
 * @author andre
 */
public class myC64Screen  {
    public class DoubleBufferWithBufferedImage extends Applet {
        private BufferedImage  buffer = null;       
        public DoubleBufferWithBufferedImage(int sizeX,int sizeY) {            
            setSize(sizeX, sizeY);
            Dimension d = getSize();
            buffer = new BufferedImage(d.width, d.height, 
                    BufferedImage.TYPE_INT_RGB);            
        }        
        public void paint(Graphics g) {
            super.paint(g);
            g.drawImage(buffer, 0, 0, null);
        }
        public void setColor(int x, int y, Color col) {
            buffer.setRGB(x, y, col.getRGB());
        }
    }
    
    private JFrame f = new JFrame();
    private DoubleBufferWithBufferedImage dbImage;
    
    public void setPixelCol(int x, int y, int r, int g, int b) {
        dbImage.setColor(x, y, new Color(r,g,b));
    }
    public int getSizeX() {
        return f.getWidth();
    }
    public int getSizeY() {
        return f.getHeight();
    }
    public myC64Screen(int sizeX, int sizeY) {
        f.setSize(sizeX, sizeY);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dbImage = new DoubleBufferWithBufferedImage(sizeX,sizeY);
        f.getContentPane().add(dbImage);
        f.setVisible(true);         
    }
    
}
