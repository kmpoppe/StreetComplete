package de.westnordost.streetcomplete.data.user.statistics

import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val STATISTICS_BACKEND_URL = "https://streetcomplete.app/statistics/"
val statisticsModule = module {
    factory(named("EditTypeStatistics")) { EditTypeStatisticsDao(get(), EditTypeStatisticsTable.NAME) }
    factory(named("CountryStatistics")) { CountryStatisticsDao(get(), CountryStatisticsTable.NAME) }

    factory(named("EditTypeStatisticsCurrentWeek")) { EditTypeStatisticsDao(get(), EditTypeStatisticsTable.NAME_CURRENT_WEEK) }
    factory(named("CountryStatisticsCurrentWeek")) { CountryStatisticsDao(get(), CountryStatisticsTable.NAME_CURRENT_WEEK) }

    factory { ActiveDatesDao(get()) }

    factory { StatisticsApiClient(get(), STATISTICS_BACKEND_URL, get()) }
    factory { StatisticsParser(get(named("TypeAliases"))) }

    single<StatisticsSource> { get<StatisticsController>() }
    single { StatisticsController(
        editTypeStatisticsDao = get(named("EditTypeStatistics")),
        countryStatisticsDao = get(named("CountryStatistics")),
        currentWeekEditTypeStatisticsDao = get(named("EditTypeStatisticsCurrentWeek")),
        currentWeekCountryStatisticsDao = get(named("CountryStatisticsCurrentWeek")),
        activeDatesDao = get(),
        countryBoundaries = get(named("CountryBoundariesLazy")),
        prefs = get(),
        userLoginSource = get(),
    ) }
}
