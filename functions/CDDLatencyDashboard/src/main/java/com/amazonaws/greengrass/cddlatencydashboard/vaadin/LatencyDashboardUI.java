package com.amazonaws.greengrass.cddlatencydashboard.vaadin;

import com.amazonaws.greengrass.cddlatencydashboard.events.ImmutableMessageFromCloudEvent;
import com.amazonaws.greengrass.cddlatencydashboard.events.Latencies;
import com.amazonaws.greengrass.cddlatencydashboard.events.TimerFiredEvent;
import com.amazonaws.greengrass.cddlatencydashboard.helpers.JsonHelper;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Grid;
import com.vaadin.ui.UIDetachedException;
import com.vaadin.ui.VerticalLayout;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * This UI is the application entry point. A UI may either represent a browser window
 * (or tab) or some part of an HTML page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Push(transport = Transport.LONG_POLLING)
@Theme("valo")
public class LatencyDashboardUI extends DaggerUI {
    public static final String PATTERN = "/*";
    private final Grid<Latencies> latencyGrid = new Grid<>();
    private final List<Latencies> latencyList = new ArrayList<>();
    private final JsonHelper jsonHelper = new JsonHelper();

    private final VerticalLayout verticalLayout = new VerticalLayout();

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        // Make sure we get events from the dispatcher
        dispatcher.add(ImmutableMessageFromCloudEvent.class, this::messageFromCloud);
        dispatcher.add(TimerFiredEvent.class, this::timerFiredEvent);

        String title = Optional.ofNullable(System.getenv("TITLE")).orElse("Data grid");
        latencyGrid.setWidth("100%");
        latencyGrid.setCaption(title);

        verticalLayout.addComponentsAndExpand(latencyGrid);

        setContent(verticalLayout);
    }

    public void messageFromCloud(ImmutableMessageFromCloudEvent immutableMessageFromCloudEvent) {
        try {
            access(() -> {
                Latencies latencies = jsonHelper.fromJson(Latencies.class, immutableMessageFromCloudEvent.getMessage().getBytes());
                latencyList.add(latencies);
                latencyGrid.setItems(latencyList);

                if (latencyGrid.getColumns().size() == 0) {
                    latencyGrid.removeAllColumns();
                    IntStream.range(0, latencies.getLatencies().size())
                            .forEachOrdered(index -> latencyGrid.addColumn(rowValue -> rowValue.getLatencies().get(index).getValue())
                                    .setCaption(latencies.getLatencies().get(index).getName()));
                }

                latencyGrid.scrollToEnd();
            });
        } catch (UIDetachedException e) {
            // Client probably disconnected, ignore
        } catch (Exception e) {
            // Do not throw exceptions in event bus subscriber methods
        }
    }

    public void timerFiredEvent(TimerFiredEvent timerFiredEvent) {
        try {
            access(() -> {
            });
        } catch (UIDetachedException e) {
            // Client probably disconnected, ignore
        } catch (Exception e) {
            // Do not throw exceptions in event bus subscriber methods
        }
    }

    @WebServlet(asyncSupported = true)
    @VaadinServletConfiguration(ui = LatencyDashboardUI.class, productionMode = false)
    public static class SkeletonUIServlet extends VaadinDaggerServlet {
    }
}
