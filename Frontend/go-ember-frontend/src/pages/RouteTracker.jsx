import { useState } from 'react';
import JourneyInfoRq from '../components/JourneyInfoRq';
import LiveBusTracker from '../components/LiveBusTracker';
import '../css/RouteTracker.css';

function RouteTracker() {
    const [routeRequest, setRouteRequest] = useState(null);

    return (
        <section className="route-tracker" aria-label="Route tracker">
            {routeRequest ? (
                <>
                    <header className="route-tracker-route-summary">
                        <p className="journey-eyebrow">LIVE BUS SEARCH</p>
                        <h1>{routeRequest.startLocation} to {routeRequest.endLocation}</h1>
                        <button className="route-change-button" type="button" onClick={() => setRouteRequest(null)}>
                            Change route
                        </button>
                    </header>
                    <LiveBusTracker
                        key={`${routeRequest.startLocation}-${routeRequest.endLocation}`}
                        origin={routeRequest.startLocation}
                        destination={routeRequest.endLocation}
                    />
                </>
            ) : (
                <JourneyInfoRq onStartJourney={setRouteRequest} />
            )}
        </section>
    );
}

export default RouteTracker;
