export const TRACKER_KEY = 'ember-passport-tracker-v1';
export const emptyTracker = () => ({ version: 1, routeRequest: null, ownerToken: null, ride: null, stops: [], journeys: [] });

export function loadTracker(storage) {
    const raw = storage.getItem(TRACKER_KEY);
    if (!raw) return emptyTracker();
    const data = JSON.parse(raw);
    if (data.version !== 1 || !Array.isArray(data.stops) || !Array.isArray(data.journeys)
        || (data.ride && !Array.isArray(data.ride.route))) {
        throw new Error('Saved ride data could not be read.');
    }
    return data;
}

export function saveTracker(storage, data) {
    storage.setItem(TRACKER_KEY, JSON.stringify(data));
}

export function reassignQueuedAwards(tracker, newOwnerToken) {
    if (!newOwnerToken || tracker.ride || (!tracker.stops.length && !tracker.journeys.length)) {
        throw new Error('Only pending awards can be moved to a signed-in guest passport.');
    }
    return { ...tracker, ownerToken: newOwnerToken };
}
