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

function LiveBusTracker({ origin, destination }) {
    const [buses, setBuses] = useState([]);
    const [selectedVehicleId, setSelectedVehicleId] = useState('');
    const [loading, setLoading] = useState(true);
    const [feedback, setFeedback] = useState(null);
    const [isTracking, setIsTracking] = useState(false);
    const selectedVehicleIdRef = useRef('');
    const trackingCursorRef = useRef(null);
    const isTrackingRef = useRef(false);
    const pollRef = useRef(null);

    useEffect(() => {
        let active = true;
        let polling = false;

        async function pollLiveBuses() {
            if (polling) return;
            polling = true;

            try {
                const liveBuses = await getLiveBuses(origin, destination);
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
                    if (isTrackingRef.current) {
                        setFeedback({
                            type: 'status',
                            text: `Tracking Bus ${selectedBus.vehicleId} from ${origin} to ${destination}.`,
                        });
                    }
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

                if (!isTrackingRef.current) {
                    trackingCursorRef.current = {
                        tripKey,
                        currentStopId: currentStop?.locationId ?? cursor.currentStopId,
                        nextStop,
                    };
                    return;
                }

                const { userId } = await ensureCurrentPassport();

                for (const stop of passedStops) {
                    const visit = await recordLocationVisit(userId, stop.locationId, stop.name);
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
                polling = false;
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
    }, [origin, destination]);

    function selectBus(event) {
        const vehicleId = event.target.value;
        selectedVehicleIdRef.current = vehicleId;
        trackingCursorRef.current = null;
        isTrackingRef.current = false;
        setIsTracking(false);
        setSelectedVehicleId(vehicleId);
        setFeedback(vehicleId ? { type: 'status', text: 'Waiting for this bus’s live stop.' } : null);
        pollRef.current?.();
    }

    function toggleTracking() {
        if (!selectedBus) return;

        if (isTrackingRef.current) {
            isTrackingRef.current = false;
            setIsTracking(false);
            setFeedback({ type: 'status', text: 'Ride ended. Stamps from passed stops are saved.' });
            return;
        }

        trackingCursorRef.current = {
            tripKey: `${selectedBus.vehicleId}:${selectedBus.tripUid}`,
            currentStopId: selectedBus.currentStop?.locationId ?? null,
            nextStop: selectedBus.nextStop,
        };
        isTrackingRef.current = true;
        setIsTracking(true);
        setFeedback({
            type: 'status',
            text: `Tracking Bus ${selectedBus.vehicleId} from ${origin} to ${destination}.`,
        });
        pollRef.current?.();
    }

    const selectedBus = buses.find(bus => String(bus.vehicleId) === selectedVehicleId);

    return (
        <section className="live-bus-tracker" aria-labelledby="live-bus-title">
            <div className="live-bus-heading">
                <p className="journey-eyebrow">EMBER LIVE</p>
                <h2 id="live-bus-title">Buses serving this route</h2>
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
                        {`Bus ${bus.vehicleId} · ${origin} → ${destination}`}
                    </option>
                ))}
            </select>

            {selectedBus && (
                <p className="live-bus-stops">
                    {selectedBus.currentStop?.name || 'Current stop unavailable'}
                    {selectedBus.nextStop?.name ? ` → ${selectedBus.nextStop.name}` : ''}
                </p>
            )}
            {selectedBus && (
                <button
                    className="live-bus-track-button"
                    type="button"
                    onClick={toggleTracking}
                >
                    {isTracking ? 'I got off this bus' : "I'm getting on this bus"}
                </button>
            )}
            {feedback && (
                <p className={`live-bus-feedback is-${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>
                    {feedback.text}
                </p>
            )}
            {!loading && buses.length === 0 && !feedback && (
                <p className="live-bus-feedback">No active Ember buses serve this route right now.</p>
            )}
        </section>
    );
}

export default LiveBusTracker;