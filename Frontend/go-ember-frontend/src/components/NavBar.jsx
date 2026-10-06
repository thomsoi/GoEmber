import '../css/NavBar.css';
import TravelIcon from './TravelIcon';

const navItems = [
    { id: 'home', label: 'Home', href: '/' },
    { id: 'routes', label: 'Routes', href: '/route-tracker' },
];

function NavBar({ activePage }) {
    return (
        <nav className="navbar" aria-label="Main navigation">
            <div className="navbar-items">
                {navItems.map(item => {
                    const isActive = activePage === item.id;

                    return (
                        <a
                            key={item.id}
                            className={`navbar-link${isActive ? ' is-active' : ''}`}
                            href={item.href}
                            aria-current={isActive ? 'page' : undefined}
                        >
                            <TravelIcon name={item.id === 'home' ? 'book' : 'route'} />{item.label}
                        </a>
                    );
                })}
            </div>
        </nav>
    );
}

export default NavBar;
