package com.krishang.handoff;

import static org.junit.Assert.*;
import static com.krishang.handoff.TripHandoff.Result.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TripHandoffTest {
    private final TripHandoff.Trip trip =
            new TripHandoff.Trip(1, List.of("Coffee", "Charger"), "Trailhead");

    @Test public void lostAckThenRetryPreservesOneOrderedTrip() {
        var car = new TripHandoff.Car(null);
        var phone = new TripHandoff.Phone(trip, false);
        assertEquals(ACCEPTED, car.receive(phone.trip()).result());
        assertFalse(phone.delivered());
        var ack = car.receive(phone.trip());
        assertEquals(DUPLICATE, ack.result());
        phone.acknowledge(ack);
        assertTrue(phone.delivered());
        assertEquals(List.of("Coffee", "Charger"), car.displayed().stops());
        assertEquals("Trailhead", car.displayed().destination());
    }

    @Test public void newRevisionReplacesTripAndOldAckCannotDeliverIt() {
        var car = new TripHandoff.Car(null);
        var phone = new TripHandoff.Phone(trip, false);
        var old = car.receive(phone.trip());
        phone.acknowledge(old);
        assertTrue(phone.delivered());
        phone.revise("Beach");
        phone.acknowledge(old);
        assertFalse(phone.delivered());
        phone.acknowledge(car.receive(phone.trip()));
        assertTrue(phone.delivered());
        assertEquals(STALE, car.receive(trip).result());
        assertEquals("Beach", car.displayed().destination());
    }

    @Test public void reusedRevisionWithDifferentContentIsRejected() {
        var car = new TripHandoff.Car(trip);
        var changed = new TripHandoff.Trip(1, List.of(), "Airport");
        var phone = new TripHandoff.Phone(changed, false);
        var ack = car.receive(changed);
        assertEquals(CONFLICT, ack.result());
        phone.acknowledge(ack);
        assertFalse(phone.delivered());
        assertEquals(trip, car.displayed());
    }

    @Test public void defensivelyCopiesStopsAndAllowsDirectTrip() {
        var stops = new ArrayList<>(List.of("First"));
        var draft = new TripHandoff.Trip(2, stops, "End");
        stops.clear();
        assertEquals(List.of("First"), draft.stops());
        assertTrue(new TripHandoff.Trip(2, List.of(), "End").stops().isEmpty());
    }

    @Test public void invalidRevisionsAndPlacesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TripHandoff.Trip(0, List.of(), "End"));
        assertThrows(IllegalArgumentException.class, () -> new TripHandoff.Trip(1, List.of(), ""));
        assertThrows(IllegalArgumentException.class, () -> new TripHandoff.Trip(1, List.of(" "), "End"));
        assertThrows(IllegalArgumentException.class, () -> new TripHandoff.Trip(1, null, "End"));
    }

    @Test public void trimsUnicodeWhitespaceAtEdgesAndRejectsBlankPlaces() {
        assertEquals("Coffee shop", TripHandoff.trimPlace("\u2003Coffee shop\u2003"));
        assertEquals("", TripHandoff.trimPlace("\u2003\t"));
        assertThrows(IllegalArgumentException.class,
                () -> new TripHandoff.Trip(1, List.of(), "\u2003"));
    }
}
