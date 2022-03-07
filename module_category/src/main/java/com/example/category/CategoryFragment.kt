package com.example.category

import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.example.category.adapter.CategoryListAdapter
import com.example.category.state.LeftCategoryState
import com.example.category.viewmodel.LeftCategoryViewModel
import com.example.category.viewmodel.RightCategoryViewModel
import com.example.common.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_main.*

class CategoryFragment : BaseFragment(R.layout.fragment_main), MavericksView {
    private val leftViewModel: LeftCategoryViewModel by activityViewModel()
    private val rightViewModel: RightCategoryViewModel by activityViewModel()

    private val categoryListAdapter by lazy { CategoryListAdapter(R.layout.main_left_item, arrayListOf())  }
    private val categoryLayoutManager: LinearLayoutManager by lazy { LinearLayoutManager(this.activity) }

    override fun initView() {
        categoryList.run {
            layoutManager = categoryLayoutManager
            adapter = categoryListAdapter
            categoryListAdapter.setOnItemClickListener{ _, _, position ->
                setSelectCategory(position)
                scrollToMiddle(position)
                rightViewModel.queryContentByCate(categoryListAdapter.data[position].code)
            }
        }
        rightContainer.run {
            setEnableRefresh(false)
            setEnableAutoLoadMore(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rightContainer.removeAllViews()
    }

    override fun initData() {
        leftViewModel.initBrandList()
        leftViewModel.onEach(LeftCategoryState::brandList) {
                brandList -> run {
                if (brandList.isNotEmpty()) {
                    rightViewModel.queryContentByCate(brandList[0].code)
                }
            }
        }
    }

    override fun invalidate() {
        withState(leftViewModel, rightViewModel) {
            left, right ->
            if (left.brandList.isNotEmpty()) {
                categoryListAdapter.setList(left.brandList)
            }
            if (right.content.cateList.isNotEmpty()) {
                categoryRightView.setData(right.content)
            }
        }
    }

    private fun setSelectCategory(selectIndex: Int) {
        categoryListAdapter.run {
            var preSelectIndex = data.indexOfFirst { v -> v.isSelect == true }
            if (preSelectIndex - 1 >= 0) {
                data[preSelectIndex - 1].fillet = ""
                notifyItemChanged(preSelectIndex - 1)
            }
            if (preSelectIndex + 1 < categoryListAdapter.data.size) {
                data[preSelectIndex + 1].fillet = ""
                notifyItemChanged(preSelectIndex + 1)
            }
            data[preSelectIndex].isSelect = false
            data[selectIndex].isSelect = true
            if (selectIndex - 1 >= 0) {
                data[selectIndex - 1].fillet = "down"
                notifyItemChanged(selectIndex - 1)
            }
            if (selectIndex + 1 < categoryListAdapter.data.size) {
                data[selectIndex + 1].fillet = "up"
                notifyItemChanged(selectIndex + 1)
            }
            notifyItemChanged(preSelectIndex)
            notifyItemChanged(selectIndex)
        }
    }

    private fun scrollToMiddle(position: Int) {
        val displayHeight = categoryList.height
        categoryLayoutManager.scrollToPositionWithOffset(position, displayHeight / 2 - 48)
    }
}