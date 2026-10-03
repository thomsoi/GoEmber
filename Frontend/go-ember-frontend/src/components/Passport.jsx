import emberLogo from '../assets/emberlogo.jpg';
import '../css/Passport.css';

function Passport({
    username = 'Guest',
    stampsCollected = 0,
    routesTravelled = 0,
    townsVisited = 0,
    distanceTravelledKm = 0,
}) {
    const formatCount = value => Number.isFinite(value)
        ? new Intl.NumberFormat().format(value)
        : '0';

    return (
        <section className="passport" aria-label="Travel passport">
            <article className="passport-card" aria-labelledby="passport-title">
                <header className="passport-header">
                    <div className="passport-logo-wrap">
                        <img className="passport-logo" src={emberLogo} alt="Ember" />
                    </div>
                    <h2 id="passport-title" className="passport-title">Passport</h2>
                </header>

                <div className="passport-profile">
                    <span className="passport-rank">Explorer</span>
                    <h3 className="passport-username">{username}</h3>
                </div>

                <dl className="passport-stats" aria-label="Travel stats">
                    <div className="passport-stat">
                        <dt>Stamps collected</dt>
                        <dd>{formatCount(stampsCollected)}</dd>
                    </div>
                    <div className="passport-stat">
                        <dt>Routes travelled</dt>
                        <dd>{formatCount(routesTravelled)}</dd>
                    </div>
                    <div className="passport-stat">
                        <dt>Towns visited</dt>
                        <dd>{formatCount(townsVisited)}</dd>
                    </div>
                    <div className="passport-stat">
                        <dt>Distance travelled</dt>
                        <dd>{formatCount(distanceTravelledKm)} <span>km</span></dd>
                    </div>
                </dl>
            </article>
        </section>
    );
}

export default Passport;