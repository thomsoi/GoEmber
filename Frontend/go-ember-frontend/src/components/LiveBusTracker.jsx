import { useEffect, useRef, useState } from 'react';
import {
    ensureCurrentPassport,
    getLiveBuses,
    recordCompletedJourney,
    recordLocationVisit,
    recordTownVisit,
} from '../services/BackendAPI';
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

function findJourneyEndpointStops(route, origin, destination) {
    const normalize = value => value.trim().replace(/\s+/g, ' ').toLowerCase();
    const matchesLocation = (stop, query) => {
        const normalizedQuery = normalize(query);
        return [stop.name, stop.regionName]
            .some(name => name && normalize(name).includes(normalizedQuery));
    };
    const originIndex = route.findIndex(stop => matchesLocation(stop, origin));
    const destinationIndex = route.findIndex(
        (stop, index) => index > originIndex && matchesLocation(stop, destination),
    );

    if (originIndex < 0 || destinationIndex < 0) {
        return { endpointStops: [], towns: [] };
    }

    const routeStops = route.slice(originIndex, destinationIndex + 1);
    const towns = new Set([origin.trim().toLowerCase(), destination.trim().toLowerCase()]);
    routeStops.forEach(stop => {
        const town = stop.regionName?.trim();
        if (town) towns.add(town.toLowerCase());
    });

    return {
        endpointStops: [
            { ...route[originIndex], name: origin.trim() },
            { ...route[destinationIndex], name: destination.trim() },
        ],
        towns: [...towns],
    };
}

function uniqueTownNames(towns) {
    const uniqueTowns = new Map();
    towns.forEach(town => {
        const trimmedTown = town.trim();
        const normalizedTown = trimmedTown.toLowerCase();
        if (trimmedTown && !uniqueTowns.has(normalizedTown)) {
            uniqueTowns.set(normalizedTown, trimmedTown);
        }
    });
    return [...uniqueTowns.values()];
}

function LiveBusTracker({ origin, destination, onSelectedBusChange }) {
    const [buses, setBuses] = useState([]);
    const [selectedVehicleId, setSelectedVehicleId] = useState('');
    const [loading, setLoading] = useState(true);
    const [feedback, setFeedback] = useState(null);
    const [isTracking, setIsTracking] = useState(false);
    const [pendingStops, setPendingStops] = useState([]);
    const [pendingJourneys, setPendingJourneys] = useState([]);
    const [isAddingStamps, setIsAddingStamps] = useState(false);
    const selectedVehicleIdRef = useRef('');
    const trackingCursorRef = useRef(null);
    const isTrackingRef = useRef(false);
    const pollRef = useRef(null);
    const journeyCounterRef = useRef(0);

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
                onSelectedBusChange?.(selectedBus ?? null);
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
                        processedStopIds: new Set(),
                        journeyId: cursor?.journeyId ?? ++journeyCounterRef.current,
                        endpointStops: cursor?.endpointStops ?? [],
                        journey: cursor?.journey ?? null,
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
                const unrecordedPassedStops = passedStops.filter(
                    stop => !cursor.processedStopIds.has(stop.locationId),
                );

                if (unrecordedPassedStops.length === 0) {
                    trackingCursorRef.current = {
                        tripKey,
                        currentStopId: currentStop?.locationId ?? cursor.currentStopId,
                        nextStop,
                        processedStopIds: cursor.processedStopIds,
                        journeyId: cursor.journeyId,
                        endpointStops: cursor.endpointStops,
                        journey: cursor.journey,
                    };
                    return;
                }

                if (!isTrackingRef.current) {
                    trackingCursorRef.current = {
                        tripKey,
                        currentStopId: currentStop?.locationId ?? cursor.currentStopId,
                        nextStop,
                        processedStopIds: cursor.processedStopIds,
                        journeyId: cursor.journeyId,
                        endpointStops: cursor.endpointStops,
                        journey: cursor.journey,
                    };
                    return;
                }

                for (const stop of unrecordedPassedStops) {
                    cursor.processedStopIds.add(stop.locationId);
                }

                setPendingStops(pending => {
                    const queuedLocationIds = new Set(pending.map(stop => stop.locationId));
                    const newStops = unrecordedPassedStops.reduce((stops, stop) => {
                        if (queuedLocationIds.has(stop.locationId)) return stops;

                        queuedLocationIds.add(stop.locationId);
                        stops.push({
                            ...stop,
                            queueId: `${tripKey}:${cursor.journeyId}:${stop.locationId}`,
                        });
                        return stops;
                    }, []);
                    return [...pending, ...newStops];
                });
                trackingCursorRef.current = {
                    tripKey,
                    currentStopId: currentStop?.locationId ?? trackingCursorRef.current.currentStopId,
                    nextStop,
                    processedStopIds: cursor.processedStopIds,
                    journeyId: cursor.journeyId,
                    endpointStops: cursor.endpointStops,
                    journey: cursor.journey,
                };
                if (active) {
                    setFeedback({
                        type: 'status',
                        text: `${unrecordedPassedStops.length} passed ${unrecordedPassedStops.length === 1 ? 'stop is' : 'stops are'} ready to add to your passport.`,
                    });
                }
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
    }, [origin, destination, onSelectedBusChange]);

    function selectBus(event) {
        const vehicleId = event.target.value;
        selectedVehicleIdRef.current = vehicleId;
        trackingCursorRef.current = null;
        isTrackingRef.current = false;
        setIsTracking(false);
        setSelectedVehicleId(vehicleId);
        onSelectedBusChange?.(buses.find(bus => String(bus.vehicleId) === vehicleId) ?? null);
        setFeedback(vehicleId ? { type: 'status', text: 'Waiting for this bus’s live stop.' } : null);
        pollRef.current?.();
    }

    function toggleTracking() {
        if (!selectedBus) return;

        if (isTrackingRef.current) {
            isTrackingRef.current = false;
            setIsTracking(false);
            const cursor = trackingCursorRef.current;
            if (cursor.journey) {
                const { requestKey, ...journey } = cursor.journey;
                setPendingJourneys(journeys => [
                    ...journeys,
                    { ...journey, journeyKey: requestKey },
                ]);
            }
            setFeedback({
                type: 'status',
                text: 'Ride ended. Add the origin, destination and passed stops to your passport when you are ready.',
            });
            return;
        }

        const tripKey = `${selectedBus.vehicleId}:${selectedBus.tripUid}`;
        const journeyDetails = findJourneyEndpointStops(selectedBus.route ?? [], origin, destination);
        const requestKey = crypto.randomUUID();
        trackingCursorRef.current = {
            tripKey,
            currentStopId: selectedBus.currentStop?.locationId ?? null,
            nextStop: selectedBus.nextStop,
            processedStopIds: new Set(),
            journeyId: ++journeyCounterRef.current,
            endpointStops: journeyDetails.endpointStops,
            journey: {
                requestKey,
                originLocationId: journeyDetails.endpointStops[0]?.locationId,
                destinationLocationId: journeyDetails.endpointStops[1]?.locationId,
                originName: origin,
                destinationName: destination,
                townsToStamp: uniqueTownNames([origin, destination]),
                towns: journeyDetails.towns,
            },
        };
        isTrackingRef.current = true;
        setIsTracking(true);
        setFeedback({
            type: 'status',
            text: `Tracking Bus ${selectedBus.vehicleId} from ${origin} to ${destination}.`,
        });
        pollRef.current?.();
    }

    async function addStampsToPassport() {
        if (isAddingStamps || isTracking || (pendingStops.length === 0 && pendingJourneys.length === 0)) return;

        setIsAddingStamps(true);
        let addedCount = 0;
        let recordingJourneys = false;

        try {
            const { userId } = await ensureCurrentPassport();
            const uniqueStops = [...new Map(
                pendingStops.map(stop => [stop.locationId, stop]),
            ).values()];

            for (const stop of uniqueStops) {
                await recordLocationVisit(userId, stop.locationId, stop.name);
                setPendingStops(current => current.filter(item => item.locationId !== stop.locationId));
                addedCount++;
            }

            recordingJourneys = true;
            for (const journey of pendingJourneys) {
                const pendingTownNames = journey.townsToStamp ?? [];
                for (const townName of pendingTownNames) {
                    await recordTownVisit(userId, townName);
                    setPendingJourneys(current => current.map(item => (
                        item.journeyKey === journey.journeyKey
                            ? {
                                ...item,
                                townsToStamp: item.townsToStamp.filter(town => town !== townName),
                            }
                            : item
                    )));
                }
                const { journeyKey, ...details } = journey;
                delete details.townsToStamp;
                await recordCompletedJourney(userId, { ...details, journeyKey });
                setPendingJourneys(current => current.filter(item => item.journeyKey !== journeyKey));
            }

            setFeedback({
                type: 'success',
                text: `Added ${addedCount} ${addedCount === 1 ? 'stop stamp' : 'stop stamps'} and ${pendingJourneys.length} ${pendingJourneys.length === 1 ? 'route' : 'routes'} to your passport.`,
            });
        } catch (error) {
            setFeedback({
                type: 'error',
                text: recordingJourneys
                    ? `Stop stamps were added, but route statistics could not be saved: ${error.message || 'Please try again.'}`
                    : addedCount > 0
                    ? `Added stamps for ${addedCount} stops, but could not add the rest: ${error.message || 'Please try again.'}`
                    : error.message || 'Could not add stamps to your passport.',
            });
        } finally {
            setIsAddingStamps(false);
        }
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
            {!isTracking && (pendingStops.length > 0 || pendingJourneys.length > 0) && (
                <button
                    className="live-bus-add-stamps-button"
                    type="button"
                    onClick={addStampsToPassport}
                    disabled={isAddingStamps}
                >
                    {isAddingStamps ? 'Adding stamps…' : `Add stamps to passport (${pendingStops.length + pendingJourneys.length})`}
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