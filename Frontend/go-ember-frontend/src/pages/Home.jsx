import ProgressBar from '../components/ProgressBar';
import Passport from '../components/Passport';
import '../css/Home.css';

function Home() {
    return (
        <div className="home">
            <ProgressBar />
            <Passport />
        </div>
    );
}

export default Home;