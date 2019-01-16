package uk.gov.justice.digital.hmpps.cattool.mockapis

import com.github.tomakehurst.wiremock.junit.WireMockRule
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import uk.gov.justice.digital.hmpps.cattool.mockapis.mockResponses.*
import uk.gov.justice.digital.hmpps.cattool.model.Caseload
import uk.gov.justice.digital.hmpps.cattool.model.UserAccount

import java.time.format.DateTimeFormatter

import static com.github.tomakehurst.wiremock.client.WireMock.*

class Elite2Api extends WireMockRule {

  Elite2Api() {
    super(8080)
  }

  void stubUpdateActiveCaseload() {
    this.stubFor(
      put('/api/users/me/activeCaseLoad')
        .willReturn(
        aResponse()
          .withStatus(200)
      ))
  }

  void stubGetMyDetails(UserAccount user) {
    stubGetMyDetails(user, Caseload.LEI.id)
  }

  void stubGetMyDetails(UserAccount user, String caseloadId) {
    this.stubFor(
      get('/api/users/me')
        .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader('Content-Type', 'application/json')
          .withBody(JsonOutput.toJson([
          staffId         : user.staffMember.id,
          username        : user.username,
          firstName       : user.staffMember.firstName,
          lastName        : user.staffMember.lastName,
          email           : 'itaguser@syscon.net',
          activeCaseLoadId: caseloadId
        ]))))
  }

  void stubGetMyCaseloads(List<Caseload> caseloads) {
    def json = new JsonBuilder()
    json caseloads, { caseload ->
      caseLoadId caseload.id
      description caseload.description
      type caseload.type
      caseloadFunction 'DUMMY'
    }

    this.stubFor(
      get('/api/users/me/caseLoads')
        .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader('Content-Type', 'application/json')
          .withBody(json.toString())
      ))
  }

  void stubHealth() {
    this.stubFor(
      get('/health')
        .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader('Content-Type', 'application/json')
          .withBody('''
                {
                    "status": "UP",
                    "healthInfo": {
                        "status": "UP",
                        "version": "version not available"
                    },
                    "diskSpace": {
                        "status": "UP",
                        "total": 510923390976,
                        "free": 143828922368,
                        "threshold": 10485760
                    },
                    "db": {
                        "status": "UP",
                        "database": "HSQL Database Engine",
                        "hello": 1
                    }
                }'''.stripIndent())
      ))
  }

  void stubUncategorised() {
    this.stubFor(
      get("/api/offender-assessments/category/LEI/uncategorised")
        .willReturn(
        aResponse()
          .withBody(JsonOutput.toJson([
          [
            "bookingId" : 11,
            "offenderNo": "B2345XY",
            firstName   : 'PENELOPE',
            lastName    : 'PITSTOP',
            status      : 'UNCATEGORISED',
          ],
          [
            "bookingId" : 12,
            "offenderNo": "B2346YZ",
            firstName   : 'ANT',
            lastName    : 'HILLMOB',
            status      : 'AWAITING_APPROVAL',
          ],
        ]
        ))
          .withHeader('Content-Type', 'application/json')
          .withStatus(200))
    )
  }

  def stubSentenceData(List offenderNumbers, List bookingIds, String formattedStartDate, Boolean emptyResponse = false) {
    def index = 0

    def response = emptyResponse ? [] : offenderNumbers.collect({ no ->
      [
        "offenderNo"    : no,
        "firstName"     : "firstName-${index}",
        "lastName"      : "lastName-${index}",
        "sentenceDetail": [bookingId: bookingIds[index++],
                           sentenceStartDate: formattedStartDate]
      ]
    })

    this.stubFor(
      post("/api/offender-sentences/bookings")
        .withRequestBody(equalToJson(JsonOutput.toJson(bookingIds), true, false))
        .willReturn(
        aResponse()
          .withBody(JsonOutput.toJson(response))
          .withHeader('Content-Type', 'application/json')
          .withStatus(200))
    )
  }


  def stubAlerts(List offenderNumbers, Boolean emptyResponse = false) {
    this.stubFor(
      post("/api/bookings/offenderNo/LEI/alerts")
        .withRequestBody(equalToJson(JsonOutput.toJson(offenderNumbers), true, false))
        .willReturn(
        aResponse()
          .withBody(emptyResponse ? JsonOutput.toJson([]) : HouseblockResponse.alertsResponse)
          .withHeader('Content-Type', 'application/json')
          .withStatus(200)))
  }

  def stubSystemAccessAlerts(List offenderNumbers, Boolean emptyResponse = false) {
    this.stubFor(
      post("/api/bookings/offenderNo/alerts")
        .withRequestBody(equalToJson(JsonOutput.toJson(offenderNumbers), true, false))
        .willReturn(
        aResponse()
          .withBody(emptyResponse ? JsonOutput.toJson([]) : HouseblockResponse.alertsResponse)
          .withHeader('Content-Type', 'application/json')
          .withStatus(200)))
  }

  def stubAssessments(List offenderNumbers, Boolean emptyResponse = false) {
    this.stubFor(
      post("/api/offender-assessments/CATEGORY")
        .withRequestBody(equalToJson(JsonOutput.toJson(offenderNumbers), true, false))
        .willReturn(
        aResponse()
          .withBody(emptyResponse ? JsonOutput.toJson([]) : HouseblockResponse.assessmentsResponse)
          .withHeader('Content-Type', 'application/json')
          .withStatus(200)))
  }

  void stubImage() {
    this.stubFor(
      get(urlMatching("/api/bookings/offenderNo/.+/image/data"))
        .willReturn(aResponse()
        .withStatus(404)))
  }
}