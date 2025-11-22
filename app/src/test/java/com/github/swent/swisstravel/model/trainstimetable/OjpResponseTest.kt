package com.github.swent.swisstravel.model.trainstimetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.simpleframework.xml.core.Persister

class OjpResponseTest {

  private lateinit var serializer: Persister
  private lateinit var sampleXmlResponse: String

  @Before
  fun setUp() {
    serializer = Persister()
    sampleXmlResponse =
        """
            <OJP xmlns:siri="http://www.siri.org.uk/siri" xmlns="http://www.vdv.de/ojp" version="2.0">
                <OJPResponse>
                    <siri:ServiceDelivery>
                        <OJPTripDelivery>
                            <TripResult>
                                <Trip>
                                    <Duration>PT1H28M</Duration>
                                    <TripLeg>
                                        <LegStart>
                                            <Location>
                                                <LocationName>
                                                    <Text>Lausanne</Text>
                                                </LocationName>
                                                <GeoPosition>
                                                    <siri:Longitude>6.63227</siri:Longitude>
                                                    <siri:Latitude>46.51965</siri:Latitude>
                                                </GeoPosition>
                                            </Location>
                                            <DepArrTime>2025-11-20T10:00:00Z</DepArrTime>
                                        </LegStart>
                                        <LegEnd>
                                            <Location>
                                                <LocationName>
                                                    <Text>Geneva</Text>
                                                </LocationName>
                                                <GeoPosition>
                                                    <siri:Longitude>6.14316</siri:Longitude>
                                                    <siri:Latitude>46.20439</siri:Latitude>
                                                </GeoPosition>
                                            </Location>
                                            <DepArrTime>2025-11-20T10:35:00Z</DepArrTime>
                                        </LegEnd>
                                        <Duration>PT35M</Duration>
                                        <Service>
                                            <Mode>
                                                <Name>
                                                    <Text>Train</Text>
                                                </Name>
                                            </Mode>
                                        </Service>
                                    </TripLeg>
                                    <TripLeg>
                                        <LegStart>
                                             <Location>
                                                <LocationName>
                                                    <Text>Geneva</Text>
                                                </LocationName>
                                            </Location>
                                            <DepArrTime>2025-11-20T10:45:00Z</DepArrTime>
                                        </LegStart>
                                        <LegEnd>
                                            <Location>
                                                <LocationName>
                                                    <Text>Bern</Text>
                                                </LocationName>
                                            </Location>
                                            <DepArrTime>2025-11-20T11:28:00Z</DepArrTime>
                                        </LegEnd>
                                        <Service>
                                            <Mode>
                                                <Name>
                                                    <Text>Bus</Text>
                                                </Name>
                                            </Mode>
                                        </Service>
                                    </TripLeg>
                                </Trip>
                            </TripResult>
                        </OJPTripDelivery>
                    </siri:ServiceDelivery>
                </OJPResponse>
            </OJP>
        """
            .trimIndent()
  }

  @Test
  fun `OjpResponse is parsed correctly from XML`() {
    val ojpResponse = serializer.read(OjpResponse::class.java, sampleXmlResponse)

    assertNotNull("OjpResponse should not be null", ojpResponse)
    assertNotNull("OJPResponseElement should not be null", ojpResponse.ojpResponse)
    assertNotNull("ServiceDelivery should not be null", ojpResponse.ojpResponse?.serviceDelivery)
    assertNotNull(
        "OjpTripDelivery should not be null",
        ojpResponse.ojpResponse?.serviceDelivery?.ojpTripDelivery)
  }

  @Test
  fun `TripResults are parsed correctly`() {
    val ojpResponse = serializer.read(OjpResponse::class.java, sampleXmlResponse)
    val tripResults = ojpResponse.ojpResponse?.serviceDelivery?.ojpTripDelivery?.tripResults

    assertNotNull("TripResults list should not be null", tripResults)
    assertEquals("There should be 1 TripResult", 1, tripResults?.size)

    val trip = tripResults?.first()?.trip
    assertNotNull("Trip should not be null", trip)
    assertEquals("Total trip duration should be PT1H28M", "PT1H28M", trip?.duration)
  }

  @Test
  fun `TripLegs are parsed correctly`() {
    val ojpResponse = serializer.read(OjpResponse::class.java, sampleXmlResponse)
    val tripLegs =
        ojpResponse.ojpResponse
            ?.serviceDelivery
            ?.ojpTripDelivery
            ?.tripResults
            ?.first()
            ?.trip
            ?.tripLegs

    assertNotNull("TripLegs list should not be null", tripLegs)
    assertEquals("There should be 2 TripLegs", 2, tripLegs?.size)
  }

  @Test
  fun `First TripLeg details are parsed correctly`() {
    val ojpResponse = serializer.read(OjpResponse::class.java, sampleXmlResponse)
    val firstLeg =
        ojpResponse.ojpResponse
            ?.serviceDelivery
            ?.ojpTripDelivery
            ?.tripResults
            ?.first()
            ?.trip
            ?.tripLegs
            ?.first()

    assertNotNull("First leg should not be null", firstLeg)
    assertEquals("First leg duration should be PT35M", "PT35M", firstLeg?.duration)

    // Test LegStart
    assertEquals("Lausanne", firstLeg?.legStart?.location?.locationName?.text)
    assertEquals("2025-11-20T10:00:00Z", firstLeg?.legStart?.depArrTime)
    assertEquals(6.63227, firstLeg?.legStart?.location?.geoPosition?.longitude)
    assertEquals(46.51965, firstLeg?.legStart?.location?.geoPosition?.latitude)

    // Test LegEnd
    assertEquals("Geneva", firstLeg?.legEnd?.location?.locationName?.text)
    assertEquals("2025-11-20T10:35:00Z", firstLeg?.legEnd?.depArrTime)
    assertEquals(6.14316, firstLeg?.legEnd?.location?.geoPosition?.longitude)
    assertEquals(46.20439, firstLeg?.legEnd?.location?.geoPosition?.latitude)

    // Test Service Mode
    assertEquals("Train", firstLeg?.service?.mode?.name?.text)
  }

  @Test
  fun `Second TripLeg details are parsed correctly`() {
    val ojpResponse = serializer.read(OjpResponse::class.java, sampleXmlResponse)
    val secondLeg =
        ojpResponse.ojpResponse
            ?.serviceDelivery
            ?.ojpTripDelivery
            ?.tripResults
            ?.first()
            ?.trip
            ?.tripLegs
            ?.get(1)

    assertNotNull("Second leg should not be null", secondLeg)

    // Leg duration and GeoPosition are optional and not present in the second leg
    assertNull("Duration for the second leg should be null", secondLeg?.duration)
    assertNull(
        "GeoPosition for the second leg start should be null",
        secondLeg?.legStart?.location?.geoPosition)

    // Test LegStart and LegEnd
    assertEquals("Geneva", secondLeg?.legStart?.location?.locationName?.text)
    assertEquals("2025-11-20T10:45:00Z", secondLeg?.legStart?.depArrTime)
    assertEquals("Bern", secondLeg?.legEnd?.location?.locationName?.text)
    assertEquals("2025-11-20T11:28:00Z", secondLeg?.legEnd?.depArrTime)

    // Test Service Mode
    assertEquals("Bus", secondLeg?.service?.mode?.name?.text)
  }
}
