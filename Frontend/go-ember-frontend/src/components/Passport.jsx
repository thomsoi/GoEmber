import emberLogo from '../assets/emberlogo.jpg';
import { useState } from 'react';
import PassportCollection, { PASSPORT_PAGE_SIZE } from './PassportCollection';
import '../css/Passport.css';

function Passport({
    username = 'Guest',
    stamps = [],
    busNumbersRidden = 0,
    cities = [],
    distanceTravelledKm = 0,
}) {
    const [currentPage, setCurrentPage] = useState(0);
    const [section, setSection] = useState('stamps');
    const showingCities = section === 'cities';
    const entries = showingCities ? cities : stamps;
    const pageCount = Math.max(1, Math.ceil(entries.length / PASSPORT_PAGE_SIZE));
    const stampsCollected = stamps.length;

    const formatCount = value => Number.isFinite(value)
        ? new Intl.NumberFormat().format(value)
        : '0';

    function openSection(nextSection) {
        setSection(nextSection);
        setCurrentPage(1);
    }

    function changePage(direction) {
        setCurrentPage(page => Math.max(0, Math.min(pageCount, page + direction)));
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
                                            onClick={() => openSection('stamps')}
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
                                    <dd>
                                        <button
                                            className="passport-stamps-link"
                                            type="button"
                                            onClick={() => openSection('cities')}
                                            aria-label={`View all ${cities.length} visited cities and towns`}
                                        >
                                            {formatCount(cities.length)}
                                            <span>View cities</span>
                                        </button>
                                    </dd>
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
                        <PassportCollection key={`${section}:${currentPage}`}
                            section={section} stamps={stamps} cities={cities} page={currentPage} />
                    )}
                </div>

                <button
                    className="passport-page-arrow"
                    type="button"
                    aria-label={currentPage === 0 ? 'Open collected stamps' : 'Next passport page'}
                    onClick={() => currentPage === 0 ? openSection('stamps') : changePage(1)}
                    disabled={currentPage === pageCount}
                >
                    <span className="passport-arrow-icon is-right" aria-hidden="true" />
                </button>
            </div>
        </section>
    );
}

export default Passport;
