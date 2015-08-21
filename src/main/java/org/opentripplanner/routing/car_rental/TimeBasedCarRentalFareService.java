package org.opentripplanner.routing.car_rental;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Currency;
import java.util.List;

/**
 * Created by klemen on 21.8.2015.
 */

public class TimeBasedCarRentalFareService implements FareService, Serializable {
    private static final long serialVersionUID = 7222621661906177942L;

    private static Logger log = LoggerFactory.getLogger(TimeBasedCarRentalFareService.class);

    // Each entry is <max time, cents at that time>; the list is sorted in
    // ascending time order
    private List<P2<Integer>> pricing_by_second;

    private Currency currency;

    protected TimeBasedCarRentalFareService(Currency currency, List<P2<Integer>> pricingBySecond) {
        this.currency = currency;
        this.pricing_by_second = pricingBySecond;
    }

    @Override
    public Fare getCost(GraphPath path) {
        int cost = 0;
        long start = -1;

        for (State state : path.states) {
            if (state.getVertex() instanceof BikeRentalStationVertex
                    && state.getBackState().getVertex() instanceof BikeRentalStationVertex) {
                if (start == -1) {
                    start = state.getTimeSeconds();
                } else {
                    int time_on_bike = (int) (state.getTimeSeconds() - start);
                    int ride_cost = -1;
                    for (P2<Integer> bracket : pricing_by_second) {
                        int time = bracket.first;
                        if (time_on_bike < time) {
                            ride_cost = bracket.second;
                            break;
                        }
                    }
                    if (ride_cost == -1) {
                        log.warn("Car rental has no associated pricing (too long?) : "
                                + time_on_bike + " seconds");
                    } else {
                        cost += ride_cost;
                    }
                    start = -1;
                }
            }
        }

        Fare fare = new Fare();
        fare.addFare(Fare.FareType.regular, new WrappedCurrency(currency), cost);
        return fare;
    }

    @Override
    public List<FareService> getFareServices() {
        return null;
    }
}
