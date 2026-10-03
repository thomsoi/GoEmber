import './App.css'
import Home from './pages/Home'
import RouteTracker from './pages/RouteTracker'
import NavBar from './components/NavBar'

function App() {
  const isRouteTracker = window.location.pathname.replace(/\/+$/, '') === '/route-tracker'

  return (
    <>
      <main className='main-content'>
        {isRouteTracker ? <RouteTracker /> : <Home />}
      </main>
      <NavBar activePage={isRouteTracker ? 'routes' : 'home'} />
    </>
  )
}

export default App;
