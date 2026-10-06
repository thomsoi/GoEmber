import '../css/ProgressBar.css';
import TravelIcon from './TravelIcon';

function ProgressBar({ stampsCollected = 0, stampsPerLevel = 5 }) {
    const stampCount = Number.isFinite(stampsCollected)
        ? Math.max(0, Math.floor(stampsCollected))
        : 0;
    const levelStampGoal = Number.isFinite(stampsPerLevel) && stampsPerLevel > 0
        ? Math.max(1, Math.floor(stampsPerLevel))
        : 5;
    const level = Math.floor(stampCount / levelStampGoal) + 1;
    const stampsTowardNextLevel = stampCount % levelStampGoal;
    const progress = (stampsTowardNextLevel / levelStampGoal) * 100;

    return (
        <section className="progress-bar" aria-label="Passport progress">
            <h2 className="progress-bar-level"><TravelIcon name="star" />Level {level}</h2>
            <div className="progress-bar-count" aria-live="polite">
                {stampsTowardNextLevel} of {levelStampGoal} stamps to level {level + 1}
            </div>
            <div
                className="progress-bar-track"
                role="progressbar"
                aria-label={`Progress to level ${level + 1}`}
                aria-valuemin={0}
                aria-valuemax={levelStampGoal}
                aria-valuenow={stampsTowardNextLevel}
                aria-valuetext={`${stampsTowardNextLevel} of ${levelStampGoal} stamps`}
            >
                <div className="progress-bar-fill" style={{ width: `${progress}%` }} />
            </div>
        </section>
    );
}

export default ProgressBar;
