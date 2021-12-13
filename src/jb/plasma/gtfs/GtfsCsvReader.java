package jb.plasma.gtfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class GtfsCsvReader
{
    public static HashMap<String, Stop> readStops(Path stopsTxt) throws IOException
    {
        HashMap<String, Stop> map = new HashMap<>();
        Files.lines(stopsTxt).skip(1).forEach(line ->
        {
            String[] parts = line.split(",");
            for (int i = 0; i < parts.length; i++)
            {
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
            }

            String id = parts[0].intern(); // e.g. "26401"
            String name = parts[2].intern(); // e.g. "Albury Station Platform 1"
            String parent = parts[9].intern(); // e.g. "26401"
            Stop data = new Stop(id, name,map.getOrDefault(parent, null));
            map.put(id, data);
        });

        return map;
    }

    public static HashMap<String, Route> readRoutes(Path routesTxt) throws IOException
    {
        HashMap<String, Route> map = new HashMap<>();
        Files.lines(routesTxt).skip(1).forEach(line ->
        {
            String[] parts = line.split(",");
            for (int i = 0; i < parts.length; i++)
            {
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
            }

            String id = parts[0].intern();
            Route data = new Route(
                    id, // e.g. "APS_1a"
                    parts[3].intern(), // e.g. "City Circle to Macarthur via Airport"
                    parts[4].intern()  // e.g. "T8 Airport & South Line"
            );
            map.put(id, data);
        });

        return map;
    }

    public static HashMap<String, ServicePeriod> readServicePeriods(Path calendarTxt) throws IOException
    {
        HashMap<String, ServicePeriod> map = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);

        Files.lines(calendarTxt).skip(1).forEach(line ->
        {
            String[] parts = line.split(",");
            for (int i = 0; i < parts.length; i++)
            {
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
            }

            String id = parts[0].intern();
            ServicePeriod data = new ServicePeriod(
                    id, // e.g. "955.134.128"
                    parts[1].charAt(0) == '1', // e.g. "0" or "1"
                    parts[2].charAt(0) == '1',
                    parts[3].charAt(0) == '1',
                    parts[4].charAt(0) == '1',
                    parts[5].charAt(0) == '1',
                    parts[6].charAt(0) == '1',
                    parts[7].charAt(0) == '1',
                    LocalDate.parse(parts[8], formatter), // e.g. "20211129"
                    LocalDate.parse(parts[9], formatter)
            );
            map.put(id, data);
        });

        return map;
    }

    public static HashMap<String, Trip> readTrips(Path tripsTxt, Map<String, Route> routes, Map<String, ServicePeriod> calendars) throws IOException
    {
        HashMap<String, Trip> map = new HashMap<>();

        Files.lines(tripsTxt).skip(1).forEach(line ->
        {
            String[] parts = line.split(",");
            for (int i = 0; i < parts.length; i++)
            {
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
            }

            String tripId = parts[2].intern();
            String routeId = parts[0].intern();
            String calendarId = parts[1].intern();
            Trip data = new Trip(
                    tripId, // e.g. "108B.959.129.12.T.8.68357311"
                    routes.get(routeId), // e.g. "WST_2c"
                    calendars.get(calendarId), // e.g. "959.129.12"
                    parts[3].intern(), // e.g. "Gordon via Lindfield"
                    parts[6].intern()
            );
            map.put(tripId, data);
        });

        return map;
    }

    public static Stream<StopTime> readStopTimes(Path stoptimesTxt, Map<String, Trip> trips, Map<String, Stop> stops) throws IOException
    {
        return Files.lines(stoptimesTxt).skip(1).map(line ->
        {
            String[] parts = line.split(",");
            for (int i = 0; i < parts.length; i++)
            {
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
            }

            boolean pickup = parts[6].trim().charAt(0) == '0'; // e.g. "0" or "1". 0 actually means "yes"
            boolean dropoff = parts[7].trim().charAt(0) == '0'; // e.g. "0" or "1". 0 actually means "yes"
            if (pickup || dropoff)
            {
                return new StopTime(
                        trips.get(parts[0]), // e.g. "108B.959.129.12.T.8.68357311"
                        parts[2], // e.g. "04:46:06"
                        stops.get(parts[3]), // e.g. "2135234"
                        pickup,
                        dropoff
                );
            } else return null;
        }).filter(Objects::nonNull);
    }
}
