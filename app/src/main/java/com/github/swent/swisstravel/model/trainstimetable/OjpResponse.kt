package com.github.swent.swisstravel.model.trainstimetable

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

/**
 * Data classes for parsing the XML response from the OJP (Open Journey Planner) API. These classes
 * are structured to match the OJP 2.0 XML schema and are used by SimpleXML to deserialize the
 * response into Kotlin objects.
 *
 * This file was written with the help of Gemini.
 */

// Define namespaces for clarity
private const val OJP_NS = "http://www.vdv.de/ojp"
private const val SIRI_NS = "http://www.siri.org.uk/siri"

/**
 * The root element of the OJP API response.
 *
 * @property ojpDeliver The main delivery content of the response.
 */
@Root(name = "OJP", strict = false)
@Namespace(reference = OJP_NS) // Add default namespace
data class OjpResponse(
    @field:Element(name = "OJPResponse") var ojpResponse: OJPResponseElement? = null
)

@Root(name = "OJPResponse", strict = false)
@Namespace(reference = OJP_NS)
data class OJPResponseElement(
    @field:Element(name = "ServiceDelivery")
    @field:Namespace(prefix = "siri", reference = SIRI_NS) // Add SIRI namespace
    var serviceDelivery: ServiceDelivery? = null
)

@Root(name = "ServiceDelivery", strict = false)
@Namespace(prefix = "siri", reference = SIRI_NS)
data class ServiceDelivery(
    @field:Element(name = "OJPTripDelivery")
    @field:Namespace(reference = OJP_NS) // Switch back to OJP namespace
    var ojpTripDelivery: OjpTripDelivery? = null
)

/**
 * Contains a list of trip results.
 *
 * @property tripResults A list of possible trips that match the request.
 */
@Root(name = "OJPTripDelivery", strict = false)
@Namespace(reference = OJP_NS)
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
@Namespace(reference = OJP_NS)
data class TripResult(@field:Element(name = "Trip") var trip: Trip? = null)

/**
 * Represents a complete trip, composed of one or more legs.
 *
 * @property tripLegs A list of [TripLeg] objects that make up the trip.
 */
@Root(name = "Trip", strict = false)
@Namespace(reference = OJP_NS)
data class Trip(
    @field:ElementList(inline = true, name = "TripLeg", required = false)
    var tripLegs: List<TripLeg>? = null,
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
@Namespace(reference = OJP_NS)
data class TripLeg(
    @field:Element(name = "LegStart", required = false) var legStart: LegStop? = null,
    @field:Element(name = "LegEnd", required = false) var legEnd: LegStop? = null,
    @field:Element(name = "Duration", required = false) var duration: String? = null,
    @field:Element(name = "Service", required = false) var service: Service? = null
)

/** Represents a stop (departure or arrival) in a trip leg. */
@Root(name = "LegStop", strict = false)
@Namespace(reference = OJP_NS)
data class LegStop(
    @field:Element(name = "Location") var location: LocationElement? = null,
    @field:Element(name = "DepArrTime") var depArrTime: String? = null
)

/**
 * Contains information about the transport service used.
 *
 * @property mode The mode of transport (e.g., train, bus).
 */
@Root(name = "Service", strict = false)
@Namespace(reference = OJP_NS)
data class Service(@field:Element(name = "Mode") var mode: Mode? = null)

/**
 * Describes the mode of transport.
 *
 * @property name The name of the transport mode (e.g., "Train").
 */
@Root(name = "Mode", strict = false)
@Namespace(reference = OJP_NS)
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
@Namespace(reference = OJP_NS)
data class GeoPosition(
    @field:Element(name = "Longitude")
    @field:Namespace(reference = SIRI_NS)
    var longitude: Double? = null,
    @field:Element(name = "Latitude")
    @field:Namespace(reference = SIRI_NS)
    var latitude: Double? = null
)

/** Represents the <Location> wrapper element found inside a <LegStop>. */
@Root(name = "Location", strict = false)
@Namespace(reference = OJP_NS)
data class LocationElement(
    @field:Element(name = "LocationName") var locationName: TextElement? = null,
    @field:Element(name = "GeoPosition", required = false) var geoPosition: GeoPosition? = null
)

/**
 * A generic wrapper for text elements in the XML response.
 *
 * @property text The actual text content.
 */
@Root(strict = false)
data class TextElement(
    @field:Element(name = "Text") @field:Namespace(reference = OJP_NS) var text: String? = null
)
