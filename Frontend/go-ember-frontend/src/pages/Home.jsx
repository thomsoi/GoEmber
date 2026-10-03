import ProgressBar from '../components/ProgressBar';
import Passport from '../components/Passport';
import '../css/Home.css';

const sampleStamps = [
    { id: 'glenview-terminal', stopName: 'Glenview Terminal', timesCollected: 4, lastCollected: '2026-09-18', level: 'gold' },
    { id: 'harbour-exchange', stopName: 'Harbour Exchange', timesCollected: 3, lastCollected: '2026-09-12', level: 'silver' },
    { id: 'cedar-square', stopName: 'Cedar Square', timesCollected: 1, lastCollected: '2026-09-06', level: 'bronze' },
    { id: 'riverside', stopName: 'Riverside', timesCollected: 2, lastCollected: '2026-09-01', level: 'bronze' },
    { id: 'old-town-market', stopName: 'Old Town Market', timesCollected: 3, lastCollected: '2026-08-24', level: 'silver' },
    { id: 'university-gate', stopName: 'University Gate', timesCollected: 1, lastCollected: '2026-08-16', level: 'bronze' },
];

function Home() {
    return (
        <div className="home">
            <ProgressBar stampsCollected={sampleStamps.length} />
            <Passport
                stamps={sampleStamps}
                routesTravelled={3}
                townsVisited={6}
                distanceTravelledKm={42.8}
            />
        </div>
    );
}

export default Home;