import { useEffect, useState } from 'react';
import { searchLocations } from '../services/BackendAPI';

function LocationSearchField({ id, label, value, selectedLocation, onValueChange, onSelect }) {
    const [locations, setLocations] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        const query = value.trim();
        if (query.length < 2 || selectedLocation?.name === query) {
            return undefined;
        }

        let active = true;
        const timeoutId = window.setTimeout(() => {
            searchLocations(query)
                .then(results => {
                    if (!Array.isArray(results)) {
                        throw new Error('Ember returned an invalid location search response.');
                    }
                    if (active) setLocations(results);
                })
                .catch(requestError => {
                    if (active) setError(requestError.message || 'Could not search Ember stops.');
                })
                .finally(() => {
                    if (active) setLoading(false);
                });
        }, 250);

        return () => {
            active = false;
            window.clearTimeout(timeoutId);
        };
    }, [value, selectedLocation]);

    return (
        <div className="journey-field">
            <label htmlFor={id}>{label}</label>
            <input
                id={id}
                type="text"
                value={value}
                onChange={event => {
                    const nextValue = event.target.value;
                    setLocations([]);
                    setError('');
                    setLoading(nextValue.trim().length >= 2);
                    onValueChange(nextValue);
                }}
                placeholder="Search Ember stops or towns"
                autoComplete="off"
                aria-autocomplete="list"
                aria-controls={`${id}-results`}
                aria-expanded={locations.length > 0}
                required
            />
            {loading && <span className="journey-location-status" role="status">Searching Ember stops…</span>}
            {error && <span className="journey-location-error" role="alert">{error}</span>}
            {locations.length > 0 && (
                <ul className="journey-location-results" id={`${id}-results`}>
                    {locations.map(location => (
                        <li key={location.id}>
                            <button
                                type="button"
                                onClick={() => {
                                    setLocations([]);
                                    setLoading(false);
                                    setError('');
                                    onSelect(location);
                                }}
                            >
                                <span>{location.name}</span>
                                <small>{location.detailed_name || location.region_name}</small>
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            {!loading && !error && value.trim().length >= 2
                && !selectedLocation && locations.length === 0 && (
                    <span className="journey-location-status">No matching Ember stops found.</span>
                )}
        </div>
    );
}

export default LocationSearchField;
