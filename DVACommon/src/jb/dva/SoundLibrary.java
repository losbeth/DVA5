//
//  SoundLibrary.java
//  DVA
//
//  Created by Jonathan Boles on 29/05/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//
package jb.dva;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SoundLibrary implements Serializable, Comparable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final List<File> files = new LinkedList<>();
    private Map<String, SoundReference> soundMap = new LinkedHashMap<>();
    private Map<String, String> canonicalNameMap = new LinkedHashMap<>();
    private List<String> sortedKeys = new LinkedList<>();
    private HashSet<String> keys = new HashSet<>();
    private URL icon = null;
    private int longestSoundName = 0;
    private final Properties properties = new Properties();
    private final static Logger logger = LogManager.getLogger(SoundLibrary.class);

    public SoundLibrary(String name) {
        this.name = name;
    }

    public SoundLibrary(String name, List<SoundLibrary> librariesToCombine, URL icon)
    {
        this(name);
        this.icon = icon;

        librariesToCombine.sort(Comparator.comparingInt(SoundLibrary::size));
        for (SoundLibrary lib : librariesToCombine)
        {
            addFallback(lib);
        }
    }

    public void addFile(File f)
    {
        this.files.add(f);
    }

    public void addFallback(SoundLibrary lib) {
        // putAll will replace elements with any conflicting keys.
        // Therefore, create a new hashmap based off the fallback, and then putAll() the library's own map.
        Map<String, SoundReference> tmp1 = new LinkedHashMap<>(lib.soundMap);
        for (Entry<String, SoundReference> e : tmp1.entrySet())
        {
            SoundReference sr = (e.getValue()).copy();
            sr.isFallback = true;
            tmp1.put(e.getKey(), sr);
        }
        tmp1.putAll(this.soundMap);
        this.soundMap = tmp1;

        Map<String, String> tmp2 = new LinkedHashMap<>(lib.canonicalNameMap);
        tmp2.putAll(this.canonicalNameMap);
        this.canonicalNameMap = tmp2;

        keys.addAll(lib.soundMap.keySet());
        sortedKeys.clear();
        sortedKeys.addAll(keys);
        Collections.sort(sortedKeys);

        if (lib.longestSoundName > longestSoundName)
            longestSoundName = lib.longestSoundName;
    }

    public List<File> getFiles() {
        return this.files;
    }

    public String getName()
    {
        return this.name;
    }

    public String toString() {
        return getName();
    }

    public URL getIcon()
    {
        return this.icon;
    }

    public void populate() throws IOException {
        List<URL> urls = new LinkedList<>();
        for (File file : this.files) {
            if (file.isFile() && file.getPath().toLowerCase().endsWith(".jar")) {
                try (JarFile jar = new JarFile(file))
                {
                    Enumeration<JarEntry> jarEntries = jar.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry je = jarEntries.nextElement();
                        URL u = new URL("jar:" + file.toURI().toURL() + "!/" + je.toString());
                        urls.add(u);
                    }
                }
            } else if (file.isDirectory()) {
                File[] soundFiles = file.listFiles();
                if (soundFiles != null) {
                    for (File soundFile : soundFiles) {
                        URL u = soundFile.toURI().toURL();
                        urls.add(u);
                    }
                }
            }

            populateUrlTable(urls);

            keys = new HashSet<>();
            keys.addAll(soundMap.keySet());
            sortedKeys = new LinkedList<>(soundMap.keySet());
            Collections.sort(sortedKeys);
        }
    }

    private void populateUrlTable(List<URL> urls) throws IOException
    {
        logger.debug("Populating library {}", getName());
        // Deal with filenames that are too long by reading their name from the properties file.
        Map<String, URL> truncated = new HashMap<String, URL>();
        for (URL u : urls)
        {
            String urlString = u.toString();
            if (urlString.toLowerCase().endsWith(".wad") ||
                    urlString.toLowerCase().endsWith(".wav") ||
                    urlString.toLowerCase().endsWith(".mp3")) {
                // Found a sound file
                try {
                    String canonicalName = URLDecoder.decode(urlString.substring(urlString.lastIndexOf('/')+1), StandardCharsets.UTF_8.name());
                    if (canonicalName.lastIndexOf('.') > 0) canonicalName = canonicalName.substring(0, canonicalName.lastIndexOf('.'));
                    if (canonicalName.endsWith(".truncated")) {
                        canonicalName = canonicalName.substring(0, canonicalName.length() - 10);
                        truncated.put(canonicalName, u);
                    } else {
                        putSound(canonicalName, u);
                    }
                } catch (UnsupportedEncodingException e) {
                    // not going to happen - value came from JDK's own StandardCharsets
                }
            }
            else if (urlString.toLowerCase().endsWith(".png") ||
                    urlString.toLowerCase().endsWith(".jpg") ||
                    urlString.toLowerCase().endsWith(".jpeg") ||
                    urlString.toLowerCase().endsWith(".gif"))
            {
                // Found a graphics file, use it as the icon
                logger.debug("icon found - {}", urlString);
                this.icon = u;
            }
            else if (urlString.toLowerCase().endsWith("files.list"))
            {
                logger.debug("files.list found");
            }
            else if (urlString.toLowerCase().endsWith("properties.properties"))
            {
                // Found a properties file
                logger.debug("properties file found");
                properties.load(u.openStream());
            }
        }

        for (Entry<String, URL> t : truncated.entrySet()) {
            putSound(properties.getProperty(t.getKey()), t.getValue());
        }
        logger.info("Populated {} with {} items", getName(), canonicalNameMap.size());
    }

    private void putSound(String canonicalName, URL u) {
        boolean isEnding = false, isBeginning = false;
        if (canonicalName.endsWith(".f")) {
            // End of sentence sound
            isEnding = true;
            canonicalName = canonicalName.substring(0, canonicalName.length() - 2);
        }
        else if (canonicalName.endsWith(".b")) {
            // Beginning of sentence sound
            isBeginning = true;
            canonicalName = canonicalName.substring(0, canonicalName.length() - 2);
        }
        String translated = canonicalName.toLowerCase();

        canonicalNameMap.put(translated, canonicalName);

        SoundReference ref;
        if (soundMap.containsKey(translated)) {
            ref = soundMap.get(translated);
        } else {
            ref = new SoundReference();
            ref.canonicalName = canonicalName;
            soundMap.put(translated, ref);
        }

        // The cityrail sound files have two sets -- regular inflection named e.g. Central.mp3
        // and falling inflection named e.g. Central.f.mp3
        // Eventually regular inflection would be used for intermediate words in a sentence and
        // falling inflection at the end of a sentence, but this is not yet implemented.
        if (isBeginning) {
            ref.beginning = u;
            if (ref.regular == null) ref.regular = u;
        } else if (isEnding) {
            ref.ending = u;
            if (ref.regular == null) ref.regular = u;
        } else {
            ref.regular = u;
        }

        if (translated.length() > longestSoundName)
            longestSoundName = translated.length();
    }

    public boolean contains(String s) {
        return keys.contains(s);
    }

    public SoundReference get(String s) {
        return soundMap.get(s);
    }

    public String getCanonicalName(String s) {
        return canonicalNameMap.get(s);
    }

    public int size() {
        return soundMap.size();
    }

    public List<String> keySet() {
        return sortedKeys;
    }

    public int getLongestSoundNameLength() {
        return longestSoundName;
    }

    public boolean supportsInflections()
    {
        return properties.getProperty("SupportsInflections", "").length() > 0;
    }

    @Override
    public int compareTo(Object o) {
        if (name.equals("All")) return 1;
        else if (((SoundLibrary)o).name.equals("All")) return -1;
        return name.compareTo(((SoundLibrary)o).name);
    }
}