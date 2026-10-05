import Stamp from './Stamp';

export const PASSPORT_PAGE_SIZE = 4;

export default function PassportCollection({ section, stamps, cities, page }) {
    const showingCities = section === 'cities';
    const entries = showingCities ? cities : stamps;
    const pageCount = Math.max(1, Math.ceil(entries.length / PASSPORT_PAGE_SIZE));
    const currentEntries = entries.slice((page - 1) * PASSPORT_PAGE_SIZE, page * PASSPORT_PAGE_SIZE);

    return (
        <article className="passport-card passport-stamp-page" aria-labelledby="passport-collection-title">
            <header className="passport-stamp-header">
                <p>EMBER EXPLORER</p>
                <h2 id="passport-collection-title">{showingCities ? 'Cities and towns visited' : 'Bus stop stamps'}</h2>
                <span>Page {page} of {pageCount}</span>
            </header>
            {currentEntries.length > 0 ? (
                <div className="passport-stamp-grid" aria-live="polite">
                    {showingCities
                        ? currentEntries.map(city => (
                            <article className="passport-city" key={city.key}>
                                <h3>{city.name}</h3>
                                <p>Visited</p>
                            </article>
                        ))
                        : currentEntries.map(stamp => <Stamp key={stamp.id} stamp={stamp} />)}
                </div>
            ) : (
                <p className="passport-empty-stamps">
                    {showingCities ? 'No cities or towns visited yet.' : 'No bus stop stamps collected yet.'}
                </p>
            )}
        </article>
    );
}
