import { useEffect, useState } from 'react';
import ProgressBar from '../components/ProgressBar';
import Passport from '../components/Passport';
import { ensureCurrentPassport } from '../services/BackendAPI';
import '../css/Home.css';

function Home() {
    const [passportData, setPassportData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        let active = true;

        ensureCurrentPassport()
            .then(data => {
                if (active) setPassportData(data);
            })
            .catch(() => {
                if (active) setError('Could not load your passport. Check that the Ember service is running and try again.');
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, [reloadKey]);

    function retryLoadingPassport() {
        setLoading(true);
        setError('');
        setReloadKey(key => key + 1);
    }

    const passport = passportData?.passport;
    const stamps = (passport?.stamps ?? []).map(stamp => ({
        id: stamp.stampKey ?? stamp.locationId,
        stopName: stamp.stampName,
        timesCollected: stamp.visitCount,
        lastCollected: stamp.mostRecentVisitAt,
        level: stamp.tier,
        percentOfUsersWithStamp: stamp.percentOfUsersWithStamp,
    }));

    return (
        <div className="home">
            {loading && <p className="passport-data-state" role="status">Loading your passport…</p>}
            {error && (
                <div className="passport-data-error" role="alert">
                    <p>{error}</p>
                    <button type="button" onClick={retryLoadingPassport}>Try again</button>
                </div>
            )}
            {passport && !error && (
                <>
                    <ProgressBar stampsCollected={stamps.length} />
                    <Passport
                        username={passportData.username}
                        stamps={stamps}
                        routesTravelled={passport.routesTravelled ?? 0}
                        townsVisited={passport.townsVisited ?? 0}
                        distanceTravelledKm={passport.totalDistanceTravelled}
                    />
                </>
            )}
        </div>
    );
}

export default Home;