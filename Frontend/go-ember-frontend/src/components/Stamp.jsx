import '../css/Stamp.css';

function Stamp({ stamp }) {
    const level = ['bronze', 'silver', 'gold'].includes(stamp.level?.toLowerCase())
        ? stamp.level.toLowerCase()
        : 'bronze';
    const collectionDate = new Date(`${stamp.lastCollected}T00:00:00`);
    const formattedDate = Number.isNaN(collectionDate.getTime())
        ? stamp.lastCollected
        : new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' }).format(collectionDate);

    return (
        <article className={`stamp stamp--${level}`}>
            <div className="stamp-seal" aria-label={`${level} stamp`}>
                <span>{level}</span>
            </div>
            <h3 className="stamp-stop-name">{stamp.stopName}</h3>
            <p className="stamp-collection-count">Collected {stamp.timesCollected} {stamp.timesCollected === 1 ? 'time' : 'times'}</p>
            <time className="stamp-last-collected" dateTime={stamp.lastCollected}>{formattedDate}</time>
        </article>
    );
}

export default Stamp;