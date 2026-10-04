import { useState } from 'react';
import '../css/JourneyInfoRq.css';

function JourneyInfoRq({ onStartJourney, disabled = false }) {
    const [startLocation, setStartLocation] = useState('');
    const [endLocation, setEndLocation] = useState('');
    const [formError, setFormError] = useState('');

    function handleSubmit(event) {
        event.preventDefault();
        if (startLocation.trim() && endLocation.trim()
            && startLocation.trim().toLocaleLowerCase() === endLocation.trim().toLocaleLowerCase()) {
            setFormError('Choose a different destination from your starting point.');
            return;
        }

        setFormError('');
        onStartJourney({
            startLocation: startLocation.trim(),
            endLocation: endLocation.trim(),
        });
    }

    return (
        <section className="journey-info-rq" aria-labelledby="journey-request-title">
            <p className="journey-eyebrow">EMBER ROUTES</p>
            <h1 id="journey-request-title">Plan a journey</h1>
            <p className="journey-intro">Find live buses serving your route, or leave the fields blank to see the soonest departures.</p>

            <form className="journey-request-form" onSubmit={handleSubmit}>
                <label className="journey-field">
                    <span>Starting point</span>
                    <input
                        type="text"
                        value={startLocation}
                        onChange={event => setStartLocation(event.target.value)}
                        placeholder="Enter a stop or town"
                        autoComplete="street-address"
                        disabled={disabled}
                    />
                </label>
                <label className="journey-field">
                    <span>Destination</span>
                    <input
                        type="text"
                        value={endLocation}
                        onChange={event => setEndLocation(event.target.value)}
                        placeholder="Where would you like to go?"
                        disabled={disabled}
                    />
                </label>
                {formError && <p className="journey-form-error" role="alert">{formError}</p>}
                <button className="journey-submit-button" type="submit" disabled={disabled}>Find live buses</button>
            </form>
        </section>
    );
}

export default JourneyInfoRq;
