import RouteNode from './RouteNode';
import '../css/DisplayRoute.css';

function DisplayRoute({ startLocation, endLocation, departureTime, stops, onVisitNextStop }) {
    const nextStop = stops.find(stop => !stop.visited);
    const orderedStops = [...stops].reverse();

    return (
        <section className="display-route" aria-labelledby="display-route-title">
            <header className="display-route-header">
                <p className="journey-eyebrow">YOUR JOURNEY · {departureTime}</p>
                <h1 id="display-route-title">{startLocation} to {endLocation}</h1>
                <p className="display-route-note">Sample itinerary · stop times are estimates</p>
            </header>

            <ol className="display-route-list" aria-label="Journey stops, destination first">
                {orderedStops.map(stop => (
                    <li
                        className={`display-route-stop${stop.visited ? ' is-visited' : ''}${stop.id === nextStop?.id ? ' is-next' : ''}`}
                        key={stop.id}
                    >
                        <RouteNode stop={stop} isNext={stop.id === nextStop?.id} />
                    </li>
                ))}
            </ol>

            <div className="display-route-progress">
                <p>
                    {stops.filter(stop => stop.visited).length} of {stops.length} stops visited
                </p>
                <button
                    className="visit-stop-button"
                    type="button"
                    onClick={onVisitNextStop}
                    disabled={!nextStop}
                >
                    {nextStop ? `Mark ${nextStop.name} visited` : 'All stops visited'}
                </button>
            </div>
        </section>
    );
}

export default DisplayRoute;