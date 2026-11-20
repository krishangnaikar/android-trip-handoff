package com.krishang.handoff;

import java.util.List;
import java.util.Objects;

final class TripHandoff {
    record Trip(long revision, List<String> stops, String destination) {
        Trip {
            if (revision < 1 || destination == null || destination.isBlank()) {
                throw new IllegalArgumentException("Trip needs a revision and destination");
            }
            if (stops == null || stops.stream().anyMatch(stop -> stop == null || stop.isBlank())) {
                throw new IllegalArgumentException("Stops cannot be blank");
            }
            stops = List.copyOf(stops);
        }
    }

    enum Result { ACCEPTED, DUPLICATE, STALE, CONFLICT }

    record Ack(long revision, Result result) {
        boolean confirmsDelivery() {
            return result == Result.ACCEPTED || result == Result.DUPLICATE;
        }
    }

    static final class Car {
        private Trip displayed;

        Car(Trip displayed) { this.displayed = displayed; }
        Trip displayed() { return displayed; }

        Ack receive(Trip trip) {
            long revision = trip.revision();
            if (displayed != null && revision <= displayed.revision()) {
                if (revision < displayed.revision()) return new Ack(revision, Result.STALE);
                return new Ack(revision, displayed.equals(trip) ? Result.DUPLICATE : Result.CONFLICT);
            }
            displayed = trip;
            return new Ack(revision, Result.ACCEPTED);
        }
    }

    static final class Phone {
        private Trip trip;
        private boolean delivered;

        Phone(Trip trip, boolean delivered) {
            this.trip = Objects.requireNonNull(trip);
            this.delivered = delivered;
        }

        Trip trip() { return trip; }
        boolean delivered() { return delivered; }

        void revise(String destination) {
            trip = new Trip(Math.incrementExact(trip.revision()), trip.stops(), destination);
            delivered = false;
        }

        void acknowledge(Ack ack) {
            if (ack.confirmsDelivery() && ack.revision() == trip.revision()) delivered = true;
        }
    }
}
