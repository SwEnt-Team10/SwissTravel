package com.github.swent.swisstravel.model.trainstimetable

import com.github.swent.swisstravel.model.trip.Location
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Creates an XML request body for the OJP (Open Journey Planner) TripRequest endpoint.
 *
 * This function generates a complete OJP XML structure as a [String]. The request specifies the
 * origin and destination by their geographic coordinates, requests the single fastest route, and
 * includes parameters to get detailed leg information.
 *
 * This function was written with the help of Gemini.
 *
 * @param from The starting [Location] of the trip.
 * @param to The destination [Location] of the trip.
 * @param requestorRef A reference string to identify the application making the request.
 * @return A [String] containing the formatted XML for the OJP TripRequest.
 */
fun createOjpTripRequest(
    from: Location,
    to: Location,
    requestorRef: String = "swisstravel-app"
): String {
  val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  return """
        <?xml version="1.0" encoding="UTF-8"?>
        <OJP xmlns="http://www.vdv.de/ojp" xmlns:siri="http://www.siri.org.uk/siri" version="2.0">
            <OJPRequest>
                <siri:ServiceRequest>
                    <siri:RequestTimestamp>$now</siri:RequestTimestamp>
                    <siri:RequestorRef>$requestorRef</siri:RequestorRef>
                    <OJPTripRequest>
                        <siri:RequestTimestamp>$now</siri:RequestTimestamp>
                        <siri:MessageIdentifier>msg-$now</siri:MessageIdentifier>
                        <Origin>
                            <PlaceRef>
                                <GeoPosition>
                                    <siri:Longitude>${from.coordinate.longitude}</siri:Longitude>
                                    <siri:Latitude>${from.coordinate.latitude}</siri:Latitude>
                                </GeoPosition>
                            </PlaceRef>
                            <DepArrTime>$now</DepArrTime>
                        </Origin>
                        <Destination>
                            <PlaceRef>
                                <GeoPosition>
                                    <siri:Longitude>${to.coordinate.longitude}</siri:Longitude>
                                    <siri:Latitude>${to.coordinate.latitude}</siri:Latitude>
                                </GeoPosition>
                            </PlaceRef>
                        </Destination>
                        <Params>
                            <NumberOfResults>1</NumberOfResults>
                            <UseRealtimeData>none</UseRealtimeData>
                            <IncludeIntermediateStops>false</IncludeIntermediateStops>
                        </Params>
                    </OJPTripRequest>
                </siri:ServiceRequest>
            </OJPRequest>
        </OJP>
    """
      .trimIndent()
}
