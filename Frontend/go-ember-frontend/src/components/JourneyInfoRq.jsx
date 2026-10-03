import { useState } from 'react';
import '../css/JourneyInfoRq.css';

function JourneyInfoRq({ onStartJourney }) {
    const [departureTime, setDepartureTime] = useState(() => new Date().toTimeString().slice(0, 5));
    const [startLocation, setStartLocation] = useState('');
    const [endLocation, setEndLocation] = useState('');
    const [formError, setFormError] = useState('');

    function handleSubmit(event) {
        event.preventDefault();
        if (startLocation.trim().toLocaleLowerCase() === endLocation.trim().toLocaleLowerCase()) {
            setFormError('Choose a different destination from your starting point.');
            return;
        }

        setFormError('');
        onStartJourney({
            startLocation: startLocation.trim(),
            endLocation: endLocation.trim(),
            departureTime,
        });
    }

    return (
        <section className="journey-info-rq" aria-labelledby="journey-request-title">
            <p className="journey-eyebrow">EMBER ROUTES</p>
            <h1 id="journey-request-title">Plan a journey</h1>
            <p className="journey-intro">Choose your departure time and where you are going.</p>

            <form className="journey-request-form" onSubmit={handleSubmit}>
                <label className="journey-field">
                    <span>Departure time</span>
                    <input
                        type="time"
                        value={departureTime}
                        onChange={event => setDepartureTime(event.target.value)}
                        required
                    />
                </label>
                <label className="journey-field">
                    <span>Starting point</span>
                    <input
                        type="text"
                        value={startLocation}
                        onChange={event => setStartLocation(event.target.value)}
                        placeholder="Enter a stop or town"
                        autoComplete="street-address"
                        required
                    />
                </label>
                <label className="journey-field">
                    <span>Destination</span>
                    <input
                        type="text"
                        value={endLocation}
                        onChange={event => setEndLocation(event.target.value)}
                        placeholder="Where would you like to go?"
                        required
                    />
                </label>
                {formError && <p className="journey-form-error" role="alert">{formError}</p>}
                <button className="journey-submit-button" type="submit">Find a route</button>
            </form>
            <p className="journey-sample-note">A sample itinerary will be shown until live routes are available.</p>
        </section>
    );
}

export default JourneyInfoRq;