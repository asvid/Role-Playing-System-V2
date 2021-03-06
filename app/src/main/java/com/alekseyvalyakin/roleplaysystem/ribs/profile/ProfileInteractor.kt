package com.alekseyvalyakin.roleplaysystem.ribs.profile

import com.alekseyvalyakin.roleplaysystem.data.useravatar.UserAvatarRepository
import com.alekseyvalyakin.roleplaysystem.di.activity.ActivityListener
import com.alekseyvalyakin.roleplaysystem.di.activity.ThreadConfig
import com.alekseyvalyakin.roleplaysystem.utils.StringUtils
import com.alekseyvalyakin.roleplaysystem.utils.image.ImagesResult
import com.alekseyvalyakin.roleplaysystem.utils.image.LocalImageProvider
import com.alekseyvalyakin.roleplaysystem.utils.subscribeWithErrorLogging
import com.uber.rib.core.BaseInteractor
import com.uber.rib.core.Bundle
import com.uber.rib.core.RibInteractor
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject

/**
 * Coordinates Business Logic for [ProfileScope].
 *
 */
@RibInteractor
class ProfileInteractor : BaseInteractor<ProfilePresenter, ProfileRouter>() {

    @Inject
    lateinit var presenter: ProfilePresenter
    @Inject
    lateinit var activityListener: ActivityListener
    @Inject
    lateinit var profileViewModelProvider: ProfileViewModelProvider
    @field:[Inject ThreadConfig(ThreadConfig.TYPE.UI)]
    lateinit var uiScheduler: Scheduler
    @Inject
    lateinit var localImageProvider: LocalImageProvider
    @Inject
    lateinit var userAvatarRepository: UserAvatarRepository

    private var currentModel: ProfileViewModel? = null

    override fun didBecomeActive(savedInstanceState: Bundle?) {
        super.didBecomeActive(savedInstanceState)
        userAvatarRepository.subscribeForUpdates()
                .addToDisposables()

        profileViewModelProvider.observeProfileViewModel()
                .observeOn(uiScheduler)
                .subscribeWithErrorLogging {
                    currentModel = it
                    presenter.updateViewModel(it)
                }.addToDisposables()

        localImageProvider.observeImage().switchMap {
            if (it is ImagesResult.Success) {
                return@switchMap userAvatarRepository.uploadAvatar(it.images.first().originalPath)
                        .doOnSubscribe { _ -> presenter.showLoadingContent(true) }
                        .doAfterTerminate { presenter.showLoadingContent(false) }
                        .toFlowable()
            }
            return@switchMap Flowable.error<String>(RuntimeException((it as ImagesResult.Error).error))
        }.onErrorReturn {
            Timber.e(it)
            StringUtils.EMPTY_STRING
        }.subscribeWithErrorLogging()
                .addToDisposables()

        presenter.observeUiEvents()
                .observeOn(uiScheduler)
                .concatMap(this::handleEvent)
                .subscribeWithErrorLogging().addToDisposables()
    }

    private fun handleEvent(event: ProfilePresenter.Event): Observable<*> {
        return when (event) {
            is ProfilePresenter.Event.BackPress -> {
                Observable.fromCallable {
                    activityListener.backPress()
                }
            }
            is ProfilePresenter.Event.EditNamePress -> {
                Observable.fromCallable {
                    currentModel?.let {
                        presenter.showEditDisplayNameDialog(it.displayName)
                    }
                }
            }

            is ProfilePresenter.Event.ChooseAvatar -> {
                return Observable.fromCallable {
                    localImageProvider.pickImage()
                }
            }

            is ProfilePresenter.Event.EditNameConfirm -> {
                return profileViewModelProvider.onNameChanged(event.name).toObservable<Any>()
            }
        }
    }

}
