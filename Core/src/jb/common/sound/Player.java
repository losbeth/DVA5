//
//  Player.java
//  DVA
//
//  Created by Jonathan Boles on 29/05/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//
package jb.common.sound;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import jb.common.OSDetection;
import jb.common.ExceptionReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Player extends Thread {
    private final List<URL> audioClipList;
    private LevelMeterThread levelMeterThread = null;
    private BigClip clip;
    final static Logger logger = LogManager.getLogger(Player.class);
    private final Runnable longConcatCallback;
    private final Runnable afterConcatCallback;
    private final Timer timer;
    final File tempDir;
    public static final int LongConcatThreshold = 700;

    public Player(List<URL> urlList, LevelMeterPanel levelMeterPanel, File tempDir) {
        this(urlList, null, null, levelMeterPanel, tempDir);
    }

    public Player(List<URL> urlList, Runnable longConcatCallback, Runnable afterConcatCallback, LevelMeterPanel levelMeterPanel, File tempDir) {
        for (URL u : urlList)
        {
            logger.info(u.toString());
        }
        audioClipList = urlList;
        this.longConcatCallback = longConcatCallback;
        this.afterConcatCallback = afterConcatCallback;
        this.timer = new Timer();
        this.tempDir = tempDir;
        if (levelMeterPanel != null)
        {
            this.levelMeterThread = new LevelMeterThread(levelMeterPanel);
            this.levelMeterThread.start();
        }
    }

    private static File getCacheDir(File tempDir)
    {
        return new File(tempDir, "PlayerCache");
    }

    public static void emptyCache(File tempDir)
    {
        File cacheDir = getCacheDir(tempDir);
        if (cacheDir.exists()) {
            File[] cacheFiles = cacheDir.listFiles();
            if (cacheFiles != null) {
                for (File f : cacheFiles) {
                    if (f.getName().toLowerCase().endsWith(".wav") || f.getName().toLowerCase().endsWith(".mp3"))
                        f.delete();
                }
            }
        }
    }

    public void run()
    {
        run2();
    }

    public void run2() {
        try
        {
            if (audioClipList.size() > 1)
            {
                File cacheDir = getCacheDir(tempDir);
                long combinedHash = 17;
                for (Object o : audioClipList)
                {
                    combinedHash = 23 * combinedHash + o.hashCode();
                }
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File[] cacheFiles = cacheDir.listFiles();
                if (cacheFiles != null) {
                    for (File f : cacheFiles) {
                        if ((new Date().getTime() - f.lastModified()) > (60 * 60 * 1000)) {
                            f.delete();
                        }
                    }
                }
                File cacheFile = new File(cacheDir, combinedHash + ".mp3");
                File tempCacheFile = new File(cacheDir, combinedHash + ".temp.mp3");
                logger.debug("Using cache file: {}", cacheFile.getPath());

                final TimerTask longConcatTimerTask = new TimerTask() {
                    public void run() {
                        if (longConcatCallback != null) {
                            longConcatCallback.run();
                        }
                    }
                };

                if (!cacheFile.exists()) {
                    logger.debug("Cache file does not exist, creating");
                    if (longConcatCallback != null) {
                        timer.schedule(longConcatTimerTask, LongConcatThreshold);
                    }

                    // Handle RPi sound taking a short moment to play
                    // https://forums.raspberrypi.com/viewtopic.php?t=292234
                    List<URL> audioClipListCopy = audioClipList;
                    if (OSDetection.isRaspberryPi()) {
                        audioClipListCopy = new LinkedList<>(audioClipList);
                        audioClipListCopy.add(0, Player.class.getResource("/silence500msec.wav"));
                        audioClipListCopy.add(0, Player.class.getResource("/silence200msec.wav"));
                    }
                    MediaConcatenatorFfmpeg.concat(audioClipListCopy, tempCacheFile.getPath(), tempDir);

                    if (tempCacheFile.exists())
                        tempCacheFile.renameTo(cacheFile);
                }

                if (longConcatCallback != null)
                    longConcatTimerTask.cancel();
                if (afterConcatCallback != null)
                    afterConcatCallback.run();

                if (cacheFile.exists()) {
                    logger.debug("Playing cache file");
                    if (!isInterrupted())
                        run1(cacheFile.toURI().toURL());
                }

            } else if (audioClipList.size() == 1) {
                if (!isInterrupted())
                    run1(audioClipList.get(0));
            }

        } catch (IOException e) {
            ExceptionReporter.reportException(e);
            /*} catch (InterruptedException e) {
            DVA.reportException(e);*/
        }

    }

    private void run1(URL u) {
        try {
            GetDataLineLevelAudioInputStream ais;
            double[] levels;

            //System.out.println("Processing url: " + u.toString());
            InputStream istream = new BufferedInputStream(u.openStream(), 102400);
            AudioInputStream in= AudioSystem.getAudioInputStream(istream);

            //AudioInputStream in= AudioSystem.getAudioInputStream(u);
            AudioInputStream din;
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            ais = new GetDataLineLevelAudioInputStream(din, decodedFormat, din.getFrameLength());

            // Create the clip
            clip = new BigClip();

            // This method does not return until the audio file is completely loaded
            clip.open(ais);

            // Calc the sound power levels
            ais.process();
            //System.out.println("done processing");

            if (levelMeterThread != null)
            {
                this.levelMeterThread.next(ais.getLevels(), clip.getMicrosecondLength() / 1000.0);
            }
            playClipAsync(clip);

            // Done processing the next file. Block on the current one, if there's one playing.
            if (clip != null)
            {
                blockOnSound();
            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            ExceptionReporter.reportException(e);
        }

        if (levelMeterThread != null)
        {
            this.levelMeterThread.next(null, 0);
        }
    }

    private synchronized void blockOnSound()
    {
        try
        {
            wait();
        }
        catch (InterruptedException e)
        {
            if (this.levelMeterThread != null)
            {
                this.levelMeterThread.interrupt();
            }
        }
    }

    private synchronized void soundStopped() {
        notifyAll();
    }

    private void playClipAsync(Clip clip)
    {
        // Start playing
        clip.start();

        // Set listener to notify and unblock when done.
        clip.addLineListener(e -> {
            if (e.getType().equals(LineEvent.Type.STOP)) {
                soundStopped();
            }
        });
    }

    public void stopPlaying()
    {
        if (clip != null)
        {
            clip.stop();
            clip = null;
        }
        if (levelMeterThread != null)
        {
            this.levelMeterThread.interrupt();
        }
        interrupt();
    }

    public static class LevelMeterThread extends Thread
    {
        private double[] levels;
        private double durationSec;
        private final LevelMeterPanel levelMeterPanel;

        public LevelMeterThread(LevelMeterPanel levelMeterPanel)
        {
            this.levelMeterPanel = levelMeterPanel;
        }

        public void run()
        {
            try
            {
                do
                {
                    displayLevelsAndWait();
                } while (this.levels != null);
            } catch (InterruptedException ignored) {
            }
            levelMeterPanel.setLevel(0);
        }

        private synchronized void displayLevelsAndWait() throws InterruptedException
        {
            if (levels != null)
            {
                long start = System.currentTimeMillis();
                int index = 0;
                while (index < levels.length) {
                    levelMeterPanel.setLevel(levels[index]);
                    index = (int)(((System.currentTimeMillis() - start) / durationSec / 1000) * levels.length);
                    Thread.sleep(25);
                }
                levelMeterPanel.setLevel(0);
            }
            wait();
        }

        public synchronized void next(double[] levels, double durationSec)
        {
            this.levels = levels;
            this.durationSec = durationSec;
            notifyAll();
        }
    }
}