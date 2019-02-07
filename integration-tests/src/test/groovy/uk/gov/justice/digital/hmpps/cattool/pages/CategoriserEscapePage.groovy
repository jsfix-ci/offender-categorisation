package uk.gov.justice.digital.hmpps.cattool.pages

import geb.Page

class CategoriserEscapePage extends Page {

  static String bookingId

  static url = '/form/ratings/escapeRating/' + bookingId

  static at = {
    headingText == 'Risk of escape'
  }

  static content = {
    headingText { $('h1.govuk-heading-l').text() }
    headerBlock { $('div.govuk-body-s') }
    headerValue { headerBlock.$('p.govuk-\\!-font-weight-bold') }
    warningTextDiv { $('div.govuk-warning-text')}
    saveButton { $('button.govuk-button') }
    radio { $('input', name: 'escapeFurtherCharges') }
  }
}