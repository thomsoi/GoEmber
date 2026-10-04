Locations

Find stops and places to travel from or to.
List locations
get/v1/locations/

Return non-test locations, ordered by recent booking popularity. Defaults to stop areas; use type to select stop points, hubs or all types. Optional origin and destination lists contain location IDs that allow travel to or from each result. Results may be cached for up to ten minutes. Use location search to find stops by name.

STOP_AREA locations group boarding points, such as all stances at a bus station. Use type=all to include both areas and their STOP_POINT locations; each point's area_id links it to its parent area. The response examples use illustrative data.
Authorizations:
NoneCognitoJWT
query Parameters
type	
string
Default: "STOP_AREA"
Enum: "STOP_POINT" "STOP_AREA" "HUB" "all"
include_origins	
boolean
Default: false

Include allowed_origins location IDs.
include_destinations	
boolean
Default: false

Include allowed_destinations location IDs.
Responses
200

Locations.
400

Invalid location type.
Response samples

    200

Content type
application/json
Example
Stop area (default)

Illustrative bus station covering all its boarding stances.
Copy
Expand all Collapse all
[

    {
        "id": 1000,
        "type": "STOP_AREA",
        "name": "Example Bus Station",
        "region_name": "Example Town",
        "detailed_name": "Bus Station",
        "description": "Covers all boarding stances at the bus station.",
        "code": "EXT"
    }

]
Search locations
get/v1/locations/search/

Search stop names, regions, detailed names and codes. With no query, return popular bookable locations. Results normally require future service and an allowed fare. An origin restricts results to reachable destinations. With a text query, include_unreachable=true also returns unreachable matches.

Supplying ids resolves those locations directly, bypassing the text, limit and bookability filters. Fields with null or empty-string values are omitted. Optional availability reports scheduled journeys, rather than free seats.
Authorizations:
NoneCognitoJWT
query Parameters
query	
string
Default: ""

Case-insensitive search; whitespace-separated terms must all match.
limit	
integer [ 1 .. 50 ]
Default: 10

Maximum search results; ignored when ids is supplied.
type	
string
Default: "STOP_AREA"
Enum: "STOP_POINT" "STOP_AREA" "HUB" "all"

Location type to return.
origin	
integer

Origin location ID for reachable destinations and availability.
ids	
Array of integers <= 50 items

Resolve up to 50 location IDs; repeated parameters or comma-separated IDs are accepted.
include_origins	
boolean
Default: false

Include allowed_origins IDs in each result.
include_destinations	
boolean
Default: false

Include allowed_destinations IDs in each result.
include_unreachable	
boolean
Default: false

With a text query, include matches unreachable from origin.
include_availability	
boolean
Default: false

Add scheduled journey availability for each result. Requires origin.
travel_date	
string <date>

Date for optional availability. Defaults to the current UTC date.
Responses
200

Matching locations.
400

Invalid search parameters or include_availability without origin.
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
[

    {
        "id": 1000,
        "type": "STOP_AREA",
        "name": "Example Bus Station",
        "region_name": "Example Town",
        "detailed_name": "Bus Station",
        "description": "Covers all boarding stances at the bus station.",
        "code": "EXT"
    }

]
Quotes and fares

Find journeys, check availability and calculate ticket prices.
Get Quotes
get/v1/quotes/

Search for possible journeys between locations. Full journey information will be returned, including pricing.

Quotes are ordered by the scheduled departure of the origin of the scheduled transit leg.
Authorizations:
NoneCognitoJWT
query Parameters
origin
required
	
integer
destination
required
	
integer
departure_date_from
required
	
string <date-time>
departure_date_to
required
	
string <date-time>
arrival_date_from	
string <date-time>
arrival_date_to	
string <date-time>
adult	
integer
child	
integer
young_child	
integer
concession	
integer
wheelchair	
integer
bicycle	
integer
replacing_pass	
string
order_uid	
string

Required when replacing_pass is supplied
Responses
200

Available journey quotes
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
{

    "quotes": [
        {}
    ],
    "min_card_transaction": 0,
    "replacing_pass": {
        "code": "string",
        "total_price": 0
    }

}
Get Fare Triangle
get/v1/fares/

Return onboard adult and child fare tables for active route numbers. Each direction contains ordered stops and fares keyed first by origin ID, then destination ID. Amounts are in pence. Missing directions are null; missing pairs are not offered. For online journey prices, use GET /v1/quotes/.
Authorizations:
NoneCognitoJWT
Responses
200

Fare tables for active routes.
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
[

    {
        "route_number": "string",
        "outbound": {},
        "inbound": {}
    }

]
Check journey availability
get/v1/quotes/availability/

Count scheduled journeys from one origin to each requested destination on a travel date, and find the fewest vehicle changes. Counts include journeys with an allowed fare and are independent of seat availability and bookings. Use GET /v1/quotes/ to find bookable journeys and prices.

The response is keyed by destination location ID. A destination with no journeys has count 0 and min_changes null; a direct journey has min_changes 0. Omitting destinations, or supplying an empty list, returns {}. Results may be cached for up to 24 hours.
Authorizations:
NoneCognitoJWT
query Parameters
origin
required
	
integer

Origin location ID.
destinations	
Array of integers <= 50 items

Up to 50 destination IDs. Repeat destinations=1&destinations=2; comma-separated IDs are also accepted.
travel_date	
string <date>

Travel date. Defaults to the current UTC date; journeys are searched within that day's Europe/London time window.
Responses
200

Journey availability by destination.
400

Missing or invalid origin, invalid date or destination IDs, or more than 50 destinations.
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
{

    "2": {
        "count": 8,
        "min_changes": 0
    },
    "3": {
        "count": 0,
        "min_changes": null
    }

}
Trips

Look up a trip, its stops and its route geography.
Get Trip Info
get/v1/trips/{raw_trip_id}/

Use the query parameters to specify the info you want to receive. You can set multiple parameters to get any combination of data, or set all=true to get all the available trip info.

Unauthorised or unprivileged users can only receive vehicle, route and description information. Only a subset of this information will be returned.
Authorizations:
NoneCognitoJWT
path Parameters
raw_trip_id
required
	
string

Trip UID or numeric ID
query Parameters
all	
boolean

get all available trip info
route	
boolean

get list of location times ordered by scheduled departure time
vehicle	
boolean

Get info about the assigned vehicle
description	
boolean

Get general information about the trip, including route number and notes
include_cancelled_stops	
boolean

Include cancelled stops in the output (default: false)
Responses
200

OK
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
{

    "description": {
        "calendar_date": "2019-08-24",
        "notes": "string",
        "notes_details": {},
        "pattern_id": 0,
        "route_number": "string",
        "type": "public"
    },
    "route": [
        {}
    ],
    "vehicle": {
        "bicycle": 0,
        "brand": "string",
        "colour": "string",
        "gps": {},
        "has_toilet": true,
        "has_wifi": true,
        "id": 0,
        "is_backup_vehicle": true,
        "name": "string",
        "owner_id": 0,
        "plate_number": "string",
        "seat": 0,
        "secondary_gps": {},
        "wheelchair": 0
    }

}
Get Trip Geographies
get/v1/trip-geographies/

Fetch stored geography for up to 25 trip UIDs in one request. Duplicate UIDs are removed; trips without geography are omitted from the result.
Authorizations:
NoneCognitoJWT
query Parameters
trips
required
	
Array of strings <= 25 items

Trip UIDs as repeated trips parameters.
Responses
200

Geography keyed by trip UID.
400

Invalid trip list or more than 25 trip UIDs.
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
{

    "property1": {
        "id": 0,
        "stops": [],
        "paths": {}
    },
    "property2": {
        "id": 0,
        "stops": [],
        "paths": {}
    }

}
Get Trip Geography
get/v1/trips/{raw_trip_id}/geography/

Get stored route geometry using the trip UID. Paths are encoded polylines at precision 6 in longitude, latitude order; see Useful libraries in Reference for mapping guidance.
Authorizations:
NoneCognitoJWT
path Parameters
raw_trip_id
required
	
string

Trip UID.
Responses
200

Stored trip geography.
404

No stored geography exists for this trip.
Response samples

    200

Content type
application/json
Copy
Expand all Collapse all
{

    "id": 0,
    "stops": [
        0
    ],
    "paths": {
        "property1": "string",
        "property2": "string"
    }

}
Live vehicles

Download live vehicle data in Protocol Buffer format.
Get Vehicle Live
get/v1/vehicles/live/

Download public live vehicle positions and current or next trip details as a binary Protobuf LiveVehicleList. Decode the response using the schemas in Reference or the included protobuf directory.
Authorizations:
NoneCognitoJWT
Responses
200

Public vehicle protobuf

Decode the binary response as to.ember.vehicles.LiveVehicleList using vehicles.proto. See the Protobuf reference for every field and decoding examples.
Get Vehicle Live Details
get/v1/vehicles/live/{vehicle_id}/

Returns live details and trip geography for a vehicle.
Authorizations:
NoneCognitoJWT
path Parameters
vehicle_id
required
	
string
Responses
200

Public vehicle protobuf

Decode the binary response as to.ember.vehicles.LiveVehicleData using vehicles.proto. See the Protobuf reference for every field and decoding examples.
Service updates

Read public service updates and their summaries.
Get Service Update
get/v1/service-updates/

Return the most recently started service update active at as_of, which defaults to now. The response always includes result, containing the update or null when none is active. If the latest active update's preview flag differs from the requested preview value, result is also null.
Authorizations:
NoneCognitoJWT
query Parameters
as_of	
string <date-time>

Time to check. Defaults to now.
preview	
boolean
Default: false

Whether to request a preview update.
Responses
200

Current public service update, or null when no update matches.
Response samples

    200

Content type
application/json
Example
An active service update

Illustrative update showing the public fields and optional link and location.
Copy
Expand all Collapse all
{

    "result": {
        "id": 1001,
        "uid": "WbD6CkfM4kRhVXQp8dYJNs",
        "type": "other",
        "short_message": "Some services are delayed due to roadworks.",
        "detailed_message_md": "Allow extra time for your journey while roadworks are taking place.",
        "detailed_message_rendered": "<p>Allow extra time for your journey while roadworks are taking place.</p>",
        "preview": false,
        "start_time": "2026-10-03T09:00:00Z",
        "end_time": "2026-10-03T17:00:00Z",
        "update_time": "2026-10-03T08:45:00Z",
        "location_id": 1000,
        "link": "https://example.com/travel-update",
        "link_label": "More information"
    }

}
Get Service Update Summary
get/v1/service-updates/summary/

Return a short summary of the current public service update. With no current update, type is none and short_message is absent. With an update, type is custom and short_message contains its summary.
Authorizations:
NoneCognitoJWT
Responses
200

Current public service update summary. 
