import '../css/RouteNode.css';

function RouteNode({ stop, isNext }) {
    return (
        <div className={`route-node${stop.visited ? ' is-visited' : ''}${isNext ? ' is-next' : ''}`}>
            <span className="route-node-marker" aria-hidden="true" />
            <div className="route-node-details">
                <div className="route-node-heading">
                    <h2>{stop.name}</h2>
                    {stop.arrivalTime && <time>{stop.arrivalTime}</time>}
                </div>
                {stop.location && <p>{stop.location}</p>}
                {typeof stop.stampCollected === 'boolean' && (
                    <span className="route-node-stamp">
                        {stop.stampCollected ? 'Stamp collected' : 'Stamp not collected'}
                    </span>
                )}
            </div>
        </div>
    );
}

export default RouteNode;