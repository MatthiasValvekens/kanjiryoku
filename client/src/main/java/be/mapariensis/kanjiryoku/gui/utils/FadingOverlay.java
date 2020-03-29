package be.mapariensis.kanjiryoku.gui.utils;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;

public class FadingOverlay {
    private boolean hide = true;
    private float opacity = 0;
    private final Image image;
    private final JComponent component;
    private static final Integer INTERVAL = 50;
    private final ActionListener updater = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            opacity += 0.1;
            if (opacity >= 1) {
                hide = true;
                timer.stop();
                timer = null;
            }
            component.repaint();
        }
    };
    private Timer timer;

    public FadingOverlay(JComponent component, Image image) {
        this.image = image;
        this.component = component;
    }

    public void startFade(boolean block) {
        if (image == null)
            return;
        opacity = 0;
        hide = false;
        if (timer != null && timer.isRunning())
            timer.stop();
        timer = new Timer(INTERVAL, updater);
        timer.start();
        if (block) {
            while (timer != null && timer.isRunning()) {
                try {
                    Thread.sleep(2 * INTERVAL);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void paint(Graphics g) {
        if (hide || image == null)
            return;
        ((Graphics2D) g).setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, opacity));
        g.drawImage(image, 0, 0, component);
    }
}
