package uk.gov.justice.digital.hmpps.cattool.specs

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import geb.spock.GebReportingSpec
import org.junit.Rule
import uk.gov.justice.digital.hmpps.cattool.mockapis.Elite2Api
import uk.gov.justice.digital.hmpps.cattool.mockapis.OauthApi
import uk.gov.justice.digital.hmpps.cattool.mockapis.RiskProfilerApi
import uk.gov.justice.digital.hmpps.cattool.model.DatabaseUtils
import uk.gov.justice.digital.hmpps.cattool.model.TestFixture
import uk.gov.justice.digital.hmpps.cattool.pages.*
import uk.gov.justice.digital.hmpps.cattool.pages.recat.ApprovedViewRecatPage

import static uk.gov.justice.digital.hmpps.cattool.model.UserAccount.*

class LandingPageSpecification extends GebReportingSpec {

  def setup() {
    db.clearDb()
  }

  @Rule
  Elite2Api elite2Api = new Elite2Api()

  @Rule
  RiskProfilerApi riskProfilerApi = new RiskProfilerApi()

  @Rule
  OauthApi oauthApi = new OauthApi(new WireMockConfiguration()
    .extensions(new ResponseTemplateTransformer(false)))

  TestFixture fixture = new TestFixture(browser, elite2Api, oauthApi, riskProfilerApi)
  DatabaseUtils db = new DatabaseUtils()

  def "A recategoriser user can start a recat from the landing page"() {

    given: 'A recategoriser is logged in'
    elite2Api.stubRecategorise()
    fixture.loginAs(RECATEGORISER_USER)

    when: 'The user arrives at the landing page'
    elite2Api.stubGetOffenderDetails(12)
    elite2Api.stubGetCategory(12, 'C')
    go '/12'

    then: 'The page contains a recat button'
    at LandingPage
    startButton.displayed
    startButton.@href.contains('/tasklistRecat/12?reason=MANUAL')

    when: 'It is clicked'
    riskProfilerApi.stubGetSocProfile('B2345YZ', 'C', false)
    startButton.click()

    then: 'We are sent to the recat tasklist'
    at TasklistRecatPage
    currentUrl.contains '/tasklistRecat/12'
    def data = db.getData(12)
    data.status == ["STARTED"]
    data.review_reason.value == ["MANUAL"]
  }

  def "A recategoriser user sees a warning for initial cat"() {

    given: 'A recategoriser is logged in'
    elite2Api.stubRecategorise()
    fixture.loginAs(RECATEGORISER_USER)

    when: 'The user arrives at the landing page'
    elite2Api.stubGetOffenderDetails(12, 'B2345YZ', false, false, 'U')
    elite2Api.stubGetCategory(12, 'U')
    go '/12'

    then: 'The page contains an initial cat warning'
    at LandingPage
    !startButton.displayed
    warning.text() contains 'This prisoner seems to need an INITIAL category'
  }

  def "A recategoriser user sees a warning for cat A"() {

    given: 'A recategoriser is logged in'
    elite2Api.stubRecategorise()
    fixture.loginAs(RECATEGORISER_USER)

    when: 'The user arrives at the landing page'
    elite2Api.stubGetOffenderDetails(12, 'B2345YZ', false, false, 'A')
    elite2Api.stubGetCategory(12, 'A')
    go '/12'

    then: 'The page contains a warning'
    at LandingPage
    !startButton.displayed
    warning.text() contains 'This prisoner is Cat A. They cannot be categorised here.'
  }

  def "A basic user can view previous categorisations if prisoner is in their prison"() {
    db.createData(12, '{}'); // should get ignored
    db.doCreateCompleteRow(-2, 12, '{"supervisor": {"review": {"proposedCategory": "B"}}}', 'CATEGORISER_USER', 'APPROVED', 'INITIAL', null, null, null,
      2, '{}', 'LEI', 'B2345YZ', 'current_timestamp(2)', null, null, '2019-07-29')
    db.doCreateCompleteRow(-3, 12, '{"supervisor": {"review": {"supervisorOverriddenCategory": "C"}}}', 'RECATEGORISER_USER', 'APPROVED', 'RECAT', null, null, null,
      3, '{}', 'BXI', 'B2345YZ', 'current_timestamp(2)', null, null, '2019-08-05')
    db.doCreateCompleteRow(-4, 12,
      '{"recat": {"decision": {"category": "D"}}, "supervisor": {"review": {"proposedCategory": "D", "supervisorCategoryAppropriate": "Yes"}}}',
      'RECATEGORISER_USER', 'APPROVED', 'RECAT', null, null, null,
      4, '{}', 'LPI', 'B2345YZ', 'current_timestamp(2)', null, null, '2019-08-29')

    given: 'a basic user is logged in'
    fixture.loginAs(READONLY_USER)

    when: 'the user arrives at the landing page and clicks the link to check previous reviews'
    elite2Api.stubGetOffenderDetails(12)
    elite2Api.stubGetBasicOffenderDetails(12)
    elite2Api.stubGetCategory(12, 'C')
    go '/12'
    at LandingPage
    elite2Api.stubAgencyDetails('BXI')
    elite2Api.stubAgencyDetails('LEI')
    elite2Api.stubAgencyDetails('LPI')
    historyButton.click()

    then: 'The previous category reviews page is displayed correctly'
    at CategoryHistoryPage
    rows[0].find('td')*.text() == ['29/07/2019', 'B', 'LEI prison', 'View (opens in new tab)']
    rows[1].find('td')*.text() == ['05/08/2019', 'C', 'BXI prison', 'View (opens in new tab)']
    rows[2].find('td')*.text() == ['29/08/2019', 'D', 'LPI prison', 'View (opens in new tab)']
    rows[0].find('td > a').@href.contains '/form/approvedView/12?sequenceNo=2'

    when: 'the user selects a review'
    elite2Api.stubAssessments(['B2345YZ'])
    rows[2].find('td > a').click()

    then: 'the approved view page is shown'
    at ApprovedViewRecatPage
  }
}
