import { recordCompletedJourney, recordLocationVisits, recordTownVisit } from './BackendAPI.js';

const VISIT_BATCH_SIZE = 50;

/** Remove queued work only after both the server write and local checkpoint succeed. */
export async function syncPendingAwards(userId, pending, updateTracker) {
    // Stable operation IDs make retries safe when a committed response is lost.
    for (let index = 0; index < pending.stops.length; index += VISIT_BATCH_SIZE) {
        const batch = pending.stops.slice(index, index + VISIT_BATCH_SIZE);
        await recordLocationVisits(userId, batch);
        const acknowledged = new Set(batch.map(stop => stop.operationId));
        updateTracker(current => ({ ...current,
            stops: current.stops.filter(stop => !acknowledged.has(stop.operationId)) }));
    }

    for (const journey of pending.journeys) {
        const { townsToStamp, completionRecorded, ...details } = journey;
        if (!completionRecorded) {
            await recordCompletedJourney(userId, details);
            updateTracker(current => ({ ...current, journeys: current.journeys.map(item =>
                item.journeyKey === journey.journeyKey ? { ...item, completionRecorded: true } : item) }));
        }
        for (const town of townsToStamp) {
            const operationId = `${journey.journeyKey}:town:${journey.towns.indexOf(town)}`;
            await recordTownVisit(userId, town, operationId);
            updateTracker(current => ({ ...current, journeys: current.journeys.map(item =>
                item.journeyKey === journey.journeyKey
                    ? { ...item, townsToStamp: item.townsToStamp.filter(name => name !== town) } : item) }));
        }
        updateTracker(current => ({ ...current,
            journeys: current.journeys.filter(item => item.journeyKey !== journey.journeyKey) }));
    }
}
