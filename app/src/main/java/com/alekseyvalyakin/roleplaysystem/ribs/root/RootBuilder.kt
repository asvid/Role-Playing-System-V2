package com.alekseyvalyakin.roleplaysystem.ribs.root

import android.view.LayoutInflater
import android.view.ViewGroup
import com.alekseyvalyakin.roleplaysystem.di.activity.ActivityComponent
import com.alekseyvalyakin.roleplaysystem.di.rib.RibDependencyProvider
import com.alekseyvalyakin.roleplaysystem.ribs.auth.AuthBuilder
import com.alekseyvalyakin.roleplaysystem.ribs.main.MainBuilder
import com.alekseyvalyakin.roleplaysystem.ribs.main.MainRibListener
import com.uber.rib.core.BaseViewBuilder
import com.uber.rib.core.InteractorBaseComponent
import com.uber.rib.core.RouterNavigatorFactory
import dagger.Binds
import dagger.BindsInstance
import dagger.Provides
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Scope

/**
 * Builder for the {@link RootScope}.
 *
 * TODO describe this scope's responsibility as a whole.
 */
class RootBuilder(dependency: ActivityComponent) : BaseViewBuilder<RootView, RootRouter, ActivityComponent>(dependency) {

    /**
     * Builds a new [RootRouter].
     *
     * @param parentViewGroup parent view group that this router's view will be added to.
     * @return a new [RootRouter].
     */
    override fun build(parentViewGroup: ViewGroup): RootRouter {
        val view = createView(parentViewGroup)
        val interactor = RootInteractor()
        val component = dependency.builder()
                .view(view)
                .interactor(interactor)
                .mainRibListener(object : MainRibListener {
                    override fun onCreateGamePressed() {
                        Timber.d("On create game")
                    }
                })
                .build()
        return component.rootRouter()
    }

    override fun inflateView(inflater: LayoutInflater, parentViewGroup: ViewGroup): RootView? {
        return RootView(parentViewGroup.context)
    }

    interface ParentComponent : RibDependencyProvider {

    }

    @dagger.Module
    abstract class Module {

        @RootScope
        @Binds
        internal abstract fun presenter(view: RootView): RootInteractor.RootPresenter

        @dagger.Module
        companion object {

            @RootScope
            @Provides
            @JvmStatic
            fun router(
                    component: Component,
                    view: RootView,
                    interactor: RootInteractor,
                    routerNavigatorFactory: RouterNavigatorFactory): RootRouter {
                return RootRouter(view, interactor, component, routerNavigatorFactory,
                        AuthBuilder(component),
                        MainBuilder(component))
            }
        }

    }

    @RootScope
    @dagger.Subcomponent(modules = [(Module::class)])
    interface Component : InteractorBaseComponent<RootInteractor>,
            BuilderComponent,
            RibDependencyProvider,
            AuthBuilder.ParentComponent,
            MainBuilder.ParentComponent {

        @dagger.Subcomponent.Builder
        interface Builder {
            @BindsInstance
            fun interactor(interactor: RootInteractor): Builder

            @BindsInstance
            fun view(view: RootView): Builder

            @BindsInstance
            fun mainRibListener(mainRibListener: MainRibListener): Builder

            fun build(): Component
        }
    }

    interface BuilderComponent {
        fun rootRouter(): RootRouter
    }

    @Scope
    @Retention(AnnotationRetention.BINARY)
    internal annotation class RootScope

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    internal annotation class RootInternal
}
