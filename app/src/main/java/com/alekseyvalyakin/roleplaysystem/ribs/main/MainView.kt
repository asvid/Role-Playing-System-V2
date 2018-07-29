package com.alekseyvalyakin.roleplaysystem.ribs.main

import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.alekseyvalyakin.roleplaysystem.R
import com.alekseyvalyakin.roleplaysystem.utils.*
import com.alekseyvalyakin.roleplaysystem.views.SearchToolbar
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxrelay2.PublishRelay
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import io.reactivex.Observable
import org.jetbrains.anko.*
import org.jetbrains.anko.design._CoordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.recyclerview.v7.recyclerView
import java.util.concurrent.TimeUnit

/**
 * Top level view for {@link MainBuilder.MainScope}.
 */
class MainView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : _CoordinatorLayout(context), MainInteractor.MainPresenter {

    private lateinit var searchToolbar: SearchToolbar
    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private val relay = PublishRelay.create<MainInteractor.UiEvents>()
    private val flexibleAdapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter(emptyList())

    init {
        AnkoContext.createDelegate(this).apply {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                searchToolbar = searchToolbar {
                }.lparams(width = matchParent, height = wrapContent)

                recyclerView = recyclerView {
                    id = R.id.recycler_view
                    isVerticalScrollBarEnabled = true
                    layoutManager = LinearLayoutManager(context)
                    adapter = flexibleAdapter
                }.lparams(width = matchParent, height = matchParent)
            }.lparams(width = matchParent, height = matchParent)

            fab = floatingActionButton {
                id = R.id.fab
                size = FloatingActionButton.SIZE_NORMAL
                tintImage(R.color.material_light_white)
                setImageDrawable(getCompatDrawable(R.drawable.ic_add_black_24dp))
            }.lparams {
                gravity = Gravity.BOTTOM or Gravity.END
                margin = getIntDimen(R.dimen.dp_8)
            }
        }
    }

    override fun updateModel(model: MainViewModel) {
        flexibleAdapter.updateDataSet(model.flexibleItems)
    }

    override fun observeUiEvents(): Observable<MainInteractor.UiEvents> {
        return Observable.merge(observeSearchInput(),
                observeSearchRightIconClick(),
                relay,
                observeFabClick())
    }

    private fun observeFabClick(): Observable<MainInteractor.UiEvents> {
        return RxView.clicks(fab).map { MainInteractor.UiEvents.FabClick() }
    }

    private fun observeSearchInput(): Observable<MainInteractor.UiEvents.SearchInput> = searchToolbar.observeSearchInput()
            .debounce(200L, TimeUnit.MILLISECONDS)
            .throttleLast(100L, TimeUnit.MILLISECONDS)
            .map { MainInteractor.UiEvents.SearchInput(it.toString()) }

    override fun showSearchContextMenu() {
        val popupMenu = PopupMenu(context, searchToolbar.getPopupViewAnchor())
        popupMenu.menu.add(0, LOGOUT, 0, getString(R.string.logout))
        popupMenu.setOnMenuItemClickListener { item ->
            when {
                item.itemId == LOGOUT -> relay.accept(MainInteractor.UiEvents.Logout())
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    private fun observeSearchRightIconClick(): Observable<MainInteractor.UiEvents.SearchRightIconClick> = searchToolbar.observeRightImageClick()
            .map { MainInteractor.UiEvents.SearchRightIconClick() }

    companion object {
        private const val LOGOUT = 1
    }
}
