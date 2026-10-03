import { useEffect, useRef, useState } from 'react';
import { ensureCurrentPassport, getLiveBuses, recordLocationVisit } from '../services/BackendAPI';
import '../css/LiveBusTracker.css';

const POLL_INTERVAL_MS = 15000;

function stopsPassedSince(previousStopId, currentStop, route) {
    const previousIndex = route.findIndex(stop => stop.locationId === previousStopId);
    const currentIndex = route.findIndex(stop => stop.locationId === currentStop.locationId);

    if (previousIndex >= 0 && currentIndex > previousIndex) {
        return route.slice(previousIndex + 1, currentIndex + 1);
    }

    return [currentStop];
}

function LiveBusTracker() {
    const [buses, setBuses] = useState([]);
    const [selectedVehicleId, setSelectedVehicleId] = useState('');
    const [loading, setLoading] = useState(true);
    const [feedback, setFeedback] = useState(null);
    const selectedVehicleIdRef = useRef('');
    const trackingCursorRef = useRef(null);
    const pollRef = useRef(null);
    const pollingRef = useRef(false);

    useEffect(() => {
        let active = true;

        async function pollLiveBuses() {
            if (pollingRef.current) return;
            pollingRef.current = true;

            try {
                const liveBuses = await getLiveBuses();
                if (!active) return;
                setBuses(liveBuses ?? []);
                setLoading(false);

                const selectedBus = liveBuses?.find(
                    bus => String(bus.vehicleId) === selectedVehicleIdRef.current,
                );
                if (!selectedBus || (!selectedBus.currentStop && !selectedBus.nextStop)) return;

                const currentStop = selectedBus.currentStop;
                const nextStop = selectedBus.nextStop;
                const tripKey = `${selectedBus.vehicleId}:${selectedBus.tripUid}`;
                const cursor = trackingCursorRef.current;

                if (!cursor || cursor.tripKey !== tripKey) {
                    trackingCursorRef.current = {
                        tripKey,
                        currentStopId: currentStop?.locationId ?? null,
                        nextStop,
                    };
                    setFeedback({
                        type: 'status',
                        text: currentStop
                            ? `Tracking ${selectedBus.routeNumber || 'bus route'} from ${currentStop.name}.`
                            : `Tracking ${selectedBus.routeNumber || 'bus route'}; next stop is ${nextStop?.name ?? 'unavailable'}.`,
                    });
                    return;
                }

                const currentStopChanged = currentStop
                    && cursor.currentStopId !== null
                    && cursor.currentStopId !== currentStop.locationId;
                const nextStopChanged = cursor.nextStop
                    && nextStop
                    && cursor.nextStop.locationId !== nextStop.locationId;
                const passedStops = currentStopChanged
                    ? stopsPassedSince(cursor.currentStopId, currentStop, selectedBus.route ?? [])
                    : nextStopChanged
                        ? [cursor.nextStop]
                        : [];

                if (passedStops.length === 0) {
                    trackingCursorRef.current = {
                        tripKey,
                        currentStopId: currentStop?.locationId ?? cursor.currentStopId,
                        nextStop,
                    };
                    return;
                }

                const { userId } = await ensureCurrentPassport();

                for (const stop of passedStops) {
                    const visit = await recordLocationVisit(userId, stop.locationId);
                    trackingCursorRef.current = currentStopChanged
                        ? { tripKey, currentStopId: stop.locationId, nextStop: cursor.nextStop }
                        : { tripKey, currentStopId: cursor.currentStopId, nextStop };
                    if (active) {
                        setFeedback({
                            type: 'success',
                            text: visit.alreadyVisited
                                ? `Already visited ${visit.stampName} · ${visit.tier} · ${visit.visitCount} visits.`
                                : `Stamp collected: ${visit.stampName} · ${visit.tier}.`,
                        });
                    }
                }

                trackingCursorRef.current = {
                    tripKey,
                    currentStopId: currentStop?.locationId ?? trackingCursorRef.current.currentStopId,
                    nextStop,
                };
            } catch (error) {
                if (active) {
                    setLoading(false);
                    setFeedback({ type: 'error', text: error.message || 'Could not update live buses.' });
                }
            } finally {
                pollingRef.current = false;
            }
        }

        pollRef.current = pollLiveBuses;
        pollLiveBuses();
        const intervalId = window.setInterval(pollLiveBuses, POLL_INTERVAL_MS);

        return () => {
            active = false;
            window.clearInterval(intervalId);
            pollRef.current = null;
        };
    }, []);

    function selectBus(event) {
        const vehicleId = event.target.value;
        selectedVehicleIdRef.current = vehicleId;
        trackingCursorRef.current = null;
        setSelectedVehicleId(vehicleId);
        setFeedback(vehicleId ? { type: 'status', text: 'Waiting for this bus’s live stop.' } : null);
        pollRef.current?.();
    }

    const selectedBus = buses.find(bus => String(bus.vehicleId) === selectedVehicleId);

    return (
        <section className="live-bus-tracker" aria-labelledby="live-bus-title">
            <div className="live-bus-heading">
                <p className="journey-eyebrow">EMBER LIVE</p>
                <h2 id="live-bus-title">Track a bus</h2>
            </div>

            <label className="live-bus-select-label" htmlFor="live-bus-select">Live buses</label>
            <select
                id="live-bus-select"
                className="live-bus-select"
                value={selectedVehicleId}
                onChange={selectBus}
                disabled={loading || buses.length === 0}
            >
                <option value="">{loading ? 'Loading buses…' : 'Choose a bus'}</option>
                {buses.map(bus => (
                    <option key={bus.vehicleId} value={bus.vehicleId}>
                        {`Bus ${bus.vehicleId}${bus.plateNumber ? ` · ${bus.plateNumber}` : ''}${bus.routeNumber ? ` · Route ${bus.routeNumber}` : ''}`}
                    </option>
                ))}
            </select>

            {selectedBus && (
                <p className="live-bus-stops">
                    {selectedBus.currentStop?.name || 'Current stop unavailable'}
                    {selectedBus.nextStop?.name ? ` → ${selectedBus.nextStop.name}` : ''}
                </p>
            )}
            {feedback && (
                <p className={`live-bus-feedback is-${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>
                    {feedback.text}
                </p>
            )}
            {!loading && buses.length === 0 && !feedback && (
                <p className="live-bus-feedback">No active Ember buses are available right now.</p>
            )}
        </section>
    );
}

export default LiveBusTracker;