package uk.gov.justice.digital.hmpps.cattool.pages

import geb.Page

class ApprovedViewPage extends Page {

  static url = '/form/approvedView'

  static at = {
    headingText == 'Categorisation outcome'
  }

  static content = {
    headingText { $('h1.govuk-heading-l').text() }
    categories { $('.govuk-warning-text') }
    comments(required: false) { $('.forms-comments-text') }
    errorSummaries(required: false) { $('ul.govuk-error-summary__list li') }
    errors(required: false) { $('span.govuk-error-message') }

    submitButton { $('a', role:'button') }
    backLink { $('a.govuk-back-link') }
  }
}