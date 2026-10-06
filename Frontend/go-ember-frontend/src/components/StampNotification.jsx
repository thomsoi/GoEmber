import stampImage from '../assets/stampDefault.png';
import '../css/StampNotification.css';

export default function StampNotification({ notification, onDismiss }) {
    return (
        <div className="stamp-notification-region" role="status" aria-live="polite" aria-atomic="true">
            {notification && <div className="stamp-notification" key={notification.count}>
                <img className="stamp-notification-icon" src={stampImage} alt="" />
                <div className="stamp-notification-copy">
                    <strong>{notification.count === 1 ? 'Stamp collected!' : `${notification.count} stamps collected!`}</strong>
                    <span>{notification.count === 1 && notification.name
                        ? `${notification.name} · Saved to your passport` : 'Saved to your passport. Happy travels!'}</span>
                </div>
                <button type="button" className="stamp-notification-dismiss" onClick={onDismiss}
                    aria-label="Dismiss stamp notification">×</button>
            </div>}
        </div>
    );
}
