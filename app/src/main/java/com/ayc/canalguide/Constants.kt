package com.ayc.canalguide

/**
 * Constant values that won't be used in XML
 */
object Constants {
    const val BASE_URL = "http://www.canals.ny.gov/xml/"
    val navInfoRegions = listOf(
        "hudsonriver", "champlain", "fortedward", "erieeastern", "frankfortharbor",
        "uticaharbor", "oswego", "eriecentral", "onondagalake", "cayugaseneca",
        "cayugalake", "senecalake", "eriewestern", "geneseeriver", "ellicottcreek"
    )
    // These regions flow east to west
    val navInfoRegionsEastWestWaterflow = arrayOf(
            navInfoRegions.indexOf("fortedward"),
            navInfoRegions.indexOf("eriewestern"),
            navInfoRegions.indexOf("erieeastern"),
            navInfoRegions.indexOf("eriecentral")
    )
}