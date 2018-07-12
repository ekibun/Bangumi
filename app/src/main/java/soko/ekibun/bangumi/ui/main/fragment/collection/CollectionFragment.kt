package soko.ekibun.bangumi.ui.main.fragment.collection

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.google.gson.reflect.TypeToken
import com.nshmura.recyclertablayout.RecyclerTabLayout
import kotlinx.android.synthetic.main.content_collection.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.CollectionStatusType
import soko.ekibun.bangumi.api.bangumi.bean.SubjectCollection
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.util.JsonUtil

class CollectionFragment: DrawerFragment(R.layout.content_collection){

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("CollectionPageIndex", pageIndex.toIntArray())
        collection_pager?.let{ outState.putInt("CollectionPage", it.currentItem) }
        viewList.forEachIndexed { index, swipeRefreshLayout -> (swipeRefreshLayout.tag as? RecyclerView)?.let {
            outState.putString("CollectionData$index", JsonUtil.toJson((it.adapter as CollectionListAdapter).data))
            outState.putParcelable("Collection$index", it.layoutManager.onSaveInstanceState())
            outState.putBoolean("CollectionStatus$index", it.tag != null)
        } }
    }

    override val showTab: Boolean = true
    override val titleRes: Int = R.string.chase_bangumi

    private val typeList = mapOf(
            R.id.collection_type_anime to Pair(SubjectType.ANIME, SubjectType.NAME_ANIME),
            R.id.collection_type_book to Pair(SubjectType.BOOK, SubjectType.NAME_BOOK),
            R.id.collection_type_game to Pair(SubjectType.GAME, SubjectType.NAME_GAME),
            R.id.collection_type_music to Pair(SubjectType.MUSIC, SubjectType.NAME_MUSIC),
            R.id.collection_type_real to Pair(SubjectType.REAL, SubjectType.NAME_REAL))
    private var selectedType = R.id.collection_type_anime

    private val api by lazy { Bangumi.createInstance() }
    var user: UserInfo? = null

    val viewList = ArrayList<SwipeRefreshLayout>()
    private val adapters = ArrayList<CollectionListAdapter>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.savedInstanceState = savedInstanceState?:this.savedInstanceState
        if (viewList.size == 0) {
            for (i in 0 until CollectionStatusType.status.size) {
                val swipeRefreshLayout = SwipeRefreshLayout(view.context)
                val recyclerView = RecyclerView(view.context)
                val adapter = CollectionListAdapter()
                adapter.emptyView = activity?.layoutInflater?.inflate(R.layout.view_empty, view as ViewGroup, false)
                adapter.isUseEmpty(false)
                adapter.setEnableLoadMore(true)
                adapter.setOnLoadMoreListener({
                    loadCollectionList(i)
                }, recyclerView)
                adapter.setOnItemClickListener { _, v, position ->
                    SubjectActivity.startActivity(v.context, adapter.data[position].subject!!)
                }
                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(view.context)
                recyclerView.isNestedScrollingEnabled = false
                swipeRefreshLayout.addView(recyclerView)
                swipeRefreshLayout.tag = recyclerView
                swipeRefreshLayout.setOnRefreshListener { reset() }
                this.adapters.add(adapter)
                this.viewList.add(swipeRefreshLayout)
            }
        }

        if(this.savedInstanceState != null){
            collection_pager.currentItem = this.savedInstanceState!!.getInt("CollectionPage", 2)
            pageIndex = this.savedInstanceState!!.getIntArray("CollectionPageIndex").toTypedArray()
            viewList.forEachIndexed { index, swipeRefreshLayout -> (swipeRefreshLayout.tag as? RecyclerView)?.let {
                it.layoutManager.onRestoreInstanceState(this.savedInstanceState!!.getParcelable("Collection$index"))
                (it.adapter as CollectionListAdapter).setNewData(JsonUtil.toEntity(this.savedInstanceState!!.getString("CollectionData$index"), object: TypeToken<List<SubjectCollection>>(){}.type))
                it.tag = if(this.savedInstanceState!!.getBoolean("CollectionStatus$index")) true else null
            } }
        }

        collection_pager.adapter = CollectionPagerAdapter(view.context, this)
        activity?.findViewById<RecyclerTabLayout>(R.id.tab_layout)?.setUpWithViewPager(collection_pager)

        collection_pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if((viewList[position].tag as? RecyclerView)?.tag == null) {
                    pageIndex[position] = 0
                    loadCollectionList(position)
                }
            } })

        collection_pager.currentItem = this.savedInstanceState?.getInt("CollectionPage", 2)?:2
        this.savedInstanceState = null
    }

    fun reset() {
        viewList.forEach { (it.tag as? RecyclerView)?.tag = null }
        pageIndex.forEachIndexed { index, _ -> pageIndex[index] = 0 }
        loadCollectionList()
    }


    private var collectionCalls = HashMap<Int, Call<List<SubjectCollection>>>()
    private var pageIndex = arrayOf(0, 0, 0, 0, 0)
    fun loadCollectionList(index: Int = collection_pager?.currentItem?:0){
        viewList.getOrNull(index)?.isRefreshing = false
        //Log.v("index", index.toString())
        collectionCalls[index]?.cancel()
        if(pageIndex[index] == 0)
            adapters.getOrNull(index)?.setNewData(null)
        val userName = user?.id?.toString() ?: return
        if(pageIndex[index] == 0)
            viewList.getOrNull(index)?.isRefreshing = true
        if(index == 2 && pageIndex[index] > 0) {
            adapters.getOrNull(index)?.loadMoreEnd()
            return
        }
        val useApi = index == 2 && selectedType in arrayOf(R.id.collection_type_anime, R.id.collection_type_book, R.id.collection_type_real)
        collectionCalls[index] = if(useApi) api.collection(userName)
            else Bangumi.getCollectionList(typeList[selectedType]?.second?:"", userName, CollectionStatusType.status[index], pageIndex[index]+1)
        adapters.getOrNull(index)?.isUseEmpty(false)
        collectionCalls[index]?.enqueue(ApiHelper.buildCallback(viewList.getOrNull(index)?.context, {
            adapters.getOrNull(index)?.isUseEmpty(true)
            it.filter { !useApi || it.subject?.type == typeList[selectedType]?.first }.let{
                if(!useApi) it.forEach {
                    it.subject?.type = typeList[selectedType]?.first?:0 }
                adapters.getOrNull(index)?.addData(it) }
            if(useApi || it.size < 10)
                adapters.getOrNull(index)?.loadMoreEnd()
            else
                adapters.getOrNull(index)?.loadMoreComplete()
            (viewList.getOrNull(index)?.tag as? RecyclerView)?.tag = true
            pageIndex[index] += 1
        },{
            viewList.getOrNull(index)?.isRefreshing = false
            adapters.getOrNull(index)?.loadMoreFail()
        }))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_search)?.isVisible = false
        val item = menu.findItem(R.id.action_type)?:return
        item.title = SubjectType.getDescription(typeList[selectedType]?.first?:0)
        item.setOnMenuItemClickListener {
            //Log.v("it", "click")
            val context = activity?:return@setOnMenuItemClickListener true
            val popup = PopupMenu(context, context.findViewById(R.id.action_type))
            popup.menuInflater.inflate(R.menu.list_collection_type, popup.menu)
            popup.setOnMenuItemClickListener{
                selectedType = it.itemId
                item.title = it.title
                reset()
                true
            }
            popup.show()
            true
        }
    }
}