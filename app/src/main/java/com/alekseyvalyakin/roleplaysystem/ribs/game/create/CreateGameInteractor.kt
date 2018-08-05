package com.alekseyvalyakin.roleplaysystem.ribs.game.create

import com.alekseyvalyakin.roleplaysystem.di.activity.ActivityListener
import com.alekseyvalyakin.roleplaysystem.utils.subscribeWithErrorLogging
import com.uber.rib.core.BaseInteractor
import com.uber.rib.core.Bundle
import com.uber.rib.core.RibInteractor
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import javax.inject.Inject

/**
 * Coordinates Business Logic for [CreateGameScope].
 *
 */
@RibInteractor
class CreateGameInteractor : BaseInteractor<CreateGameInteractor.CreateGamePresenter, CreateGameRouter>() {

    @Inject
    lateinit var presenter: CreateGamePresenter
    @Inject
    lateinit var viewModelProvider: CreateGameViewModelProvider
    @Inject
    lateinit var activityListener: ActivityListener

    private lateinit var model: CreateGameViewModel

    override fun didBecomeActive(savedInstanceState: Bundle?) {
        super.didBecomeActive(savedInstanceState)
        initModel(savedInstanceState)
        presenter.updateFabShowDisposable()
                .addToDisposables()
        presenter.observeUiEvents()
                .subscribeWithErrorLogging(this::handleEvent)
                .addToDisposables()
        presenter.updateView(model)
    }

    private fun handleEvent(event: CreateGameUiEvent) {
        when (event) {
            is CreateGameUiEvent.InputChange -> {
                model = model.copy(inputText = event.text)
            }
            is CreateGameUiEvent.ClickNext -> {
                Timber.d("Click next")
            }
            is CreateGameUiEvent.BackPress -> {
                activityListener.backPress()
            }
        }
    }

    override fun handleBackPress(): Boolean {
        val previousStep = model.step.getPreviousStep()
        if (previousStep == CreateGameStep.NONE) {
            return false
        }
        model = viewModelProvider.getCreateGameViewModel(previousStep)
        presenter.updateView(model)
        return true
    }

    private fun initModel(savedInstanceState: Bundle?) {
        model = if (savedInstanceState == null) {
            viewModelProvider.getCreateGameViewModel(CreateGameStep.TITLE)
        } else {
            savedInstanceState.getParcelable(CreateGameViewModel.KEY) as CreateGameViewModel
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CreateGameViewModel.KEY, model)
    }

    override fun willResignActive() {
        super.willResignActive()
    }

    /**
     * Presenter interface implemented by this RIB's view.
     */
    interface CreateGamePresenter {
        fun updateView(createGameViewModel: CreateGameViewModel)
        fun updateFabShowDisposable(): Disposable
        fun observeUiEvents(): Observable<CreateGameUiEvent>
    }
}
