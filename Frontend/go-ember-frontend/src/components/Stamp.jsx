import defaultStamp from '../assets/stampDefault.png';
import bronzeStamp from '../assets/stampbronze.png';
import silverStamp from '../assets/stampsilver.png';
import goldStamp from '../assets/stampgold.png';
import '../css/Stamp.css';

const stampImages = {
    untiered: defaultStamp,
    bronze: bronzeStamp,
    silver: silverStamp,
    gold: goldStamp,
};

function Stamp({ stamp }) {
    const level = ['untiered', 'bronze', 'silver', 'gold'].includes(stamp.level?.toLowerCase())
        ? stamp.level.toLowerCase()
        : 'untiered';
    const collectionDate = stamp.lastCollected
        ? new Date(stamp.lastCollected.length === 10 ? `${stamp.lastCollected}T00:00:00` : stamp.lastCollected)
        : null;
    const formattedDate = !collectionDate || Number.isNaN(collectionDate.getTime())
        ? stamp.lastCollected
        : new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' }).format(collectionDate);
    const formattedPopularity = new Intl.NumberFormat(undefined, {
        maximumSignificantDigits: 3,
    }).format(stamp.percentOfUsersWithStamp ?? 0);

    return (
        <article className={`stamp stamp--${level}`}>
            <img className="stamp-seal" src={stampImages[level]} alt={`${level} stamp`} />
            <h3 className="stamp-stop-name">{stamp.stopName}</h3>
            <p className="stamp-rarity">Collected by {formattedPopularity}% of users</p>
            <p className="stamp-collection-count">Collected {stamp.timesCollected} {stamp.timesCollected === 1 ? 'time' : 'times'}</p>
            {formattedDate && (
                <time className="stamp-last-collected" dateTime={stamp.lastCollected}>{formattedDate}</time>
            )}
        </article>
    );
}

export default Stamp;