import RouteNode from './RouteNode';
import '../css/DisplayRoute.css';

function DisplayRoute({ startLocation, endLocation, routeNumber, stops = [] }) {
    const nextStop = stops.find(stop => !stop.visited);
    const orderedStops = [...stops].reverse();

    return (
        <section className="display-route" aria-labelledby="display-route-title">
            <header className="display-route-header">
                <p className="journey-eyebrow">LIVE BUS ROUTE{routeNumber ? ` · ${routeNumber}` : ''}</p>
                <h1 id="display-route-title">{startLocation} to {endLocation}</h1>
                <p className="display-route-note">Stop order from the selected Ember bus</p>
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
        </section>
    );
}

export default DisplayRoute;