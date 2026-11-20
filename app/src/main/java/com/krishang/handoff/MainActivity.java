package com.krishang.handoff;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private TripHandoff.Phone phone;
    private TripHandoff.Car car;
    private TextView phoneStatus;
    private TextView carStatus;
    private EditText destination;
    private String event = "";

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        var initial = new TripHandoff.Trip(1, List.of("Coffee shop", "Charging station"), "Trailhead");
        phone = saved == null ? new TripHandoff.Phone(initial, false)
                : new TripHandoff.Phone(restoreTrip(saved.getBundle("phone")), saved.getBoolean("delivered"));
        car = new TripHandoff.Car(saved == null ? null : restoreTrip(saved.getBundle("car")));
        if (saved != null) event = saved.getString("event", "");

        var scroll = new ScrollView(this);
        var layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            var bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        scroll.addView(layout);
        setContentView(scroll);

        phoneStatus = new TextView(this);
        phoneStatus.setTextSize(20);
        layout.addView(phoneStatus);
        destination = new EditText(this);
        destination.setHint("New destination");
        destination.setSingleLine(true);
        destination.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        if (saved != null) destination.setText(saved.getString("draft", ""));
        layout.addView(destination);
        addButton(layout, "Save new revision", () -> {
            String value = TripHandoff.trimPlace(destination.getText().toString());
            if (value.isEmpty()) {
                destination.setError("Enter a destination");
                return;
            }
            phone.revise(value);
            destination.setText("");
            event = "New revision ready to send.";
            render();
        });
        addButton(layout, "Send and drop acknowledgement", () -> send(true));
        addButton(layout, "Send / Retry", () -> send(false));
        carStatus = new TextView(this);
        carStatus.setTextSize(20);
        layout.addView(carStatus);
        render();
    }

    private void addButton(LinearLayout layout, String text, Runnable action) {
        var button = new Button(this);
        button.setText(text);
        button.setOnClickListener(view -> action.run());
        layout.addView(button);
    }

    private void send(boolean dropAck) {
        var ack = car.receive(phone.trip());
        if (!dropAck) phone.acknowledge(ack);
        event = dropAck ? "Acknowledgement lost. Retry the same revision."
                : switch (ack.result()) {
                    case ACCEPTED -> "Car accepted the trip.";
                    case DUPLICATE -> "Car confirmed the trip it already displayed.";
                    case STALE -> "Car already has a newer revision.";
                    case CONFLICT -> "Car has different content for that revision.";
                };
        render();
    }

    private void render() {
        phoneStatus.setText("Phone · " + (phone.delivered() ? "Delivered" : "Pending")
                + "\n" + describe(phone.trip()) + "\n" + event);
        carStatus.setText("Simulated car display\n"
                + (car.displayed() == null ? "No trip received" : describe(car.displayed())));
    }

    private static String describe(TripHandoff.Trip trip) {
        StringBuilder text = new StringBuilder("Revision ").append(trip.revision());
        for (int i = 0; i < trip.stops().size(); i++) {
            text.append("\nStop ").append(i + 1).append(": ").append(trip.stops().get(i));
        }
        return text.append("\nDestination: ").append(trip.destination()).toString();
    }

    @Override protected void onSaveInstanceState(Bundle saved) {
        super.onSaveInstanceState(saved);
        saved.putBundle("phone", saveTrip(phone.trip()));
        saved.putBundle("car", saveTrip(car.displayed()));
        saved.putBoolean("delivered", phone.delivered());
        saved.putString("event", event);
        saved.putString("draft", destination.getText().toString());
    }

    private static Bundle saveTrip(TripHandoff.Trip trip) {
        if (trip == null) return null;
        var state = new Bundle();
        state.putLong("revision", trip.revision());
        state.putStringArrayList("stops", new ArrayList<>(trip.stops()));
        state.putString("destination", trip.destination());
        return state;
    }

    private static TripHandoff.Trip restoreTrip(Bundle state) {
        if (state == null) return null;
        return new TripHandoff.Trip(state.getLong("revision"), state.getStringArrayList("stops"),
                state.getString("destination"));
    }
}
