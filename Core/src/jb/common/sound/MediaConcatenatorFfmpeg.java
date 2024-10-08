package jb.common.sound;

import jb.common.OSDetection;
import jb.common.ExceptionReporter;
import jb.common.FileUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MediaConcatenatorFfmpeg
{
    private final static Logger logger = LogManager.getLogger(MediaConcatenatorFfmpeg.class);

    public static void concat(List<URL> urlList, String outputFile, File tempDir) {

        logger.info("Concatenating {} items", urlList.size());

        try
        {
            File concatTemp = new File(tempDir, "mc");
            if (!concatTemp.exists() && !concatTemp.mkdirs()) {logger.warn("Failed to mkdir {}", concatTemp);}

            File ffmpeg = new File(FileUtilities.getJarFolder(MediaConcatenatorFfmpeg.class),
                    OSDetection.isWindows() ? "ffmpeg.exe" : "ffmpeg");
            if (!ffmpeg.exists()) ffmpeg = new File("ffmpeg/mac-arm64/ffmpeg");
            List<String> ffmpegCmd = new LinkedList<>();
            ffmpegCmd.add(ffmpeg.getAbsolutePath());
            StringBuilder ffmpegCmdFilterComplex = new StringBuilder();

            Iterator<URL> it = urlList.iterator();
            for (int i = 0; it.hasNext(); i++)
            {
                URL u = it.next();
                logger.debug("Concatenating: {}", u);

                File tempFile = new File(concatTemp, Integer.toString(i));
                Files.copy(u.openStream(), Paths.get(tempFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
                ffmpegCmd.add("-i");
                ffmpegCmd.add(Integer.toString(i));
                ffmpegCmdFilterComplex.append("[").append(i).append(":0]");
            }

            ffmpegCmd.add("-filter_complex");
            ffmpegCmdFilterComplex.append("concat=n=");
            ffmpegCmdFilterComplex.append(urlList.size());
            ffmpegCmdFilterComplex.append(":v=0:a=1[out]");
            ffmpegCmd.add(ffmpegCmdFilterComplex.toString());
            ffmpegCmd.add("-map");
            ffmpegCmd.add("[out]");
            ffmpegCmd.add("-codec:a");
            if (outputFile.toLowerCase().endsWith(".wav")) {
                ffmpegCmd.add("pcm_s16le");
            } else if (outputFile.toLowerCase().endsWith(".mp3")) {
                ffmpegCmd.add("libmp3lame");
                ffmpegCmd.add("-q:a");
                ffmpegCmd.add("2");
            }
            ffmpegCmd.add(outputFile);

            logger.info("Running ffmpeg: {}", String.join(" ", ffmpegCmd));
            Process p = new ProcessBuilder(ffmpegCmd)
                    .redirectErrorStream(true)
                    .directory(concatTemp)
                    .start();

            Thread readerThread = new Thread(() -> {
                try {
                    BufferedReader processReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = processReader.readLine()) != null)
                        logger.debug(line);
                    processReader.close();
                } catch (IOException e) {
                    ExceptionReporter.reportException(e);
                }
            });

            readerThread.start();
            p.waitFor();
            readerThread.join();
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
    }
}