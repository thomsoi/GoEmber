import './App.css'
import Home from './pages/Home'
import RouteTracker from './pages/RouteTracker'
import NavBar from './components/NavBar'
import TravelIcon from './components/TravelIcon'

function App() {
  const isRouteTracker = window.location.pathname.replace(/\/+$/, '') === '/route-tracker'

  return (
    <>
      <a className="skip-link" href="#main-content">Skip to content</a>
      <header className="app-header">
        <a className="app-brand" href="/" aria-label="GoEmber home"><TravelIcon name="bus" />GoEmber</a>
        <span className="app-tagline">YOUR EMBER ADVENTURE</span>
      </header>
      <main id="main-content" className='main-content' tabIndex={-1}>
        {isRouteTracker ? <RouteTracker /> : <Home />}
      </main>
      <NavBar activePage={isRouteTracker ? 'routes' : 'home'} />
    </>
  )
}

export default App;
