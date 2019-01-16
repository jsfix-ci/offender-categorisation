package uk.gov.justice.digital.hmpps.cattool.pages

import geb.Page

class CategoriserHomePage extends Page {

  static url = '/'

  static at = {
    headingText == 'Prisoner Categorisation'
  }

  static content = {
    headingText { $('h1').text() }
    bodyRows(required: false) { $('tr.govuk-table__row', 1..-1) }
    names { bodyRows*.$('td', 0)*.text() }
    prisonNos { bodyRows*.$('td', 1)*.text()  }
    days { bodyRows*.$('td', 2)*.text()  }
    dates { bodyRows*.$('td', 3)*.text()  }
    statuses { bodyRows*.$('td', 4)*.text()  }
    logoutLink { $('a', href: '/logout')}
  }

  def logout() {
    logoutLink.click()
  }
}