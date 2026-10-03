import { useState } from 'react';
import DisplayRoute from '../components/DisplayRoute';
import JourneyInfoRq from '../components/JourneyInfoRq';
import JourneyStats from '../components/JourneyStats';
import LiveBusTracker from '../components/LiveBusTracker';
import '../css/RouteTracker.css';

function addMinutes(time, minutesToAdd) {
    const [hours, minutes] = time.split(':').map(Number);
    const totalMinutes = (hours * 60 + minutes + minutesToAdd) % (24 * 60);
    const adjustedHours = Math.floor(totalMinutes / 60).toString().padStart(2, '0');
    const adjustedMinutes = (totalMinutes % 60).toString().padStart(2, '0');
    return `${adjustedHours}:${adjustedMinutes}`;
}

function RouteTracker() {
    const [activeJourney, setActiveJourney] = useState(null);
    const [journeyStats, setJourneyStats] = useState(null);

    function startJourney({ startLocation, endLocation, departureTime }) {
        setJourneyStats(null);
        setActiveJourney({
            startLocation,
            endLocation,
            departureTime,
            startedAt: Date.now(),
            stops: [
                {
                    id: 'origin',
                    name: startLocation,
                    location: 'Journey starting point',
                    town: startLocation,
                    arrivalTime: departureTime,
                    visited: true,
                    stampCollected: true,
                    distanceFromPreviousKm: 0,
                },
                {
                    id: 'cedar-square',
                    name: 'Cedar Square',
                    location: 'Sample stop · Central district',
                    town: 'Cedar Square',
                    arrivalTime: addMinutes(departureTime, 14),
                    visited: false,
                    stampCollected: false,
                    distanceFromPreviousKm: 4.2,
                },
                {
                    id: 'riverside-exchange',
                    name: 'Riverside Exchange',
                    location: 'Sample stop · Riverside',
                    town: 'Riverside',
                    arrivalTime: addMinutes(departureTime, 27),
                    visited: false,
                    stampCollected: false,
                    distanceFromPreviousKm: 3.1,
                },
                {
                    id: 'destination',
                    name: endLocation,
                    location: 'Journey destination',
                    town: endLocation,
                    arrivalTime: addMinutes(departureTime, 41),
                    visited: false,
                    stampCollected: false,
                    distanceFromPreviousKm: 4.5,
                },
            ],
        });
    }

    function endJourney() {
        if (!activeJourney) return;

        const visitedStops = activeJourney.stops.filter(stop => stop.visited);
        setJourneyStats({
            distanceKm: visitedStops.reduce((total, stop) => total + stop.distanceFromPreviousKm, 0),
            stampsCollected: visitedStops.filter(stop => stop.stampCollected).length,
            townsVisited: new Set(visitedStops.map(stop => stop.town)).size,
            durationMinutes: Math.max(1, Math.floor((Date.now() - activeJourney.startedAt) / 60000)),
        });
        setActiveJourney(null);
    }

    return (
        <section className="route-tracker" aria-label="Route tracker">
            <LiveBusTracker />
            {activeJourney ? (
                <>
                    <DisplayRoute
                        startLocation={activeJourney.startLocation}
                        endLocation={activeJourney.endLocation}
                        departureTime={activeJourney.departureTime}
                        stops={activeJourney.stops}
                    />
                    <div className="route-tracker-stopbar">
                        <button className="route-end-button" type="button" onClick={endJourney}>
                            End journey
                        </button>
                    </div>
                </>
            ) : (
                <JourneyInfoRq onStartJourney={startJourney} />
            )}
            {journeyStats && <JourneyStats stats={journeyStats} />}
        </section>
    );
}

export default RouteTracker;
