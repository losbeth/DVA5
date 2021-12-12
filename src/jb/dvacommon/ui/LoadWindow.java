//
//  LoadWindow.java
//  DVA
//
//  Created by Jonathan Boles on 29/01/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package jb.dvacommon.ui;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import jb.common.ExceptionReporter;
import jb.common.sound.Player;
import jb.common.ui.AbsoluteOverlayLayout;
import jb.common.ui.WindowFader;
import jb.common.ui.WindowUtils;
import jb.dvacommon.DVA;
import jb.plasma.Drawer;
import org.swixml.SwingEngine;

public class LoadWindow {
    private static Font ArialRegular;
    private static Font ArialBold;

    public JFrame frame;
    public JTextPane aboutTextPane;
    public JLabel loadLabel;
    public JButton closeButton;
    public JButton licenceButton;
    public JPanel buttonPanel;
    public JPanel imagePanel;
    public JLabel versionLabel;
    private WindowFader fader;

    public LoadWindow() {
        SwingEngine renderer = new SwingEngine(this);
        try {
            frame = (JFrame) renderer.render(LoadWindow.class.getResource("/jb/dvacommon/ui/resources/loadwindow.xml"));
            imagePanel.setLayout(new AbsoluteOverlayLayout(imagePanel));
            Dimension imageDimensions = imagePanel.getPreferredSize();

            ArialRegular = Font.createFont(Font.TRUETYPE_FONT, Drawer.class.getResourceAsStream("/arial.ttf"));
            ArialBold = Font.createFont(Font.TRUETYPE_FONT, Drawer.class.getResourceAsStream("/arialbd.ttf"));

            // Read build number
            Properties props = new Properties();
            props.load(LoadWindow.class.getResourceAsStream("/buildnumber.txt"));

            JLabel dvaLabel = new JLabel("DVA");
            dvaLabel.setFont(ArialBold.deriveFont(Font.PLAIN, 86));
            dvaLabel.setForeground(Color.white);
            dvaLabel.setLocation((imageDimensions.width - dvaLabel.getPreferredSize().width) / 2, (imageDimensions.height - dvaLabel.getPreferredSize().height) / 2);
            imagePanel.add(dvaLabel, 0);

            Font imageFont = ArialRegular.deriveFont(Font.PLAIN, 14);
            loadLabel = new JLabel("Loading");
            loadLabel.setFont(imageFont);
            loadLabel.setForeground(Color.white);
            loadLabel.setLocation(12, imageDimensions.height - 24);
            imagePanel.add(loadLabel, 0);

            versionLabel = new JLabel(DVA.VersionString + " (" + props.getProperty("build.number") + ")");
            versionLabel.setFont(imageFont);
            versionLabel.setForeground(Color.white);
            versionLabel.setLocation(imageDimensions.width - versionLabel.getPreferredSize().width - 12, imageDimensions.height - 24);
            imagePanel.add(versionLabel, 0);

            String aboutText = aboutTextPane.getText();
            aboutText = aboutText.replace("$VERSION$", DVA.VersionString);
            aboutText = aboutText.replace("$BUILDNUMBER$", props.getProperty("build.number", "[no build number]"));
            aboutText = aboutText.replace("$RAILWAVSURL$", "http://railwavs.railmedia.com.au");
            aboutText = aboutText.replace("$EMAIL$", "jaboles@fastmail.fm");
            aboutText = aboutText.replace("$COPYRIGHT$",  DVA.CopyrightMessage);
            aboutText = aboutText.replace("$JAVAVERSION$", System.getProperty("java.version"));
            aboutText = aboutText.replace("$JAVAARCH$", System.getProperty("os.arch"));
            aboutTextPane.setText(aboutText);

            aboutTextPane.addHyperlinkListener(event -> {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String url = event.getURL().toString();
                    try {
                        Desktop.getDesktop().browse(URI.create(url));
                    } catch (IOException e) {
                        ExceptionReporter.reportException(e);
                    }
                }
            });

            closeButton.addActionListener(e -> frame.dispose());

            licenceButton.addActionListener(e -> {
                LicenceWindow lw = new LicenceWindow();
                lw.showSubsequentTimes();
            });
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
    }

    public void setText(String s) {
        loadLabel.setText((s.isEmpty()? " " : s));
    }

    public void show(boolean showCloseButton, boolean introSound, boolean fade) {
        if (introSound)
        {
            Player p;
            try {
                List<URL> startSoundList = new LinkedList<>();
                startSoundList.add(LoadWindow.class.getResource("/start4-CHIME.mp3"));
                startSoundList.add(LoadWindow.class.getResource("/start4-D.f.mp3"));
                startSoundList.add(LoadWindow.class.getResource("/start4-V.f.mp3"));
                startSoundList.add(LoadWindow.class.getResource("/start4-A.f.mp3"));
                startSoundList.add(LoadWindow.class.getResource("/start4-5.f.mp3"));
                p = new Player(startSoundList, null, DVA.getTemp());
                p.start();
            } catch (Exception e) {
                ExceptionReporter.reportException(e);
            }
        }

        if (showCloseButton) {
            versionLabel.setText("");
        }
        buttonPanel.setVisible(showCloseButton);
        loadLabel.setText("");
        frame.pack();
        WindowUtils.center(frame);
        frame.pack();
        if (fade)
        {
            fader = new WindowFader(frame, 350, 15);
            fader.fadeIn();
        }
        else
        {
            fader = null;
            frame.setVisible(true);
        }
    }

    public void dispose() {
        if (fader != null)
        {
            fader.fadeOut();
        }
        else
        {
            frame.dispose();
        }
    }
}
