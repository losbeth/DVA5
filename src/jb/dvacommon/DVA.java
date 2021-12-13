package jb.dvacommon;
import java.awt.Rectangle;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.UIManager;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.*;
import jb.common.*;
import jb.common.jna.windows.GDI32Ex;
import jb.common.jna.windows.Shell32Ex;
import jb.common.jna.windows.User32Ex;
import jb.common.sound.LevelMeterPanel;
import jb.common.sound.Player;
import jb.common.sound.MediaConcatenatorFfmpeg;
import jb.common.ui.ProgressWindow;
import jb.dva.Script;
import jb.dva.SoundLibrary;
import jb.dvacommon.ui.DVAShell;
import jb.dvacommon.ui.LicenceWindow;
import jb.dvacommon.ui.LoadWindow;
import jb.plasma.gtfs.GtfsGenerator;
import jb.plasma.gtfs.GtfsTimetable;
import jb.plasma.gtfs.GtfsTimetableTranslator;
import jb.plasma.ui.ScreenSaverSettingsDialog;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DVA {
    DVAShell mainWindow;
    Map<String, SoundLibrary> soundLibraryMap = new LinkedHashMap<>();
    Player player;
    ArrayList<URL> verifiedUrlList;
    final static Logger logger = LogManager.getLogger(DVA.class);

    public static final String VersionString = "5.5.0";
    public static final String CopyrightMessage = "Copyright © Jonathan Boles 1999-2021";

    // 'Special' sounds which are only shown after enabling the option
    public static final String[] SPECIAL_SOUNDS_Array = new String[] {
        "dTrog remix",
        "AnnouncementRail",
    };
    public static final Set<String> SPECIAL_SOUNDS = new HashSet<>(Arrays.asList(SPECIAL_SOUNDS_Array));
    
    // Set fallback libraries for incomplete sound libraries
    public static final String[][] FALLBACK_LIBRARIES_Array = new String[][] {
        { "dTrog remix", "Sydney-Male" },
        { "AnnouncementRail", "Sydney-Female" },
        { "Sydney-Male (replaced low-quality sounds)", "Sydney-Male" },
        { "Sydney-Female (replaced low-quality sounds)", "Sydney-Female" },
    };
    public static final Map<String, String> FALLBACK_LIBRARIES = new HashMap<>();
    
    static {
        for (String[] arr : FALLBACK_LIBRARIES_Array) {
            FALLBACK_LIBRARIES.put(arr[0], arr[1]);
        }
    }

    public DVA() {
        logger.info("DVA: {}, Java: {} {}", VersionString, System.getProperty("java.version"), System.getProperty("os.arch"));
        logger.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        logger.info("Temp is: {}", System.getProperty("java.io.tmpdir"));
        logger.info("FFmpeg.log: {}ffmpeg.log", System.getProperty("java.io.tmpdir"));
        Player.emptyCache(getTemp());
    }
    
    public DVA(boolean showMainWindow, boolean showLoadingProgress) {
        this();
        LoadWindow lw = null;
        if (showLoadingProgress) {
            lw = new LoadWindow();
            lw.show(false, showMainWindow, true);
        }

        try {
            final ObjectCache<Map<String,SoundLibrary>> mc = new ObjectCache<>(getTemp(), "soundlibrarymap" + VersionString);
            final ObjectCache<SoundLibrary> c = new ObjectCache<>(getTemp(), "soundlibrary" + VersionString);
            populateSoundLibraries();
            
            // Map cache is keyed to size of the map so that if new libraries are added or removed the cache is refreshed.
            soundLibraryMap = mc.load(DVA.class, Integer.toString(soundLibraryMap.size()), () -> {
                c.emptyCache();
                return soundLibraryMap;
            });
            
            for (final Map.Entry<String, SoundLibrary> entry : soundLibraryMap.entrySet()) {
                if (showLoadingProgress) lw.setText("Loading sound libraries... " + entry.getValue().getName());
                SoundLibrary library = c.load(DVA.class, entry.getKey(), () -> {
                    SoundLibrary l = entry.getValue();
                    try {
                        l.populate();
                    } catch (Exception ignored) {}
                    return l;
                });
                soundLibraryMap.put(entry.getKey(), library);
            }
            for (SoundLibrary library : soundLibraryMap.values()) {
                if (FALLBACK_LIBRARIES.containsKey(library.getName())) {
                    library.addFallback(soundLibraryMap.get(FALLBACK_LIBRARIES.get(library.getName())));
                }
            }
            soundLibraryMap.put("All", new SoundLibrary("All", new LinkedList<>(soundLibraryMap.values()), SoundLibrary.shrinkIcon(DVA.class.getResource("/all.png"))));
            //if (p != null) p.join();

            if (showLoadingProgress) lw.setText("Fetching GTFS timetable... ");
            GtfsGenerator.initialize(new File(getTemp(), "GtfsTimetable").toPath());
            GtfsGenerator.getInstance().download();

            if (showLoadingProgress) lw.setText("Reading timetable data... ");
            GtfsTimetable tt = GtfsGenerator.getInstance().read();

            int steps = GtfsTimetable.getAnalysisStepCount();
            if (showLoadingProgress) lw.setText("Analysing timetable... indexing locations");
            for (int i = 0; i < steps; i++)  {
                if (showLoadingProgress) lw.setText("Analysing timetable... " + tt.analyse(i));
            }
            GtfsTimetableTranslator.initialize(tt);

            if (showMainWindow) {
                if (lw != null) {
                    lw.setText("Loading...");
                }
                mainWindow = new DVAShell(DVA.this);
            }

            if (showLoadingProgress) lw.setText("");
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }

        if (showLoadingProgress) lw.dispose();

        if (showMainWindow) {
            mainWindow.setVisible(true);
        }
    }

    public DVA(String soundLibrary) {
        this();

        try {
            final ObjectCache<Map<String,SoundLibrary>> mc = new ObjectCache<>(getTemp(), "soundlibrarymap" + VersionString);
            final ObjectCache<SoundLibrary> c = new ObjectCache<>(getTemp(), "soundlibrary" + VersionString);
            populateSoundLibraries();

            // Map cache is keyed to size of the map so that if new libraries are added or removed the cache is refreshed.
            soundLibraryMap = mc.load(DVA.class, Integer.toString(soundLibraryMap.size()), () -> {
                c.emptyCache();
                return soundLibraryMap;
            });

            logger.info("Loading sound library '{}'", soundLibrary);
            String fallbackLibraryName = FALLBACK_LIBRARIES.get(soundLibrary);

            SoundLibrary singleLibrary = c.load(DVA.class, soundLibrary, () -> {
                SoundLibrary l = soundLibraryMap.get(soundLibrary);
                try {
                    l.populate();
                } catch (Exception ignored) {}
                return l;
            });
            soundLibraryMap.put(soundLibrary, singleLibrary);
            SoundLibrary fallbackLibrary = c.load(DVA.class, fallbackLibraryName, () -> {
                SoundLibrary l = soundLibraryMap.get(fallbackLibraryName);
                try {
                    l.populate();
                } catch (Exception ignored) {}
                return l;
            });
            soundLibraryMap.put(fallbackLibraryName, fallbackLibrary);

            for (SoundLibrary library : soundLibraryMap.values()) {
                if (FALLBACK_LIBRARIES.containsKey(library.getName())) {
                    library.addFallback(soundLibraryMap.get(FALLBACK_LIBRARIES.get(library.getName())));
                }
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
    }

    public Collection<SoundLibrary> getSoundLibraryList() {
        return soundLibraryMap.values();
    }

    public Collection<String> getSoundLibraryNames() {
        return soundLibraryMap.keySet();
    }

    public void quit() {
        System.exit(0);
    }

    public Player play(LevelMeterPanel levelMeterPanel, Script s, Runnable longConcatCallback, Runnable afterConcatCallback) {
        try {
            ArrayList<URL> al = s.getTranslatedUrlList(getSoundLibrary(s.getVoice()));
            player = new Player(al, longConcatCallback, afterConcatCallback, levelMeterPanel, getTemp());

            return player;
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
        return null;
    }

    public Player play(LevelMeterPanel levelMeterPanel, Script s) {
        return play(levelMeterPanel, s, null, null);
    }

    // Check that the script correctly and fully translates into a playable announcement, given the available
    // sound files.
    public int verify(Script script) {
        try {
            verifiedUrlList = script.getTranslatedUrlList(getSoundLibrary(script.getVoice()));
        } catch (Exception ex) {
            try {
                return Integer.parseInt(ex.toString().substring(ex.toString().lastIndexOf(' ')+1));
            } catch (Exception ex2) {
                ExceptionReporter.reportException(ex);
                ExceptionReporter.reportException(ex2);
            }
        }
        return -1;
    }

    public ArrayList<URL> getVerifiedUrlList() {
        return verifiedUrlList;
    }

    public void export(Script script, String targetFile) throws Exception
    {
        // Convert to list of URLs to the wads
        ArrayList<URL> al = script.getTranslatedUrlList(getSoundLibrary(script.getVoice()));
        File parent = (new File(targetFile)).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        MediaConcatenatorFfmpeg.concat(al, targetFile, null, getTemp());
    }

    public String getCanonicalScript(Script script) {
        try {
            return script.getCanonicalScript(getSoundLibrary(script.getVoice()));
        } catch (Exception ex) {
            ExceptionReporter.reportException(ex);
        }
        return null;
    }

    public void stop() {
        player.stopPlaying();
    }

    public SoundLibrary getSoundLibrary(String key) {
        return soundLibraryMap.get(key);
    }

    private SoundLibrary getOrCreateSoundLibrary(String name) {
        if (soundLibraryMap.containsKey(name)) {
            return soundLibraryMap.get(name);
        } else {
            SoundLibrary sl = new SoundLibrary(name);
            soundLibraryMap.put(name, sl);
            return sl;
        }
    }

    // This is where it all starts.
    public static void main(String[] args) {
        ExceptionReporter.ApplicationName = "DVA";
        ExceptionReporter.ApplicationVersion = DVA.VersionString;

        if (args.length > 0 && args[0].equalsIgnoreCase("/clearsettings")) {
            // Delete settings
            Settings.deleteAll();
            System.exit(0);
        }

        if (!getTemp().exists()) {
            getTemp().mkdirs();
        }

        // Apple UI stuff
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "DVA");
        System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.brushMetalLook", "true");

        // Set AUMI on Windows 7, to fix two separate DVA taskbar icons appearing during
        // launch (DVA.exe and java.exe)
        boolean isWindows = OSDetection.isWindows();
        if (isWindows && VersionComparator.Instance.compare(System.getProperty("os.version"), "6.1") >= 0) {
            Shell32Ex.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString("jb.DVA"));
        }

        /*try {
		 // High-DPI scaling
		 int pixelPerInch=java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		 javax.swing.JOptionPane.showMessageDialog(null, "pixelPerInch: "+pixelPerInch);

		 for (Iterator i = UIManager.getLookAndFeelDefaults().keySet().iterator(); i.hasNext();) {
		 String key = (String) i.next();
		 System.out.println(key);
		 if(key.endsWith(".font")) {
		 Font font = UIManager.getFont(key);
		 Font biggerFont = font.deriveFont((pixelPerInch / 72f) * font.getSize2D());
		 // change ui default to bigger font
		 UIManager.put(key,biggerFont);
		 }
		 }
		 } catch (Exception e) {
		 // wtf?? Impossible situation
		 }*/

        try {
            // Set native look and feel
            UIManager.setLookAndFeel(DVAShell.getLookAndFeelClassName(Settings.getLookAndFeelName()));
        } catch (Exception e) {
            // wtf?? Impossible situation
        }

        logger.info("argc: {}", args.length);

        // Run the program in different ways depending on command line switches
        if (args.length > 0 && args[0].equalsIgnoreCase("/x"))
        {
            // Sound download during Windows installer -- exit
            fetchSoundJars();
        }
        else if (args.length >= 3 && args[0].equalsIgnoreCase("/play"))
        {
            String soundLibrary = args[1];
            String announcementText = args[2];
            Script announcement = new Script(soundLibrary, announcementText);
            DVA dva = new DVA(soundLibrary);
            dva.verify(announcement);
            logger.info("Playing: '{}'", announcementText);
            Player p = dva.play(null, announcement, null, null);
            p.start();
            new Thread(() -> {
                try {
                    p.join();
                    logger.info("Success.");
                    System.exit(0);
                } catch (InterruptedException ignored) {
                }
            }).start();
        }
        else if (args.length >= 4 && args[0].equalsIgnoreCase("/export"))
        {
            String filename = args[1];
            String soundLibrary = args[2];
            String announcementText = args[3];
            Script announcement = new Script(soundLibrary, announcementText);
            DVA dva = new DVA(soundLibrary);
            dva.verify(announcement);
            logger.info("Exporting to '{}': '{}'", filename, announcementText);
            try {
                dva.export(announcement, filename);
            } catch (Exception e) {
                ExceptionReporter.reportException(e);
            }
            logger.info("Success.");
        }
        else
        {
            if (args.length > 0 && args[0].equalsIgnoreCase("/s")) {
                // Windows screen saver mode
                jb.plasma.ui.PlasmaUI.screenSaver(false, null);
            } else if (args.length > 0 && (args[0].equalsIgnoreCase("/c") || args[0].startsWith("/c:") || args[0].startsWith("/C:"))) {
                // Run the plasma UI in screen saver setting mode, outside of the regular
                // application.
                if (Settings.soundJarsNotDownloaded())
                {
                    fetchSoundJars();
                }
                showLicenceIfNotRead();
                new ScreenSaverSettingsDialog().setVisible(true);
            } else if (OSDetection.isWindows() && args.length > 0 && args[0].toLowerCase().startsWith("/p") && args.length >= 2) {
                //JOptionPane.showMessageDialog(null, args[1]);
                // Windows screen saver preview mode
                WinDef.HWND rvhwnd;
                int rv;
                boolean rvb;

                // HWnd of the screensaver preview mini-window rooted in the screensaver settings dialog box 
                WinDef.HWND phwnd = new WinDef.HWND(Pointer.createConstant(Long.parseLong(args[1])));
                logger.info("Preview parent window handle: {}", phwnd);

                // Get dimensions of the Windows window to install the plasma window into
                WinDef.RECT parentRectNative = new WinDef.RECT();
                rvb = User32Ex.INSTANCE.GetClientRect(phwnd, parentRectNative);
                logger.info("GetClientRect returned {}", rvb);
                Rectangle parentRect = parentRectNative.toRectangle();

                // Scale dimensions by the DPI setting
                // HAX!!!1 http://stackoverflow.com/questions/7003316/windows-display-setting-at-150-still-shows-96-dpi
                WinDef.HDC dc = User32.INSTANCE.GetDC(new WinDef.HWND(Pointer.NULL));
                int virtualWidth = GDI32.INSTANCE.GetDeviceCaps(dc, GDI32Ex.HORZRES);
                int physicalWidth = GDI32.INSTANCE.GetDeviceCaps(dc, GDI32Ex.DESKTOPHORZRES);
                User32.INSTANCE.ReleaseDC(new WinDef.HWND(Pointer.NULL), dc);
                double scaleFactor = (double)physicalWidth / (double)virtualWidth;
                parentRect.setSize((int)(parentRect.getWidth() * scaleFactor), (int)(parentRect.getHeight() * scaleFactor));
                
                // Create the plasma window
                JFrame w = jb.plasma.ui.PlasmaUI.screenSaver(true, parentRect.getSize()).get(0);
                WinDef.HWND hwnd = new WinDef.HWND(Native.getWindowPointer(w));
                logger.info("Window handle: {}", hwnd);

                // Shove the plasma window into the screensaver settings dialog box
                rvhwnd = User32.INSTANCE.SetParent(hwnd, phwnd);
                logger.info("SetParent returned {}", rvhwnd);
                rv = User32.INSTANCE.SetWindowLong(hwnd, User32.GWL_STYLE, User32.INSTANCE.GetWindowLong(hwnd, User32.GWL_STYLE) | User32.WS_CHILD);
                logger.info("SetWindowLong returned {}", rv);
                rvb = User32.INSTANCE.SetWindowPos(hwnd, phwnd, 0, 0, (int)parentRect.getWidth(), (int)parentRect.getHeight(), User32.SWP_NOZORDER | User32Ex.SWP_NOACTIVATE);
                logger.info("SetWindowPos returned {}", rvb);
            } else {
                showLicenceIfNotRead();
                
                if (Settings.soundJarsNotDownloaded())
                {
                    fetchSoundJars();
                }
                
                // Regular application
                new DVA(true, true);
            }
        }
    }

    // Find folders and jars next to the application and load them as sound libraries.
    public void populateSoundLibraries() {
        File f = getSoundJarsFolder();
        if (f.exists() && f.isDirectory()) {
            File[] soundDirs = f.listFiles();
            if (soundDirs != null) {
                for (File soundDir : soundDirs) {
                    String path = soundDir.getPath();
                    String name;
                    if (soundDir.isDirectory() && !path.toLowerCase().endsWith(".app")) {
                        name = path.substring(path.lastIndexOf(File.separatorChar) + 1);
                        if (!SPECIAL_SOUNDS.contains(name) || Settings.specialSoundsEnabled()) {
                            getOrCreateSoundLibrary(name).addFile(soundDir);
                        }
                    } else if (path.toLowerCase().endsWith(".jar")) {
                        name = path.substring(path.lastIndexOf(File.separatorChar) + 1, path.length() - 4);
                        if (!SPECIAL_SOUNDS.contains(name) || Settings.specialSoundsEnabled()) {
                            getOrCreateSoundLibrary(name).addFile(soundDir);
                        }
                    }
                }
            }
        }
    }
    
    private static void showLicenceIfNotRead()
    {
        // Show the licence if it hasn't been displayed for this version.
        if (!Settings.isLicenceRead())
        {
            LicenceWindow licenceWindow = new LicenceWindow();
            licenceWindow.showFirstTime();
            if (!licenceWindow.accepted())
            {
                System.exit(0);
            }

            Settings.setLicenceRead();
        }
    }
    
    private static void fetchSoundJars()
    {
        ProgressWindow pw = new ProgressWindow("Download Progress", "Updating sound libraries...");
        pw.setProgressText("Checking for updated sound libraries");
        ProgressAdapter pa = new ProgressAdapter(pw);
        pw.show();
        try {
            new CloudSoundJarFetcher(
                new URL(new URL("https://dvaupdate.blob.core.windows.net/"), WAzureUpdater.SoundJarsContainerName + "/"),
                new URL(new URL("https://dvaupdate.blob.core.windows.net/"), WAzureUpdater.MetadataContainerName + "/" + WAzureUpdater.SoundJarsList))
            .doFetch(pa)
            .join();
        } catch (MalformedURLException | InterruptedException ignored) {
        }
    }

    public static File getTemp()
    {
        return new File(System.getProperty("java.io.tmpdir"), "DVA");
    }

    public static File getSoundJarsFolder()
    {
        if (OSDetection.isWindows())
        {
            return new File(FileUtilities.getUserApplicationDataFolder(), "DVA");
        }
        else if (OSDetection.isMac())
        {
            return new File("/Users/Shared/Library/Application Support/DVA");
        }
        else
        {
            return new File(FileUtilities.getUserApplicationDataFolder(), ".dva");
        }
    }
}
