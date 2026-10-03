function JourneyInfoRq() {
    //This component should be displayed on the routetracker page when no route is active, the user has not selected a journey
    //It should ask the user to select a time, start and end location
    //start and end point should be manually typed out for now
    //When the backend is integrated, this will call the right API endpoint and display the route using the appropriate components
    //once a route is active, this component should be replaced with the display route component
    return (
        <div className="journey-info-rq">
            <h1>Journey Info Request</h1>
        </div>
    )
}

export default JourneyInfoRq;