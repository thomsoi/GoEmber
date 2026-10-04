import emberLogo from '../assets/emberlogo.jpg';
import { useState } from 'react';
import Stamp from './Stamp';
import '../css/Passport.css';

function Passport({
    username = 'Guest',
    stamps = [],
    busNumbersRidden = 0,
    townsVisited = 0,
    distanceTravelledKm = 0,
}) {
    const [currentPage, setCurrentPage] = useState(0);
    const stampsPerPage = 4;
    const stampPageCount = Math.max(1, Math.ceil(stamps.length / stampsPerPage));
    const stampsCollected = stamps.length;
    const currentStamps = stamps.slice((currentPage - 1) * stampsPerPage, currentPage * stampsPerPage);

    const formatCount = value => Number.isFinite(value)
        ? new Intl.NumberFormat().format(value)
        : '0';

    function changePage(direction) {
        setCurrentPage(page => Math.max(0, Math.min(stampPageCount, page + direction)));
    }

    return (
        <section className="passport" aria-label="Travel passport">
            <div className={`passport-book${currentPage > 0 ? ' is-open' : ''}`}>
                <button
                    className="passport-page-arrow"
                    type="button"
                    aria-label={currentPage === 1 ? 'Return to passport cover' : 'Previous passport page'}
                    onClick={() => changePage(-1)}
                    disabled={currentPage === 0}
                >
                    <span className="passport-arrow-icon is-left" aria-hidden="true" />
                </button>

                <div className="passport-page-stack">
                    {currentPage === 0 ? (
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
                                    <dd>
                                        <button
                                            className="passport-stamps-link"
                                            type="button"
                                            onClick={() => setCurrentPage(1)}
                                            aria-label={`View all ${stampsCollected} collected stamps`}
                                        >
                                            {formatCount(stampsCollected)}
                                            <span>View stamps</span>
                                        </button>
                                    </dd>
                                </div>
                                <div className="passport-stat">
                                    <dt>Bus numbers ridden</dt>
                                    <dd>{formatCount(busNumbersRidden)}</dd>
                                </div>
                                <div className="passport-stat">
                                    <dt>Cities and towns visited</dt>
                                    <dd>{formatCount(townsVisited)}</dd>
                                </div>
                                <div className="passport-stat">
                                    <dt>Distance travelled (estimated)</dt>
                                    <dd>
                                        {Number.isFinite(distanceTravelledKm)
                                            ? <>{formatCount(distanceTravelledKm)} <span>km</span></>
                                            : 'Unavailable'}
                                    </dd>
                                </div>
                            </dl>
                        </article>
                    ) : (
                        <article key={currentPage} className="passport-card passport-stamp-page" aria-labelledby="passport-stamps-title">
                            <header className="passport-stamp-header">
                                <p>EMBER EXPLORER</p>
                                <h2 id="passport-stamps-title">Collected stamps</h2>
                                <span>Page {currentPage} of {stampPageCount}</span>
                            </header>
                            {currentStamps.length > 0 ? (
                                <div className="passport-stamp-grid" aria-live="polite">
                                    {currentStamps.map(stamp => <Stamp key={stamp.id} stamp={stamp} />)}
                                </div>
                            ) : (
                                <p className="passport-empty-stamps">No stamps collected yet.</p>
                            )}
                        </article>
                    )}
                </div>

                <button
                    className="passport-page-arrow"
                    type="button"
                    aria-label={currentPage === 0 ? 'Open collected stamps' : 'Next passport page'}
                    onClick={() => changePage(1)}
                    disabled={currentPage === stampPageCount}
                >
                    <span className="passport-arrow-icon is-right" aria-hidden="true" />
                </button>
            </div>
        </section>
    );
}

export default Passport;
