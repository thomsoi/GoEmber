import '../css/JourneyStats.css';

function JourneyStats({ stats }) {
    return (
        <div className="journey-stats-backdrop">
            <section className="journey-stats" role="dialog" aria-modal="true" aria-labelledby="journey-stats-title">
                <p className="journey-eyebrow">JOURNEY COMPLETE</p>
                <h2 id="journey-stats-title">Journey summary</h2>
                <dl className="journey-stats-grid">
                    <div><dt>Distance travelled</dt><dd>{stats.distanceKm.toFixed(1)} km</dd></div>
                    <div><dt>Stamps collected</dt><dd>{stats.stampsCollected}</dd></div>
                    <div><dt>Towns visited</dt><dd>{stats.townsVisited}</dd></div>
                    <div><dt>Time taken</dt><dd>{stats.durationMinutes} min</dd></div>
                </dl>
                <a className="journey-stats-home" href="/">Return to Home</a>
            </section>
        </div>
    );
}

export default JourneyStats;