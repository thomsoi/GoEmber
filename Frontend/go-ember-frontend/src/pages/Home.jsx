import ProgressBar from '../components/ProgressBar';
import Passport from '../components/Passport';
import NavBar from '../components/NavBar';
import '../css/Home.css';

function Home() {
    return (
        <div className="home">
            <ProgressBar />
            <Passport />
            <NavBar />
        </div>
    );
}

export default Home;