package uk.gov.justice.digital.hmpps.cattool.pages.recat

import uk.gov.justice.digital.hmpps.cattool.pages.ApprovedViewPage

class ApprovedViewRecatPage extends ApprovedViewPage {

  static url = '/form/recat/approvedView'

  static at = {
    headingText == 'Categorisation review outcome'
  }
}
