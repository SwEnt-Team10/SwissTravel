package com.github.swent.swisstravel.model.trainstimetable

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

/**
 * Data classes for parsing the XML response from the OJP (Open Journey Planner) API. These classes
 * are structured to match the OJP 2.0 XML schema and are used by SimpleXML to deserialize the
 * response into Kotlin objects.
 *
 * This file was written with the help of Gemini.
 */

/**
 * The root element of the OJP API response.
 *
 * @property ojpDeliver The main delivery content of the response.
 */
@Root(name = "OJP", strict = false)
data class OjpResponse(@field:Element(name = "OJPDeliver") var ojpDeliver: OjpDeliver? = null)

/**
 * Contains the actual trip data delivery.
 *
 * @property ojpTripDelivery The delivery payload containing trip results.
 */
@Root(name = "OJPDeliver", strict = false)
data class OjpDeliver(
    @field:Element(name = "OJPTripDelivery") var ojpTripDelivery: OjpTripDelivery? = null
)

/**
 * Contains a list of trip results.
 *
 * @property tripResults A list of possible trips that match the request.
 */
@Root(name = "OJPTripDelivery", strict = false)
data class OjpTripDelivery(
    @field:ElementList(inline = true, name = "TripResult", required = false)
    var tripResults: List<TripResult>? = null
)

/**
 * Represents a single trip result.
 *
 * @property trip The detailed trip information.
 */
@Root(name = "TripResult", strict = false)
data class TripResult(@field:Element(name = "Trip") var trip: Trip? = null)

/**
 * Represents a complete trip, composed of one or more legs.
 *
 * @property tripLegs A list of [TripLeg] objects that make up the trip.
 */
@Root(name = "Trip", strict = false)
data class Trip(
    @field:ElementList(inline = true, name = "TripLeg") var tripLegs: List<TripLeg>? = null,
    @field:Element(name = "Duration", required = false)
    var duration: String? = null // The total duration of the trip
)

/**
 * Represents a single leg of a trip, e.g., a single train or bus ride.
 *
 * @property legStart The starting point of the leg.
 * @property legEnd The ending point of the leg.
 * @property duration The duration of this leg in ISO 8601 format.
 * @property service Information about the transport service for this leg.
 */
@Root(name = "TripLeg", strict = false)
data class TripLeg(
    @field:Element(name = "LegStart") var legStart: LegStop? = null,
    @field:Element(name = "LegEnd") var legEnd: LegStop? = null,
    @field:Element(name = "Duration") var duration: String? = null, // ISO 8601 format e.g. PT1H30M
    @field:Element(name = "Service", required = false) var service: Service? = null
)

/**
 * Represents a stop (departure or arrival) in a trip leg.
 *
 * @property locationName The name of the stop location.
 * @property geoPosition The geographic coordinates of the stop.
 * @property depArrTime The departure or arrival time in ISO 8601 format.
 */
@Root(name = "LegStop", strict = false)
data class LegStop(
    @field:Element(name = "LocationName") var locationName: TextElement? = null,
    @field:Element(name = "GeoPosition", required = false) var geoPosition: GeoPosition? = null,
    @field:Element(name = "DepArrTime") var depArrTime: String? = null
)

/**
 * Contains information about the transport service used.
 *
 * @property mode The mode of transport (e.g., train, bus).
 */
@Root(name = "Service", strict = false)
data class Service(@field:Element(name = "Mode") var mode: Mode? = null)

/**
 * Describes the mode of transport.
 *
 * @property name The name of the transport mode (e.g., "Train").
 */
@Root(name = "Mode", strict = false)
data class Mode(
    @field:Element(name = "Name") var name: TextElement? = null // e.g., "Train"
)

/**
 * Represents geographic coordinates.
 *
 * @property longitude The longitude value.
 * @property latitude The latitude value.
 */
@Root(name = "GeoPosition", strict = false)
data class GeoPosition(
    @field:Element(name = "Longitude") var longitude: Double = 0.0,
    @field:Element(name = "Latitude") var latitude: Double = 0.0
)

/**
 * A generic wrapper for text elements in the XML response.
 *
 * @property text The actual text content.
 */
@Root(strict = false)
data class TextElement(@field:Element(name = "Text") var text: String? = null)
