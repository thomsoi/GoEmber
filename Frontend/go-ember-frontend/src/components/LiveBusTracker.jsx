import { useEffect, useRef, useState } from 'react';
import { ensureCurrentPassport, currentGuestToken, getLiveBuses } from '../services/BackendAPI';
import { startRide, advanceRide, disconnectRide } from '../services/ride';
import { busKey, busLabel, departureLabel } from '../services/buses';
import { syncPendingAwards } from '../services/awardSync';
import { reassignQueuedAwards } from '../services/trackerStore';
import '../css/LiveBusTracker.css';

const POLL_INTERVAL_MS = 15000;

function LiveBusTracker({ origin, destination, tracker, updateTracker, onSelectedBusChange, onBusyChange }) {
    const [buses, setBuses] = useState([]);
    const [selectedVehicleId, setSelectedVehicleId] = useState(tracker.ride ? busKey(tracker.ride) : '');
    const [loading, setLoading] = useState(true);
    const [feedback, setFeedback] = useState(null);
    const [busy, setBusy] = useState(false);
    const busyRef = useRef(false);
    const trackerRef = useRef(tracker);
    const selectedRef = useRef(selectedVehicleId);
    useEffect(() => {
        trackerRef.current = tracker;
        selectedRef.current = selectedVehicleId;
    }, [tracker, selectedVehicleId]);
    useEffect(() => {
        onBusyChange?.(busy);
        return () => onBusyChange?.(false);
    }, [busy, onBusyChange]);

    useEffect(() => {
        let active = true;
        let polling = false;
        async function poll() {
            if (polling) return;
            polling = true;
            try {
                const detailsTripUid = selectedRef.current.includes(':') ? selectedRef.current.split(':').slice(1).join(':') : '';
                const liveBuses = await getLiveBuses(origin, destination, trackerRef.current.ride?.tripUid, detailsTripUid);
                if (!active) return;
                setBuses(liveBuses);
                setFeedback(previous => previous?.source === 'feed' ? null : previous);
                const selected = liveBuses.find(bus => busKey(bus) === selectedRef.current);
                onSelectedBusChange?.(selected ?? null);
                const ride = trackerRef.current.ride;
                if (ride) {
                    if (!selected || selected.tripUid !== ride.tripUid) {
                        setFeedback({ source: 'feed', type: 'status',
                            text: 'This ride is no longer in the live feed. You can still end it and save observed stops.' });
                    } else {
                        updateTracker(current => ({ ...current,
                            ride: current.ride ? advanceRide(current.ride, selected) : null }));
                    }
                }
            } catch (error) {
                if (active) setFeedback({ source: 'feed', type: 'error', text: error.message || 'Could not update live buses.' });
            } finally {
                polling = false;
                if (active) setLoading(false);
            }
        }
        poll();
        const interval = window.setInterval(poll, POLL_INTERVAL_MS);
        return () => { active = false; window.clearInterval(interval); };
    }, [origin, destination, selectedVehicleId, tracker.ride?.tripUid, updateTracker, onSelectedBusChange]);

    const selectedBus = buses.find(bus => busKey(bus) === selectedVehicleId);

    async function toggleTracking() {
        if (busyRef.current) return;
        try {
            if (trackerRef.current.ride) {
                busyRef.current = true;
                setBusy(true);
                let latest = null;
                let warning = '';
                try {
                    const ride = trackerRef.current.ride;
                    const liveBuses = await getLiveBuses(origin, destination, ride.tripUid, ride.tripUid);
                    latest = liveBuses.find(bus => String(bus.vehicleId) === ride.vehicleId && bus.tripUid === ride.tripUid) ?? null;
                    if (!latest) {
                        warning = 'The bus left the live feed. Only previously observed stops were counted.';
                    }
                } catch {
                    warning = 'Could not refresh the live position. Only previously observed stops were counted.';
                }
                let completed;
                updateTracker(current => {
                    completed = disconnectRide(current, latest);
                    return completed;
                });
                trackerRef.current = completed;
                await savePendingAwards(warning);
                return;
            }
            if (!selectedBus) return;
            busyRef.current = true;
            setBusy(true);
            await ensureCurrentPassport();
            const ownerToken = currentGuestToken();
            if (trackerRef.current.ownerToken && trackerRef.current.ownerToken !== ownerToken
                && (trackerRef.current.stops.length || trackerRef.current.journeys.length)) {
                throw new Error('These unsaved awards belong to an expired passport. Discard them before starting a new ride.');
            }
            const ride = startRide(selectedBus, origin, destination, crypto.randomUUID());
            updateTracker(current => ({ ...current, ownerToken, ride }));
            setFeedback({ type: 'status', text: 'Tracking your ride. Only observed stops will be recorded.' });
        } catch (error) {
            setFeedback({ type: 'error', text: error.message });
        } finally {
            busyRef.current = false;
            setBusy(false);
        }
    }

    async function savePendingAwards(warning = '') {
        try {
            const { userId } = await ensureCurrentPassport();
            if (trackerRef.current.ownerToken !== currentGuestToken()) {
                if (!window.confirm('These unsaved awards were started on another guest passport. Move them to your current passport?')) {
                    throw new Error('Unsaved awards still belong to the previous guest passport.');
                }
                updateTracker(current => reassignQueuedAwards(current, currentGuestToken()));
            }
            await syncPendingAwards(userId, trackerRef.current, updateTracker);
            setFeedback({ type: 'success', text: `${warning ? `${warning} ` : ''}Your observed stops and journey statistics were saved.` });
        } catch (error) {
            setFeedback({ type: 'error', text: `${warning ? `${warning} ` : ''}${error.message || 'Could not save awards.'} Unsaved awards are retained; you can try again.` });
        }
    }

    async function addStampsToPassport() {
        if (busyRef.current || trackerRef.current.ride) return;
        busyRef.current = true;
        setBusy(true);
        try {
            await savePendingAwards();
        } finally {
            busyRef.current = false;
            setBusy(false);
        }
    }

    function selectBus(event) {
        setSelectedVehicleId(event.target.value);
        onSelectedBusChange?.(buses.find(bus => busKey(bus) === event.target.value) ?? null);
    }

    return (
        <section className="live-bus-tracker" aria-labelledby="live-bus-title">
            <div className="live-bus-heading">
                <p className="journey-eyebrow">EMBER LIVE</p>
                <h2 id="live-bus-title">{origin && destination ? 'Buses serving this route' : 'Soonest departures'}</h2>
            </div>
            <label className="live-bus-select-label" htmlFor="live-bus-select">Live buses</label>
            <select id="live-bus-select" className="live-bus-select" value={selectedVehicleId}
                onChange={selectBus} disabled={loading || buses.length === 0 || Boolean(tracker.ride) || busy}>
                <option value="">{loading ? 'Loading buses…' : 'Choose a bus'}</option>
                {tracker.ride && !selectedBus && <option value={busKey(tracker.ride)}>Tracked bus unavailable</option>}
                {buses.map(bus => <option key={busKey(bus)} value={busKey(bus)}>
                    {busLabel(bus)}
                </option>)}
            </select>
            {selectedBus && <p className="live-bus-stops">
                {selectedBus.currentStop?.name || 'Current stop unavailable'}
                {selectedBus.nextStop?.name ? ` → ${selectedBus.nextStop.name}` : ''}
            </p>}
            {selectedBus && !selectedBus.routeDetailsLoaded && <p role="status">Loading the full route…</p>}
            {selectedBus && <p className="live-bus-stops">
                {departureLabel(selectedBus)}{selectedBus.departureStop?.name ? ` from ${selectedBus.departureStop.name}` : ''}
                {selectedBus.upcomingTrip ? ' · Next trip; boarding is available when the trip becomes active.' : ''}
            </p>}
            {(selectedBus || tracker.ride) && <button className="live-bus-track-button" type="button"
                onClick={toggleTracking} disabled={busy || (!tracker.ride && (selectedBus?.upcomingTrip || !selectedBus?.routeDetailsLoaded))}>
                {tracker.ride ? 'I got off this bus' : "I'm getting on this bus"}
            </button>}
            {!tracker.ride && (tracker.stops.length > 0 || tracker.journeys.length > 0) &&
                <button className="live-bus-add-stamps-button" type="button" onClick={addStampsToPassport} disabled={busy}>
                    {busy ? 'Saving…' : `Add stamps to passport (${tracker.stops.length + tracker.journeys.length})`}
                </button>}
            {feedback && <p className={`live-bus-feedback is-${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>
                {feedback.text}
            </p>}
            {!loading && buses.length === 0 && feedback?.type !== 'error' &&
                <p className="live-bus-feedback">{origin && destination
                    ? 'No active or upcoming Ember buses serve this route right now.'
                    : 'No upcoming Ember departures are available right now.'}</p>}
        </section>
    );
}

export default LiveBusTracker;
