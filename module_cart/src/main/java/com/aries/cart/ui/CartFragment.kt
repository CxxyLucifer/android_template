package com.aries.cart.ui

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.aries.common.base.BaseFragment
import com.aries.cart.R
import com.aries.cart.ui.adapter.StoreListAdapter
import com.aries.cart.ui.listener.OnChildItemChildClickListener
import com.aries.cart.ui.listener.OnStepperChangeListener
import com.aries.cart.ui.view.QuickEntryPopup
import com.aries.common.adapter.GoodsListAdapter
import com.aries.common.decoration.SpacesItemDecoration
import com.aries.common.util.PixelUtil
import com.aries.common.util.StatusBarUtil
import com.aries.common.util.UnreadMsgUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.android.material.appbar.AppBarLayout
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import kotlinx.android.synthetic.main.bottom_all_select.*
import kotlinx.android.synthetic.main.fragment_cart.*
import kotlinx.android.synthetic.main.fragment_cart_content.*
import kotlinx.android.synthetic.main.top_address.*
import kotlinx.android.synthetic.main.top_filter.*
import java.math.BigDecimal

class CartFragment : BaseFragment(R.layout.fragment_cart), MavericksView {
    private val viewModel: CartViewModel by activityViewModel()
    //购物车中店铺商品列表adapter
    private val cartGoodsListAdapter: StoreListAdapter by lazy {
        StoreListAdapter(R.layout.fragment_cart_item, arrayListOf())
    }
    //你可能还喜欢 或者 快点来看看 商品列表adapter
    private val goodsListAdapter by lazy { GoodsListAdapter(arrayListOf()) }
    private val staggeredGridLayoutManager: StaggeredGridLayoutManager by lazy {
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    private val topViewHeight = StatusBarUtil.getHeight() + PixelUtil.toPixelFromDIP(40f).toInt()

    override fun initView() {
        initStatusBarPlaceholder()
        //快捷菜单点点点 角标
        UnreadMsgUtil.show(threePointsBadgeNum, 2)
        //快捷菜单点击事件
        threePointsLayout.setOnClickListener { showQuickEntry() }

        cartAppBarLayout.run {
            addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                if (verticalOffset <= -topViewHeight) {
                    statusBarPlaceholder.visibility = View.VISIBLE
                } else {
                    statusBarPlaceholder.visibility = View.GONE
                }
            })
        }
        //SmartRefreshLayout
        smartRefreshLayout.run {
            setOnRefreshListener {
                viewModel.queryCartGoodsList(true)
                viewModel.initMaybeLikeList()
            }
            setEnableAutoLoadMore(true)
            setOnLoadMoreListener {
                viewModel.loadMoreMaybeLikeList()
            }
        }
        //购物车中的商品列表
        cartGoodsList.run {
            layoutManager = LinearLayoutManager(this.context)
            adapter = cartGoodsListAdapter
        }
        cartGoodsListAdapter.addChildClickViewIds(R.id.storeCheckBox)
        //监听店铺是否选中
        cartGoodsListAdapter.setOnItemChildClickListener  { _, view, position ->
            when (view.id) {
                R.id.storeCheckBox -> checkAllByStore(position)
            }
        }
        //监听商品是否选中
        cartGoodsListAdapter.setOnChildItemChildClickListener(object : OnChildItemChildClickListener {
            override fun onItemChildClick(
                adapter: BaseQuickAdapter<*, *>,
                view: View,
                parentPosition: Int,
                position: Int,
            ) {
                when (view.id) {
                    R.id.goodsCheckBox -> checkGoods(parentPosition, position)
                }
            }
        })
        //监听数量stepper加减
        cartGoodsListAdapter.setOnStepperChangeListener(object: OnStepperChangeListener {
            override fun onStepperChange(bean: CartGoodsBean, value: Int) {
                setGoodsNum(bean, value)
            }
        })

        //你可能还喜欢 或者 快点来看看 商品列表
        goodsList.run {
            addItemDecoration(SpacesItemDecoration(10))
            layoutManager = staggeredGridLayoutManager
            adapter = goodsListAdapter
        }
        //返回顶部
        backTop.setOnClickListener {
            nestedScrollView.smoothScrollTo(0, 0)
            cartAppBarLayout.setExpanded(true, true)
        }
        //全选
        totalCheckBox.setOnClickListener { checkAll() }
    }

    override fun initData() {
        viewModel.queryCartGoodsList(false)
        viewModel.initMaybeLikeList()
    }

    override fun invalidate() {
        withState(viewModel) {
            if (it.cartGoodsList.isNotEmpty()) {
                cartGoodsListAdapter.setList(it.cartGoodsList)
                setFilterInfo(it.cartGoodsList)
                calcTotalInfo(it.cartGoodsList)
                if (it.fetchType === "refresh") {
                    smartRefreshLayout.run { finishRefresh() }
                }
            }
            if (it.goodsList.isNotEmpty()) {
                goodsListAdapter.setList(it.goodsList)
                smartRefreshLayout.run { resetNoMoreData() }
            }
            if (it.nextPageGoodsList.isNotEmpty()) {
                goodsListAdapter.addData(it.nextPageGoodsList)

                smartRefreshLayout.run {
                    if (it.currentPage <= it.totalPage) finishLoadMore()
                    else finishLoadMoreWithNoMoreData()
                }
            }
        }
    }

    //设置占位符高度
    private fun initStatusBarPlaceholder() {
        topAddressLayout.setPadding(0, StatusBarUtil.getHeight(), 0 , 0)
        val layoutParams = statusBarPlaceholder.layoutParams
        layoutParams.height = StatusBarUtil.getHeight()
        statusBarPlaceholder.layoutParams = layoutParams
        statusBarPlaceholder.visibility = View.GONE

//        bottomAllSelectLayout.visibility = View.GONE
    }

    //顶部快捷入口
    private fun showQuickEntry() {
        XPopup.Builder(this.requireContext())
            .popupAnimation(PopupAnimation.TranslateFromTop)
//            .hasShadowBg(false)
            .isLightStatusBar(true)
            .asCustom(QuickEntryPopup(this.requireContext()))
            .show()
    }

    //设置顶部搜索信息
    private fun setFilterInfo(list: List<StoreGoodsBean>) {
        val totalList = list.map(StoreGoodsBean::goodsList).flatMap { element -> element.asIterable() }
        filterAll.run {
            "全部 ${totalList.size}".also { text = it }
            setTextColor(Color.parseColor("#D8433F"))
        }
        discountTxt.text = "降价 0"
    }

    //点击店铺前面的checkbox
    private fun checkAllByStore(position: Int) {
        val dataList = cartGoodsListAdapter.data
        val storeCheck = dataList[position].check!!
        if (!storeCheck) {
            dataList[position].check = true
            dataList[position].goodsList.forEach{v -> v.check = true}
        } else {
            dataList[position].check = false
            dataList[position].goodsList.forEach{v -> v.check = false}
        }

        viewModel.updateCartGoodsList(dataList)

        val totalFlag = dataList.indexOfFirst { v -> v.check == false }
        totalCheckBox.isChecked = totalFlag == -1
    }

    //点击每个商品前面的checkbox
    private fun checkGoods(parent: Int, position: Int) {
        val parentList = cartGoodsListAdapter.data
        val childList = parentList[parent].goodsList

        childList[position].check = !(childList[position].check!!)

        val falseFLag = childList.indexOfFirst { v -> v.check == false }
        parentList[parent].check = falseFLag == -1
        parentList[parent].goodsList = childList

        viewModel.updateCartGoodsList(parentList)

        val totalFlag = parentList.indexOfFirst { v -> v.check == false }
        totalCheckBox.isChecked = totalFlag == -1
    }

    //点击最下面的全选按钮
    private fun checkAll() {
        val dataList = cartGoodsListAdapter.data
        val isAllChecked = dataList.indexOfFirst { v -> v.check == false } == -1

        totalCheckBox.isChecked = !isAllChecked
        dataList.forEach { v -> v.check = !isAllChecked; v.goodsList.forEach { m -> m.check = !isAllChecked } }

        viewModel.updateCartGoodsList(dataList)
    }

    // 设置下面的全选 总价
    private fun calcTotalInfo(dataList: List<StoreGoodsBean>) {
        totalCheckBox.isChecked = dataList.indexOfFirst { v -> v.check == false } == -1

        var totalPrice: BigDecimal = BigDecimal.ZERO
        var totalNum = 0
        dataList.forEach { v ->
            v.goodsList.forEach { m -> if (m.check == true){
                totalNum += m.num
                totalPrice = totalPrice.add(m.price.toBigDecimal().multiply(m.num.toBigDecimal()))
            }}
        }

        "￥${totalPrice}".also { totalPriceTxt.text = it }
        "去结算(${totalNum})".also { btnGoOrder.text = it }
    }

    //
    private fun setGoodsNum(bean: CartGoodsBean, value: Int) {
        val dataList = cartGoodsListAdapter.data
        dataList.forEach{ v-> v.goodsList.forEach { m -> if (m.code == bean.code) m.num = value }}

        viewModel.updateCartGoodsList(dataList)
    }
}