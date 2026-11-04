package com.example.problemfinder.domain.opportunities.impl.di

import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.impl.details.ObserveOpportunityDetailsUseCaseImpl
import com.example.problemfinder.domain.opportunities.api.details.RefreshOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.impl.details.RefreshOpportunityDetailsUseCaseImpl
import com.example.problemfinder.domain.opportunities.api.details.LoadNextOpportunityEvidenceUseCase
import com.example.problemfinder.domain.opportunities.impl.details.LoadNextOpportunityEvidenceUseCaseImpl
import com.example.problemfinder.domain.opportunities.api.search.ObserveOpportunitySearchUseCase
import com.example.problemfinder.domain.opportunities.impl.search.ObserveOpportunitySearchUseCaseImpl
import com.example.problemfinder.domain.opportunities.api.search.RefreshOpportunitySearchUseCase
import com.example.problemfinder.domain.opportunities.impl.search.RefreshOpportunitySearchUseCaseImpl
import com.example.problemfinder.domain.opportunities.api.search.LoadNextOpportunitySearchPageUseCase
import com.example.problemfinder.domain.opportunities.impl.search.LoadNextOpportunitySearchPageUseCaseImpl
import com.example.problemfinder.domain.opportunities.api.search.PrepareOpportunitySearchDraftUseCase
import com.example.problemfinder.domain.opportunities.impl.search.PrepareOpportunitySearchDraftUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Step 4 wiring is complete. Neither feature needs an implementation-module import. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GuidedFeaturesDomainModule {
    @Binds abstract fun bindLoadPost(
        implementation: com.example.problemfinder.domain.opportunities.impl.details.LoadOpportunityPostUseCaseImpl,
    ): com.example.problemfinder.domain.opportunities.api.details.LoadOpportunityPostUseCase

    @Binds abstract fun bindSearchOptions(
        implementation: com.example.problemfinder.domain.opportunities.impl.search.LoadOpportunitySearchOptionsUseCaseImpl,
    ): com.example.problemfinder.domain.opportunities.api.search.LoadOpportunitySearchOptionsUseCase
    @Binds abstract fun bindObserveOpportunityDetails(implementation: ObserveOpportunityDetailsUseCaseImpl): ObserveOpportunityDetailsUseCase
    @Binds abstract fun bindRefreshOpportunityDetails(implementation: RefreshOpportunityDetailsUseCaseImpl): RefreshOpportunityDetailsUseCase
    @Binds abstract fun bindLoadNextOpportunityEvidence(implementation: LoadNextOpportunityEvidenceUseCaseImpl): LoadNextOpportunityEvidenceUseCase
    @Binds abstract fun bindObserveOpportunitySearch(implementation: ObserveOpportunitySearchUseCaseImpl): ObserveOpportunitySearchUseCase
    @Binds abstract fun bindRefreshOpportunitySearch(implementation: RefreshOpportunitySearchUseCaseImpl): RefreshOpportunitySearchUseCase
    @Binds abstract fun bindLoadNextOpportunitySearchPage(implementation: LoadNextOpportunitySearchPageUseCaseImpl): LoadNextOpportunitySearchPageUseCase
    @Binds abstract fun bindPrepareOpportunitySearchDraft(implementation: PrepareOpportunitySearchDraftUseCaseImpl): PrepareOpportunitySearchDraftUseCase
}
