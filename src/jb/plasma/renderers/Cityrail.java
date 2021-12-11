package jb.plasma.renderers;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.imageio.ImageIO;
import org.javatuples.Pair;
import jb.common.ExceptionReporter;
import jb.plasma.DepartureData;
import jb.plasma.Drawer;
import jb.plasma.ui.PlasmaPanel;

// Common ancestor for all CityRail renderers. Has stuff that is common to all of them, such as the
// way times are formatted, fonts for common text items, and parameters for scrolling behaviour.
public abstract class Cityrail extends Drawer
{
    protected Font TimeNowFont = null;
    protected Font TimeFont = null;
    protected Font DepartureTimeFont = null;
    protected Font DestinationFont = null;
    protected Font Destination2Font = null;
    protected Font MainFont = null;
    protected Font SmallFont = null;
    protected Font TypeSmallFont = null;

    protected static DateTimeFormatter TimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    protected static DateTimeFormatter DueOutFormat = DateTimeFormatter.ofPattern("HH:mm");

    protected double stationListInc = -0.1 / PlasmaPanel.FPS;
    protected double stationListPosInitial;
    protected double stationListPos;
    protected double stationListSeparation;

    protected LocalDateTime timeNow;

    public void paint(Graphics g)
    {
        super.paint(g);
        timeNow = LocalDateTime.now();
    }

    protected Pair<Integer,Integer> getDueOut(LocalDateTime dueOut)
    {
        int mins = (int)ChronoUnit.MINUTES.between(timeNow, dueOut);
        return new Pair<>((mins / 60), mins % 60);
    }
    
    public void dataChanged(List<DepartureData> newData)
    {
        super.dataChanged(newData);
        stationListPos = stationListPosInitial;
    }
}