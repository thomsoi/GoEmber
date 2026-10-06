export default function TravelIcon({ name, className = '' }) {
    const paths = {
        book: <><path d="M4 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H4z" /><path d="M13 7a3 3 0 0 1 3-3h5v15h-4a4 4 0 0 0-4 2M7 8h3M7 12h3M16 8h2M16 12h2" /></>,
        route: <><circle cx="6" cy="6" r="3" /><circle cx="18" cy="18" r="3" /><path d="M9 6h7a4 4 0 0 1 0 8H8a4 4 0 0 0 0 8" /></>,
        bus: <><rect x="4" y="3" width="16" height="17" rx="4" /><path d="M4 12h16M12 3v9M7 20v2M17 20v2M7 16h1M16 16h1" /></>,
        star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9z" />,
    };
    return <svg className={`travel-icon ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor"
        strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">{paths[name]}</svg>;
}
