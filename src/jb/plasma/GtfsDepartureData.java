package jb.plasma;

import jb.plasma.gtfs.TripInstance;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class GtfsDepartureData extends DepartureData
{
    private static final Logger Logger = LogManager.getLogger(GtfsDepartureData.class);
    private final TripInstance tripInstance;

    public GtfsDepartureData(TripInstance ti)
    {
        String[] headsignParts = ti.Trip.Headsign.split(" via ");
        this.Destination = headsignParts[0];
        this.Destination2 = headsignParts.length >= 2 ? "via " + headsignParts[1] : "";
        this.Line = ti.Trip.Route.Description;
        this.Type = ti.LimitedStops ? "Limited Stops" : "All Stops";
        this.Cars = ti.Trip.Cars;
        this.Platform = Integer.parseInt(ti.Platform.Name.split(" Station Platform ")[1]);
        this.Stops = ti.RemainingStopList;
        this.StopCarRanges = ti.RemainingStopsCarRanges.stream()
                .map(carRange ->  {
                    if (carRange == null) {
                        return null;
                    } else if (carRange.getValue0() == carRange.getValue1()) {
                        return "Car " + carRange.getValue0();
                    } else if (carRange.getValue1() - carRange.getValue0() + 1 < Cars) {
                        return "Car " + carRange.getValue0() + "-" + carRange.getValue1();
                    } else {
                        return null;
                    }
                }).toArray(String[]::new);
        this.DueOut = ti.At;

        this.tripInstance = ti;
    }

    public void logDetails()
    {
        Logger.info("Trip: {} departs from '{}' at {} to '{}' with {} cars. Continues as {}",
                tripInstance.Trip.Name,
                tripInstance.Platform.Name,
                tripInstance.At,
                tripInstance.Trip.Headsign,
                tripInstance.Trip.Cars,
                tripInstance.BlockContinuingTrip != null ? tripInstance.BlockContinuingTrip.Name : "<no continuation>");
    }
}
