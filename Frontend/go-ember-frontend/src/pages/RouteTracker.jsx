import { useCallback, useRef, useState } from 'react';
import { emptyTracker, loadTracker, saveTracker } from '../services/trackerStore';
import DisplayRoute from '../components/DisplayRoute';
import JourneyInfoRq from '../components/JourneyInfoRq';
import LiveBusTracker from '../components/LiveBusTracker';
import { getRouteStops } from '../services/ride';
import '../css/RouteTracker.css';

function RouteTracker() {
    const [tracker, setTracker] = useState(() => {
        try { return loadTracker(localStorage); }
        catch (error) { return { ...emptyTracker(), loadError: error.message }; }
    });
    const trackerRef = useRef(tracker);
    const [storageError, setStorageError] = useState('');
    const [busy, setBusy] = useState(false);
    const [selectedBus, setSelectedBus] = useState(null);
    const updateTracker = useCallback(change => {
        const next = change(trackerRef.current);
        try { saveTracker(localStorage, next); }
        catch { throw new Error('Could not save ride data on this device. Check browser storage and try again.'); }
        trackerRef.current = next;
        setTracker(next);
    }, []);
    const routeRequest = tracker.routeRequest;
    const displayedBus = selectedBus?.routeDetailsLoaded ? selectedBus : tracker.ride
        ? { vehicleId: tracker.ride.vehicleId, tripUid: tracker.ride.tripUid,
            route: tracker.ride.route, routeNumber: selectedBus?.routeNumber,
            currentStop: tracker.ride.route[tracker.ride.reachedIndex] }
        : null;

    function changeRoute(request) {
        try {
            updateTracker(current => ({ ...current, routeRequest: request }));
            setSelectedBus(null);
            setStorageError('');
        } catch (error) { setStorageError(error.message); }
    }

    return (
        <section className="route-tracker" aria-label="Route tracker">
            {storageError && <p role="alert">{storageError}</p>}
            {tracker.loadError && <p role="alert">{tracker.loadError} Discard saved data to continue.</p>}
            {(tracker.loadError || tracker.stops.length > 0 || tracker.journeys.length > 0) && !tracker.ride &&
                <button type="button" disabled={busy} onClick={() => {
                    if (!window.confirm('Discard all unsaved ride awards?')) return;
                    try { updateTracker(() => emptyTracker()); }
                    catch (error) { setStorageError(error.message); }
                }}>Discard unsaved awards</button>}
            {!tracker.loadError && <>
            {routeRequest ? (
                <>
                    <header className="route-tracker-route-summary">
                        <p className="journey-eyebrow">LIVE BUS SEARCH</p>
                        <h1>{routeRequest.startLocation && routeRequest.endLocation
                            ? `${routeRequest.startLocation} to ${routeRequest.endLocation}`
                            : routeRequest.startLocation ? `Departures from ${routeRequest.startLocation}`
                                : routeRequest.endLocation ? `Buses to ${routeRequest.endLocation}` : 'Soonest departures'}</h1>
                        <button className="route-change-button" type="button" disabled={Boolean(tracker.ride) || busy} onClick={() => changeRoute(null)}>
                            Change route
                        </button>
                    </header>
                </>
            ) : (
                <JourneyInfoRq onStartJourney={changeRoute} disabled={Boolean(tracker.ride) || busy} />
            )}
            <LiveBusTracker
                key={`${routeRequest?.startLocation ?? ''}-${routeRequest?.endLocation ?? ''}`}
                origin={routeRequest?.startLocation ?? ''}
                destination={routeRequest?.endLocation ?? ''}
                tracker={tracker}
                updateTracker={updateTracker}
                onBusyChange={setBusy}
                onSelectedBusChange={setSelectedBus}
            />
            {displayedBus?.route?.length > 0 && (
                <DisplayRoute
                    startLocation={routeRequest?.startLocation || displayedBus.departureStop?.name || displayedBus.route[0].name}
                    endLocation={routeRequest?.endLocation || displayedBus.route.at(-1).name}
                    routeNumber={displayedBus.routeNumber}
                    stops={getRouteStops(displayedBus, selectedBus?.routeDetailsLoaded ? routeRequest?.startLocation : '',
                        selectedBus?.routeDetailsLoaded ? routeRequest?.endLocation : '', tracker.ride)}
                />
            )}
            </>}
        </section>
    );
}

export default RouteTracker;
