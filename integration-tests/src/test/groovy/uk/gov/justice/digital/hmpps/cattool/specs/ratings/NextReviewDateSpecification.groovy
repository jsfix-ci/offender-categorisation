package uk.gov.justice.digital.hmpps.cattool.specs.ratings


import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import uk.gov.justice.digital.hmpps.cattool.model.TestFixture
import uk.gov.justice.digital.hmpps.cattool.pages.*
import uk.gov.justice.digital.hmpps.cattool.specs.AbstractSpecification

import java.time.LocalDate

import static uk.gov.justice.digital.hmpps.cattool.model.UserAccount.CATEGORISER_USER
import static uk.gov.justice.digital.hmpps.cattool.model.UserAccount.SUPERVISOR_USER

class NextReviewDateSpecification extends AbstractSpecification {

  def setup() {
    elite2Api.stubAgencyDetails('LPI')
    elite2Api.stubAssessments('B2345YZ')
  }

  static final SIX_MONTHS_AHEAD = LocalDate.now().plusMonths(6).format('dd/MM/yyyy')
  static final THREE_MONTHS_AHEAD = LocalDate.now().plusMonths(3).format('dd/MM/yyyy')
  static final THREE_MONTHS_AHEAD_ISO = LocalDate.now().plusMonths(3).format('yyyy-MM-dd')

  def "The nextReviewDate page saves details correctly - 6 months"() {
    when: 'I go to the Next Review Date Question page'
    fixture.gotoTasklist(false)
    at TasklistPage
    nextReviewDateButton.click()

    then: 'The page is displayed'
    at NextReviewDateQuestionPage
    headerValue*.text() == fixture.MINI_HEADER

    when: "6 months is selected"
    sixMonthsOption.click()
    submitButton.click()

    then: "I continue to the Next Review Date page"
    at NextReviewDatePage
    reviewDate.value() == SIX_MONTHS_AHEAD

    when: 'Populated date is used, saved and accessed'
    submitButton.click()
    at TasklistPage
    nextReviewDateButton.click()
    at NextReviewDateEditingPage

    then: "data is correctly populated"
    chosenDate.text() == SIX_MONTHS_AHEAD

    when: "we decide to change the data"
    changeLink.click()
    at NextReviewDateQuestionPage
    specificOption.click()
    submitButton.click()
    at NextReviewDatePage

    then: "data is correctly blanked"
    form.date == ''

    def data = db.getData(12)
    def response = new JsonSlurper().parseText(data.form_response[0].toString())
    data.status == ['STARTED']
    data.cat_type == ['INITIAL']
    response.ratings == [nextReviewDate: [date: SIX_MONTHS_AHEAD]]
    data.user_id == ['CATEGORISER_USER']
    data.assigned_user_id == ['CATEGORISER_USER']
  }

  def "Validation"() {
    when: 'I go to the Next Review Date Question page'
    fixture.gotoTasklist(false)
    at TasklistPage
    nextReviewDateButton.click()

    then: 'The page is displayed'
    at NextReviewDateQuestionPage
    headerValue*.text() == fixture.MINI_HEADER

    when: 'Nothing is selected - validation kicks in'
    submitButton.click()
    at NextReviewDateQuestionPage

    then: "Error is displayed"
    errorSummaries*.text() == ['Please select a choice']
    errors*.text() == ['Error:\nPlease select a choice']

    when: "specific date is selected"
    specificOption.click()
    submitButton.click()

    then: "I continue to the Next Review Date page"
    at NextReviewDatePage
    reviewDate.value() == ''

    when: 'No date is entered - validation kicks in'
    submitButton.click()
    at NextReviewDatePage

    then: "Error is displayed"
    errorSummaries*.text() == ['Enter a valid date that is after today']
    errors*.text() == ['Error:\nEnter a valid date that is after today']
  }

  def "The nextReviewDate Standalone page saves details correctly - in PG"() {

    given: 'there is an approved db record'
    db.createDataWithStatusAndCatType(12, 'APPROVED',
      JsonOutput.toJson([ratings: TestFixture.defaultRatingsC]),
      'INITIAL', 'B2345YZ');

    when: 'I go to the Next Review Date Standalone page'
    elite2Api.stubUncategorised()
    elite2Api.stubSentenceData(['B2345XY', 'B2345YZ'], [11, 12], [LocalDate.now().toString(), LocalDate.now().toString()])
    fixture.loginAs(CATEGORISER_USER)
    browser.at CategoriserHomePage
    elite2Api.stubGetOffenderDetails(12, 'B2345YZ')
    go '/12'
    at LandingPage
    nextReviewDateButton.click()

    then: 'The page is displayed with db date'
    at NextReviewDateStandalonePage
    reviewDate.value() == '14/12/2019'

    when: 'date is modified'
    elite2Api.stubUpdateNextReviewDate(THREE_MONTHS_AHEAD_ISO)
    reviewDate = THREE_MONTHS_AHEAD
    submitButton.click()

    then: "we proceed to the confirmed page, the endpoint was called and database has been updated"
    at NextReviewDateStandaloneConfirmedPage
    elite2Api.verifyUpdateNextReviewDate(THREE_MONTHS_AHEAD_ISO) == null
    def data = db.getData(12)
    def response = new JsonSlurper().parseText(data.form_response[0].toString())
    response.ratings.nextReviewDate == [date: THREE_MONTHS_AHEAD]
  }

  def "The nextReviewDate Standalone page saves details correctly - not in PG"() {
    when: 'I go to the Next Review Date Standalone page'
    elite2Api.stubUncategorised()
    elite2Api.stubSentenceData(['B2345XY', 'B2345YZ'], [11, 12], [LocalDate.now().toString(), LocalDate.now().toString()])
    fixture.loginAs(CATEGORISER_USER)
    browser.at CategoriserHomePage
    elite2Api.stubGetOffenderDetails(12, 'B2345YZ')
    go '/12'
    at LandingPage
    nextReviewDateButton.click()

    then: 'The page is displayed with existing date'
    at NextReviewDateStandalonePage
    reviewDate.value() == '16/01/2020'

    when: 'date is modified'
    elite2Api.stubUpdateNextReviewDate(THREE_MONTHS_AHEAD_ISO)
    reviewDate = THREE_MONTHS_AHEAD
    submitButton.click()

    then: "we proceed to the confirmed page, the endpoint was called and no data exists"
    at NextReviewDateStandaloneConfirmedPage
    elite2Api.verifyUpdateNextReviewDate(THREE_MONTHS_AHEAD_ISO) == null
    db.getData(12).empty
  }

  def "The nextReviewDate Standalone page saves details correctly - in progress"() {

    given: 'there is an in-progress db record'
    db.createDataWithStatusAndCatType(12, 'AWAITING_APPROVAL','{}',
      'INITIAL', 'B2345YZ');

    when: 'I go to the landing page'
    elite2Api.stubUncategorised()
    elite2Api.stubSentenceData(['B2345XY', 'B2345YZ'], [11, 12], [LocalDate.now().toString(), LocalDate.now().toString()])
    fixture.loginAs(CATEGORISER_USER)
    browser.at CategoriserHomePage
    elite2Api.stubGetOffenderDetails(12, 'B2345YZ')
    go '/12'
    at LandingPage

    then: 'the next review button is not present'
    !nextReviewDateButton.displayed

    when: 'I force the next review page'
    via NextReviewDateStandalonePage, '12'

    then: 'The error page is displayed'
    at ErrorPage
    errorSummaryTitle.text() == 'Categorisation is in progress: please use the tasklist to change date'
  }

  def "The nextReviewDate Standalone page saves details correctly - in PG, supervisor"() {

    given: 'there is an approved db record'
    db.createDataWithStatusAndCatType(12, 'APPROVED',
      JsonOutput.toJson([ratings: TestFixture.defaultRatingsC]),
      'INITIAL', 'B2345YZ');

    when: 'a supervisor goes to the Next Review Date Standalone page'
    elite2Api.stubUncategorisedAwaitingApproval()
    elite2Api.stubSentenceData(['B2345XY', 'B2345YZ'], [11, 12], [LocalDate.now().toString(), LocalDate.now().toString()])
    fixture.loginAs(SUPERVISOR_USER)
    at SupervisorHomePage
    elite2Api.stubGetOffenderDetails(12, 'B2345YZ')
    go '/12'
    at LandingPage
    nextReviewDateButton.click()

    then: 'The page is displayed with db date'
    at NextReviewDateStandalonePage
    reviewDate.value() == '14/12/2019'

    when: 'date is modified'
    elite2Api.stubUpdateNextReviewDate(THREE_MONTHS_AHEAD_ISO)
    reviewDate = THREE_MONTHS_AHEAD
    submitButton.click()

    then: "we proceed to the confirmed page, the endpoint was called and database has been updated"
    at NextReviewDateStandaloneConfirmedPage
    elite2Api.verifyUpdateNextReviewDate(THREE_MONTHS_AHEAD_ISO) == null
    def data = db.getData(12)
    def response = new JsonSlurper().parseText(data.form_response[0].toString())
    response.ratings.nextReviewDate == [date: THREE_MONTHS_AHEAD]
  }
}
